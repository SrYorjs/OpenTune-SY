#!/usr/bin/env bash
# Instalador PÚBLICO de OpenTune Resolver (Ubuntu 24.04, como root). Sin Tailscale.
#   Con dominio (HTTPS automático):  bash setup-opentune-public.sh midominio.duckdns.org
set -euo pipefail
DOMAIN="${1:-}"
if [[ ! "$DOMAIN" =~ ^[a-zA-Z0-9]([a-zA-Z0-9.-]*[a-zA-Z0-9])?$ || "$DOMAIN" != *.* ]]; then
  echo "Indica un dominio para HTTPS; no se instala un endpoint público sin cifrar." >&2; exit 1
fi
if [[ -e /opt/opentune-resolver/main.py || -e /opt/opentune-resolver/.env ]]; then
  echo "Ya existe una instalación; usa una actualización con respaldo." >&2; exit 1
fi
if [ "$(id -u)" -ne 0 ]; then echo "Ejecuta como root (sudo -i)"; exit 1; fi

echo "==> Actualizando sistema"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y && apt-get upgrade -y
apt-get install -y ca-certificates curl openssl ufw unattended-upgrades dnsutils

PUBLIC_IP="$(curl -4 -fsS https://api.ipify.org || true)"

if [ -n "$DOMAIN" ]; then
  RESOLVED="$(dig +short "$DOMAIN" A | tail -n1 || true)"
  if [ "$RESOLVED" != "$PUBLIC_IP" ]; then
    echo "AVISO: $DOMAIN apunta a '$RESOLVED' pero este servidor es '$PUBLIC_IP'."
    echo "Corrige el DNS (en duckdns.org pon la IP $PUBLIC_IP) y vuelve a ejecutar."; exit 1
  fi
fi

echo "==> Instalando Docker"
if ! command -v docker >/dev/null; then curl -fsSL https://get.docker.com | sh; fi

echo "==> Escribiendo archivos en /opt/opentune-resolver"
mkdir -p /opt/opentune-resolver && cd /opt/opentune-resolver
cat > multisource.py <<'OPENTUNE_V2_SOURCE'
from __future__ import annotations

import asyncio
import hmac
import logging
import os
import re
import time
from collections import OrderedDict
from dataclasses import dataclass
from urllib.parse import parse_qs, urlparse
from concurrent.futures import ThreadPoolExecutor
from upstream_security import public_addresses, upstream_transport

import httpx
import yt_dlp
from fastapi import APIRouter, Header, HTTPException, Query, Request
from fastapi.responses import Response, StreamingResponse

router = APIRouter(prefix="/v2")
API_KEY = os.getenv("API_KEY", "")
AUDIUS_API_KEY = os.getenv("AUDIUS_API_KEY", "")
PEERTUBE_INSTANCE = os.getenv("PEERTUBE_INSTANCE", "").rstrip("/")
PROXY_URL = os.getenv("PROXY_URL") or None
VIDEO_ID = re.compile(r"^[A-Za-z0-9_-]{11}$")
TRACK_ID = re.compile(r"^[A-Za-z0-9_-]{1,80}$")
PEERTUBE_ID = re.compile(r"^[a-fA-F0-9-]{36}$")
KINDS = {"audio", "video"}
QUALITIES = {"auto", "low", "high", "highest"}
PROVIDERS = {"youtube", "youtube_music", "audius", "peertube"}
SEMAPHORE = asyncio.Semaphore(max(1, min(3, int(os.getenv("RESOLVE_CONCURRENCY", "2")))))
SEARCH_TIMEOUT = min(8, max(.5, float(os.getenv("SEARCH_TIMEOUT", "5"))))
URL_TTL = min(900, max(60, int(os.getenv("RESOLUTION_TTL", "240"))))
RAM_LIMIT = 200
WORKERS = ThreadPoolExecutor(max_workers=2)
BLOCKING_SLOTS = asyncio.Semaphore(2)
IN_FLIGHT = {}
AUDIUS_HOSTS = {"api.audius.co", "creatornode.audius.co", "validator.eeba4a6ca56a0d87af802270217c2a51.r2.cloudflarestorage.com"}
AUDIUS_HOSTS.update(h.strip().lower() for h in os.getenv("AUDIUS_R2_HOSTS", "").split(",") if h.strip().lower().endswith(".r2.cloudflarestorage.com"))
RAM: OrderedDict[tuple, tuple[float, dict]] = OrderedDict()
SEARCH_RAM: OrderedDict[tuple, tuple[float, dict]] = OrderedDict()
CLIENT = httpx.AsyncClient(timeout=httpx.Timeout(connect=4, read=30, write=10, pool=4), follow_redirects=False, transport=upstream_transport(PROXY_URL))
logging.getLogger("httpx").setLevel(logging.WARNING)
logging.getLogger("httpcore").setLevel(logging.WARNING)


class ExtractionLogger:
    def debug(self, message):
        logging.getLogger(__name__).debug("Extractor diagnostic")

    def warning(self, message):
        logging.getLogger(__name__).warning("Extractor warning")

    def error(self, message):
        logging.getLogger(__name__).error("Extractor error")


async def run_provider_blocking(function, *args):
    await BLOCKING_SLOTS.acquire()
    future = asyncio.get_running_loop().run_in_executor(WORKERS, function, *args)
    future.add_done_callback(lambda _: BLOCKING_SLOTS.release())
    return await asyncio.shield(future)

async def close_clients():
    await CLIENT.aclose()
    WORKERS.shutdown(wait=False, cancel_futures=True)


def auth(x_api_key: str | None, key: str | None) -> None:
    if not API_KEY:
        raise HTTPException(status_code=503, detail="API_KEY no configurada")
    if not hmac.compare_digest((x_api_key or key or "").encode(), API_KEY.encode()):
        raise HTTPException(status_code=401, detail="No autorizado")


def validate(provider: str, item_id: str, kind: str, quality: str) -> None:
    if provider not in PROVIDERS or (provider == "peertube" and not PEERTUBE_INSTANCE):
        raise HTTPException(status_code=400, detail="Proveedor no disponible")
    if kind not in KINDS or quality not in QUALITIES:
        raise HTTPException(status_code=400, detail="Tipo o calidad inválidos")
    if (provider in {"youtube", "youtube_music"} and not VIDEO_ID.fullmatch(item_id)) or (provider == "audius" and not TRACK_ID.fullmatch(item_id)) or (provider == "peertube" and not PEERTUBE_ID.fullmatch(item_id)):
        raise HTTPException(status_code=400, detail="Identificador inválido")
    if provider == "peertube" and kind == "audio":
        raise HTTPException(status_code=422, detail="PeerTube ofrece video progresivo")
    if provider == "audius" and kind == "video":
        raise HTTPException(status_code=422, detail="Audius solo ofrece audio")


def ydl_options(flat: bool = False) -> dict:
    result = {
        "quiet": True,
        "logger": ExtractionLogger(),
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": not flat,
        "cachedir": False,
        "socket_timeout": 8,
        "retries": 1,
        "extract_flat": "in_playlist" if flat else False,
        "js_runtimes": {"deno": {"path": None}},
    }
    if PROXY_URL:
        result["proxy"] = PROXY_URL
    return result


def ydl_info(url: str, flat: bool = False) -> dict:
    with yt_dlp.YoutubeDL(ydl_options(flat)) as ydl:
        return ydl.extract_info(url, download=False) or {}


def youtube_search(query: str, limit: int, kind: str) -> list[dict]:
    suffix = " official audio" if kind == "audio" else ""
    data = ydl_info(f"ytsearch{limit}:{query}{suffix}", flat=True)
    results = []
    for item in data.get("entries") or []:
        item_id = item.get("id")
        if item_id and VIDEO_ID.fullmatch(item_id):
            results.append({"provider": "youtube", "id": item_id, "title": item.get("title"), "artist": item.get("uploader"), "durationSeconds": item.get("duration"), "kind": kind, "thumbnail": item.get("thumbnail")})
    return results


async def audius_search(query: str, limit: int) -> list[dict]:
    headers = {"Authorization": f"Bearer {AUDIUS_API_KEY}"} if AUDIUS_API_KEY else {}
    try:
        response = await CLIENT.get("https://api.audius.co/v1/tracks/search", params={"query": query, "limit": limit}, headers=headers)
        response.raise_for_status()
        items = response.json().get("data") or []
    except (httpx.HTTPError, ValueError, AttributeError):
        raise HTTPException(status_code=502, detail="Audius unavailable")
    return [{"provider": "audius", "id": str(item["id"]), "title": item.get("title"), "artist": (item.get("user") or {}).get("name"), "durationSeconds": item.get("duration"), "kind": "audio", "thumbnail": (item.get("artwork") or {}).get("480x480")} for item in items if TRACK_ID.fullmatch(str(item.get("id", ""))) and item.get("is_streamable", True) and not item.get("is_stream_gated") and not item.get("stream_conditions")]


def peertube_host() -> str:
    parsed = urlparse(PEERTUBE_INSTANCE)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password or parsed.port:
        return ""
    return parsed.hostname.lower()


async def peertube_api(path: str, params: dict | None = None) -> dict:
    if not peertube_host():
        raise HTTPException(status_code=503, detail="Instancia PeerTube inválida")
    try:
        response = await CLIENT.get(f"{PEERTUBE_INSTANCE}{path}", params=params)
        response.raise_for_status()
        return response.json()
    except (httpx.HTTPError, ValueError):
        raise HTTPException(status_code=502, detail="PeerTube no responde")


async def peertube_search(query: str, limit: int) -> list[dict]:
    try:
        data = await peertube_api("/api/v1/search/videos", {"search": query, "count": limit, "nsfw": "false"})
    except HTTPException:
        raise
    return [{"provider": "peertube", "id": row["uuid"], "title": row.get("name"), "artist": (row.get("channel") or {}).get("displayName"), "durationSeconds": row.get("duration"), "kind": "video", "thumbnail": None} for row in data.get("data", []) if PEERTUBE_ID.fullmatch(str(row.get("uuid", "")))]


async def guarded(coro) -> tuple[list[dict], bool]:
    try:
        return await asyncio.wait_for(coro, timeout=SEARCH_TIMEOUT), False
    except Exception:
        return [], True


@router.get("/providers")
async def providers(x_api_key: str | None = Header(None), key: str | None = Query(None)):
    auth(x_api_key, key)
    names = ["youtube", "youtube_music", "audius"]
    if peertube_host():
        names.append("peertube")
    return {"providers": names, "mediaCacheOnServer": False}


@router.get("/search")
async def search(query: str, kind: str = "audio", provider: str = "all", limit: int = 10, x_api_key: str | None = Header(None), key: str | None = Query(None)):
    auth(x_api_key, key)
    query = query.strip()
    if not 2 <= len(query) <= 120 or kind not in KINDS or provider not in (PROVIDERS | {"all"}) or not 1 <= limit <= 25:
        raise HTTPException(status_code=400, detail="Búsqueda inválida")
    search_key = (query.casefold(), kind, provider, limit)
    cached = SEARCH_RAM.get(search_key)
    if cached and cached[0] > time.monotonic():
        SEARCH_RAM.move_to_end(search_key)
        return cached[1]
    desired = PROVIDERS if provider == "all" else {provider}
    work = []
    if desired & {"youtube", "youtube_music"}:
        work.append(guarded(run_provider_blocking(youtube_search, query, limit, kind)))
    if "audius" in desired and kind == "audio":
        work.append(guarded(audius_search(query, limit)))
    if "peertube" in desired and kind == "video" and peertube_host():
        work.append(guarded(peertube_search(query, limit)))
    if not work:
        return {"results": [], "partial": False}
    result_groups = await asyncio.gather(*work)
    combined = []
    seen = set()
    for group, _failed in result_groups:
        for item in group:
            identity = (item["provider"], item["id"])
            if identity not in seen:
                combined.append(item)
                seen.add(identity)
    result = {"results": combined[:limit * 3], "partial": any(failed for _, failed in result_groups)}
    if not result["partial"]:
        SEARCH_RAM[search_key] = (time.monotonic() + 45, result)
        SEARCH_RAM.move_to_end(search_key)
        while len(SEARCH_RAM) > 100:
            SEARCH_RAM.popitem(last=False)
    return result


@dataclass
class Stream:
    url: str
    headers: dict
    content_type: str
    content_length: int | None
    bitrate: int | None
    expires_at: float
    title: str | None


def video_info(provider: str, item_id: str, kind: str, quality: str) -> Stream:
    root = "music.youtube.com" if provider == "youtube_music" else "www.youtube.com"
    info = ydl_info(f"https://{root}/watch?v={item_id}")
    formats = []
    for form in info.get("formats") or []:
        if form.get("has_drm") or not form.get("url"):
            continue
        if form.get("protocol") not in {"https", "http"}:
            continue
        acodec = form.get("acodec")
        vcodec = form.get("vcodec")
        if acodec in (None, "none") or (kind == "audio" and vcodec not in (None, "none")) or (kind == "video" and vcodec in (None, "none")):
            continue
        formats.append(form)
    if not formats:
        raise HTTPException(status_code=422, detail="No hay formato progresivo compatible; use DASH local para video HD")
    target = ({"low": 800_000, "high": 2_500_000} if kind == "video" else {"low": 96_000, "high": 192_000}).get(quality)
    def bitrate(item):
        return int((item.get("tbr") or item.get("abr") or 0) * 1000)
    formats.sort(key=lambda item: (bitrate(item), item.get("height") or 0))
    chosen = next((item for item in reversed(formats) if bitrate(item) <= target), formats[0]) if target else formats[-1]
    url = chosen["url"]
    args = parse_qs(urlparse(url).query)
    expiry = int(args.get("expire", [int(time.time()) + URL_TTL])[0])
    length = chosen.get("filesize") or chosen.get("filesize_approx") or args.get("clen", [None])[0]
    container = "webm" if chosen.get("ext") == "webm" else "mp4"
    mime = f"{kind}/{container}"
    return Stream(url=url, headers=chosen.get("http_headers") or {}, content_type=mime, content_length=int(length) if length else None, bitrate=bitrate(chosen), expires_at=min(expiry - 60, time.time() + URL_TTL), title=info.get("title"))


async def _get_stream(provider: str, item_id: str, kind: str, quality: str, force: bool = False) -> Stream:
    cache_key = (provider, item_id, kind, quality)
    now = time.time()
    saved = RAM.get(cache_key)
    if not force and saved and saved[0] > now:
        RAM.move_to_end(cache_key)
        return Stream(**saved[1])
    if provider == "audius":
        return Stream(url=f"https://api.audius.co/v1/tracks/{item_id}/stream", headers={"Authorization": f"Bearer {AUDIUS_API_KEY}"} if AUDIUS_API_KEY else {}, content_type="audio/mpeg", content_length=None, bitrate=None, expires_at=now + URL_TTL, title=None)
    async with SEMAPHORE:
        if provider == "peertube":
            info = await peertube_api(f"/api/v1/videos/{item_id}")
            formats = [f for f in info.get("files") or [] if urlparse(f.get("fileUrl") or "").hostname == peertube_host() and urlparse(f.get("fileUrl") or "").scheme == "https"]
            if not formats:
                raise HTTPException(status_code=422, detail="PeerTube no ofrece archivo progresivo local")
            formats.sort(key=lambda f: (f.get("resolution") or {}).get("id") or 0)
            file = formats[0] if quality == "low" else formats[-1]
            stream = Stream(url=file["fileUrl"], headers={}, content_type="video/mp4", content_length=file.get("size"), bitrate=None, expires_at=now + URL_TTL, title=info.get("name"))
        else:
            try:
                stream = await asyncio.wait_for(run_provider_blocking(video_info, provider, item_id, kind, quality), timeout=20)
            except asyncio.TimeoutError:
                raise HTTPException(status_code=504, detail="Resolución agotó el tiempo")
            except HTTPException:
                raise
            except Exception:
                raise HTTPException(status_code=502, detail="Proveedor no disponible")
    if stream.expires_at > now:
        RAM[cache_key] = (stream.expires_at, vars(stream))
        RAM.move_to_end(cache_key)
        while len(RAM) > RAM_LIMIT:
            RAM.popitem(last=False)
    return stream


async def get_stream(provider: str, item_id: str, kind: str, quality: str, force: bool = False) -> Stream:
    cache_key = (provider, item_id, kind, quality)
    if force:
        RAM.pop(cache_key, None)
    task = IN_FLIGHT.get(cache_key)
    if task is None:
        if len(IN_FLIGHT) >= 8:
            raise HTTPException(status_code=429, detail="Resolution queue full")
        task = asyncio.create_task(_get_stream(provider, item_id, kind, quality, force))
        IN_FLIGHT[cache_key] = task
        task.add_done_callback(lambda done: (IN_FLIGHT.pop(cache_key, None), done.exception() if not done.cancelled() else None))
    return await asyncio.shield(task)


def valid_upstream(provider: str, url: str) -> bool:
    if any(ord(char) <= 32 or ord(char) >= 127 for char in url):
        return False
    try:
        parts = urlparse(url)
        host = (parts.hostname or "").lower()
        if parts.scheme != "https" or parts.username or parts.password or parts.port not in (None, 443) or parts.fragment:
            return False
    except ValueError:
        return False
    if provider in {"youtube", "youtube_music"}:
        return host == "googlevideo.com" or host.endswith(".googlevideo.com")
    if provider == "audius":
        return host in AUDIUS_HOSTS
    return provider == "peertube" and bool(peertube_host()) and host == peertube_host()


async def open_upstream(provider: str, url: str, headers: dict) -> httpx.Response:
    safe_headers = {k: v for k, v in headers.items() if k.lower() in {"range", "user-agent", "referer", "origin", "accept", "accept-language"}}
    safe_headers["Accept-Encoding"] = "identity"
    for _ in range(5):
        if not valid_upstream(provider, url):
            raise HTTPException(status_code=502, detail="Origin not allowed")
        parts = urlparse(url)
        if API_KEY and any(API_KEY in values for values in parse_qs(parts.query).values()):
            raise HTTPException(status_code=502, detail="Invalid upstream credentials")
        await public_addresses(parts.hostname, 443)
        response = await CLIENT.send(CLIENT.build_request("GET", url, headers=safe_headers), stream=True)
        if response.status_code in (301, 302, 303, 307, 308):
            location = response.headers.get("location", "")
            next_url = str(httpx.URL(url).join(location))
            await response.aclose()
            if not location:
                raise HTTPException(status_code=502, detail="Missing redirect target")
            if urlparse(next_url).hostname != parts.hostname:
                safe_headers = {k: v for k, v in safe_headers.items() if k.lower() in {"range", "accept", "accept-language", "accept-encoding", "user-agent"}}
            url = next_url
            continue
        return response
    raise HTTPException(status_code=502, detail="Too many redirects")


@router.get("/resolve/{provider}/{item_id}")
async def resolve(provider: str, item_id: str, kind: str = "audio", quality: str = "auto", x_api_key: str | None = Header(None), key: str | None = Query(None)):
    auth(x_api_key, key)
    validate(provider, item_id, kind, quality)
    stream = await get_stream(provider, item_id, kind, quality)
    return {"provider": provider, "id": item_id, "kind": kind, "title": stream.title, "mimeType": stream.content_type, "bitrate": stream.bitrate, "contentLength": stream.content_length, "expiresInSeconds": max(1, int(stream.expires_at - time.time())), "streamPath": f"/v2/stream/{provider}/{item_id}?kind={kind}&quality={quality}"}


@router.api_route("/stream/{provider}/{item_id}", methods=["GET", "HEAD"])
async def stream(provider: str, item_id: str, request: Request, kind: str = "audio", quality: str = "auto", x_api_key: str | None = Header(None), key: str | None = Query(None)):
    auth(x_api_key, key)
    validate(provider, item_id, kind, quality)
    range_header = request.headers.get("range")
    if range_header and not re.fullmatch(r"bytes=(?:\d+-\d*|-\d+)", range_header):
        raise HTTPException(status_code=416, detail="Rango inválido")
    headers = {"Range": range_header} if range_header else {}
    response = None
    resolved = None
    for attempt in (0, 1):
        resolved = await get_stream(provider, item_id, kind, quality, force=(attempt == 1))
        headers.update({k: v for k, v in resolved.headers.items() if k.lower() in {"user-agent", "referer", "origin", "accept", "accept-language"}})
        try:
            response = await open_upstream(provider, resolved.url, headers)
        except httpx.HTTPError:
            raise HTTPException(status_code=502, detail="Error de transporte")
        if response.status_code in (403, 410) and attempt == 0:
            await response.aclose()
            continue
        break
    if response.status_code == 416:
        out = {"Cache-Control": "private, no-store"}
        if "content-range" in response.headers:
            out["Content-Range"] = response.headers["content-range"]
        await response.aclose()
        return Response(status_code=416, headers=out)
    if response.status_code >= 400:
        status = response.status_code
        await response.aclose()
        raise HTTPException(status_code=502, detail=f"Origen HTTP {status}")
    out = {"Cache-Control": "private, no-store"}
    if response.status_code == 206 or response.headers.get("accept-ranges") == "bytes":
        out["Accept-Ranges"] = "bytes"
    for name in ("content-range", "content-length", "content-type"):
        if name in response.headers:
            out[name] = response.headers[name]
    if out.get("content-type", "application/octet-stream").split(";")[0] == "application/octet-stream":
        out["content-type"] = resolved.content_type
    if request.method == "HEAD":
        status = response.status_code
        await response.aclose()
        return Response(status_code=status, headers=out)
    async def iterator():
        try:
            async for block in response.aiter_raw(64 * 1024):
                yield block
        finally:
            await response.aclose()
    return StreamingResponse(iterator(), status_code=response.status_code, headers=out)
OPENTUNE_V2_SOURCE
cat > upstream_security.py <<'OPENTUNE_V2_SOURCE'
import asyncio
import ipaddress
import socket

import httpx
from httpcore._backends.auto import AutoBackend
from httpcore._backends.base import AsyncNetworkBackend


async def public_addresses(host: str, port: int) -> list[str]:
    try:
        records = await asyncio.get_running_loop().getaddrinfo(host, port, type=socket.SOCK_STREAM)
    except OSError as error:
        raise httpx.ConnectError("Upstream DNS unavailable") from error
    addresses = list(dict.fromkeys(row[4][0] for row in records))
    if not addresses:
        raise httpx.ConnectError("Upstream DNS unavailable")
    for value in addresses:
        address = ipaddress.ip_address(value)
        mapped = getattr(address, "ipv4_mapped", None)
        if not address.is_global or address.is_multicast or address.is_reserved or address.is_unspecified or getattr(address,"is_site_local",False) or (mapped is not None and not mapped.is_global):
            raise httpx.ConnectError("Upstream address is not public")
    return addresses


class PublicNetworkBackend(AsyncNetworkBackend):
    def __init__(self):
        self.backend = AutoBackend()

    async def connect_tcp(self, host, port, timeout=None, local_address=None, socket_options=None):
        addresses = await public_addresses(host, port)
        last_error = None
        for address in addresses:
            try:
                return await self.backend.connect_tcp(address, port, timeout, local_address, socket_options)
            except Exception as error:
                last_error = error
        raise last_error

    async def connect_unix_socket(self, path, timeout=None, socket_options=None):
        raise httpx.ConnectError("Unix upstreams are not allowed")

    async def sleep(self, seconds):
        await asyncio.sleep(seconds)


def upstream_transport(proxy=None):
    transport = httpx.AsyncHTTPTransport(proxy=proxy, retries=0, limits=httpx.Limits(max_connections=12, max_keepalive_connections=6))
    transport._pool._network_backend = PublicNetworkBackend()
    return transport
OPENTUNE_V2_SOURCE
cat > main.py <<'OPENTUNE_EOF_main_py'
"""
OpenTune Resolver
=================
Pequeño servidor personal (FastAPI + yt-dlp) que:

  * resuelve el mejor stream de audio de YouTube Music para un videoId y una calidad,
  * y lo retransmite (proxy con soporte de Range) para que el teléfono NO tenga que
    hablar directamente con googlevideo (las URLs van atadas a la IP que las pidió).

Endpoints (todos requieren la clave en el header `X-API-Key` o en `?key=`):
  GET /health
  GET /resolve/{video_id}?quality=auto|low|high|highest   -> JSON con metadatos del formato
  GET /audio/{video_id}?quality=...                        -> audio (Range soportado)

Variables de entorno:
  API_KEY        (obligatoria)  clave compartida con tu app
  COOKIES_FILE   (opcional)     cookies.txt en formato Netscape (cuenta Premium => ~256 kbps)
  URL_TTL_SECS   (opcional)     vida máxima de la caché de resolución (def. 4h)
  PROXY_URL      (opcional)     proxy http/socks para yt-dlp y para el streaming
"""
from __future__ import annotations

import asyncio
import hmac
import logging
import os
import re
import time
from dataclasses import dataclass, asdict
from typing import Optional
from urllib.parse import urlparse, parse_qs

import httpx
import yt_dlp
from fastapi import FastAPI, Header, HTTPException, Query, Request
from fastapi.responses import JSONResponse, Response, StreamingResponse

log = logging.getLogger("opentune-resolver")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

API_KEY = os.environ.get("API_KEY", "")
COOKIES_FILE = os.environ.get("COOKIES_FILE") or None
PROXY_URL = os.environ.get("PROXY_URL") or None
URL_TTL_SECS = min(900, max(60, int(os.environ.get("URL_TTL_SECS", "240"))))

VIDEO_ID_RE = re.compile(r"^[A-Za-z0-9_-]{11}$")
QUALITIES = {"auto", "low", "high", "highest"}

app = FastAPI(title="OpenTune Resolver", docs_url=None, redoc_url=None, openapi_url=None)


# --------------------------------------------------------------------------- auth
def check_key(header_key: Optional[str], query_key: Optional[str]) -> None:
    if not API_KEY:
        raise HTTPException(status_code=500, detail="API_KEY no configurada en el servidor")
    supplied = header_key or query_key or ""
    if not hmac.compare_digest(supplied.encode(), API_KEY.encode()):
        raise HTTPException(status_code=401, detail="clave inválida")


# ------------------------------------------------------------------- resolución
@dataclass
class AudioFormat:
    itag: int
    url: str
    mime_type: str          # p.ej. audio/webm
    codecs: str             # p.ej. opus | mp4a.40.2
    bitrate: int            # bps
    content_length: int
    sample_rate: Optional[int]
    loudness_db: Optional[float]
    headers: dict
    expires_at: float       # epoch seg.


@dataclass
class Resolved:
    video_id: str
    title: Optional[str]
    duration: Optional[int]
    formats: list[AudioFormat]
    created_at: float


_cache: dict[str, Resolved] = {}
_locks: dict[str, asyncio.Lock] = {}


def _ydl_opts() -> dict:
    opts: dict = {
        "quiet": True,
        "logger": ExtractionLogger(),
        "no_warnings": True,
        "skip_download": True,
        "cachedir": False,
        "noplaylist": True,
        "format": "bestaudio/best",
        # yt-dlp necesita un runtime JS para resolver los desafíos de YouTube.
        "js_runtimes": {"deno": {"path": None}, "node": {"path": None}},
        "socket_timeout": 15,
        "retries": 2,
    }
    if COOKIES_FILE and os.path.exists(COOKIES_FILE):
        opts["cookiefile"] = COOKIES_FILE
    if PROXY_URL:
        opts["proxy"] = PROXY_URL
    return opts


def _extract_sync(video_id: str) -> Resolved:
    url = f"https://music.youtube.com/watch?v={video_id}"
    with yt_dlp.YoutubeDL(_ydl_opts()) as ydl:
        info = ydl.extract_info(url, download=False)

    formats: list[AudioFormat] = []
    for f in info.get("formats") or []:
        if f.get("vcodec") not in (None, "none"):
            continue
        acodec = f.get("acodec")
        if not acodec or acodec == "none" or not f.get("url"):
            continue
        if str(f.get("protocol", "")).startswith(("m3u8", "http_dash_segments")):
            continue  # solo descargas HTTP progresivas (Range)

        furl: str = f["url"]
        qs = parse_qs(urlparse(furl).query)
        clen = int(qs["clen"][0]) if "clen" in qs else (f.get("filesize") or f.get("filesize_approx"))
        if not clen:
            continue
        expire = int(qs["expire"][0]) if "expire" in qs else int(time.time()) + URL_TTL_SECS

        ext = (f.get("ext") or "").lower()
        mime = "audio/webm" if ext == "webm" else "audio/mp4" if ext in ("m4a", "mp4") else f"audio/{ext or 'mpeg'}"
        abr = f.get("abr") or f.get("tbr") or 0
        formats.append(
            AudioFormat(
                itag=int(re.sub(r"\D.*", "", str(f.get("format_id", "0"))) or 0),
                url=furl,
                mime_type=mime,
                codecs=acodec,
                bitrate=int(abr * 1000),
                content_length=int(clen),
                sample_rate=f.get("asr"),
                loudness_db=None,
                headers=dict(f.get("http_headers") or {}),
                expires_at=float(expire),
            )
        )

    if not formats:
        raise RuntimeError("no se encontraron formatos de audio")
    return Resolved(
        video_id=video_id,
        title=info.get("title"),
        duration=info.get("duration"),
        formats=formats,
        created_at=time.time(),
    )


def pick_format(formats: list[AudioFormat], quality: str) -> AudioFormat:
    """Calidades: low <= ~70 kbps, high <= ~160 kbps, highest = la mayor, auto = highest."""
    target = {"low": 70_000, "high": 160_000}.get(quality)  # None => highest
    codec_rank = lambda f: 2 if "opus" in f.codecs else 1  # a igualdad de bitrate preferimos opus
    ordered = sorted(formats, key=lambda f: (f.bitrate, codec_rank(f)), reverse=True)
    if target is None:
        return ordered[0]
    below = [f for f in ordered if f.bitrate <= target * 1.05]
    if below:
        return below[0]               # la mejor que no pase del objetivo
    return ordered[-1]                # si todo es mayor, la más pequeña


async def resolve(video_id: str, force: bool = False) -> Resolved:
    now = time.time()
    cached = _cache.get(video_id)
    if cached and not force:
        soonest = min(f.expires_at for f in cached.formats)
        if now < soonest - 300 and now - cached.created_at < URL_TTL_SECS:
            return cached

    lock = _locks.setdefault(video_id, asyncio.Lock())
    async with lock:
        cached = _cache.get(video_id)
        if cached and not force and time.time() - cached.created_at < 30:
            return cached  # otro request acaba de resolver
        try:
            res = await run_provider_blocking(_extract_sync, video_id)
        except Exception as e:  # yt-dlp lanza DownloadError, etc.
            log.warning("fallo al resolver %s: type=%s", video_id, type(e).__name__)
            raise HTTPException(status_code=502, detail="no se pudo resolver")
        _cache[video_id] = res
        if len(_cache) > 500:  # limpieza básica
            for k in sorted(_cache, key=lambda k: _cache[k].created_at)[:100]:
                _cache.pop(k, None)
        if len(_locks) > 500:
            for old in list(_locks):
                if old not in _cache and not _locks[old].locked():
                    _locks.pop(old, None)
        return res


def _validate(video_id: str, quality: str) -> str:
    if not VIDEO_ID_RE.match(video_id):
        raise HTTPException(status_code=400, detail="videoId inválido")
    q = quality.lower()
    if q not in QUALITIES:
        raise HTTPException(status_code=400, detail="quality debe ser auto|low|high|highest")
    return q


# ---------------------------------------------------------------------- rutas
@app.get("/health")
async def health(x_api_key: Optional[str] = Header(None), key: Optional[str] = Query(None)):
    check_key(x_api_key, key)
    return {"ok": True, "yt_dlp": yt_dlp.version.__version__}


@app.get("/resolve/{video_id}")
async def resolve_endpoint(
    video_id: str,
    quality: str = "auto",
    x_api_key: Optional[str] = Header(None),
    key: Optional[str] = Query(None),
):
    check_key(x_api_key, key)
    q = _validate(video_id, quality)
    res = await resolve(video_id)
    fmt = pick_format(res.formats, q)
    return JSONResponse(
        {
            "videoId": video_id,
            "title": res.title,
            "durationSeconds": res.duration,
            "itag": fmt.itag,
            "mimeType": fmt.mime_type,
            "codecs": fmt.codecs,
            "bitrate": fmt.bitrate,
            "contentLength": fmt.content_length,
            "sampleRate": fmt.sample_rate,
            "expiresInSeconds": max(60, int(min(fmt.expires_at - time.time(), URL_TTL_SECS))),
            "audioPath": f"/audio/{video_id}?quality={q}",
            "available": [
                {"itag": f.itag, "codecs": f.codecs, "bitrate": f.bitrate} for f in res.formats
            ],
        }
    )


@app.api_route("/audio/{video_id}", methods=["GET", "HEAD"])
async def audio(
    video_id: str,
    request: Request,
    quality: str = "auto",
    x_api_key: Optional[str] = Header(None),
    key: Optional[str] = Query(None),
):
    check_key(x_api_key, key)
    q = _validate(video_id, quality)

    rng = request.headers.get("range")
    if rng and not re.fullmatch(r"bytes=(?:\d+-\d*|-\d+)", rng):
        raise HTTPException(status_code=416, detail="Rango inválido")
    for attempt in (0, 1):
        res = await resolve(video_id, force=(attempt == 1))
        fmt = pick_format(res.formats, q)

        headers = {k: v for k, v in fmt.headers.items() if k.lower() in ("user-agent", "accept", "accept-language", "referer", "origin")}
        if rng:
            headers["Range"] = rng

        try:
            upstream = await open_upstream("youtube", fmt.url, headers)
        except httpx.HTTPError:
            raise HTTPException(status_code=502, detail="Error de transporte")

        if upstream.status_code in (403, 410) and attempt == 0:
            await upstream.aclose()
            log.info("URL caducada/denegada para %s, re-resolviendo", video_id)
            continue
        break

    if upstream.status_code == 416:
        out = {"Cache-Control": "private, no-store"}
        if "content-range" in upstream.headers:
            out["Content-Range"] = upstream.headers["content-range"]
        await upstream.aclose()
        return Response(status_code=416, headers=out)
    if upstream.status_code >= 400:
        await upstream.aclose()
        raise HTTPException(status_code=502, detail=f"googlevideo respondió {upstream.status_code}")

    out_headers = {"Cache-Control": "private, no-store"}
    if upstream.status_code == 206 or upstream.headers.get("accept-ranges") == "bytes":
        out_headers["Accept-Ranges"] = "bytes"
    for h in ("content-length", "content-range", "content-type"):
        if h in upstream.headers:
            out_headers[h.title()] = upstream.headers[h]
    out_headers.setdefault("Content-Type", fmt.mime_type)

    status = upstream.status_code
    if request.method == "HEAD":
        await upstream.aclose()
        return Response(status_code=status, headers=out_headers)

    async def body():
        try:
            async for chunk in upstream.aiter_raw(64 * 1024):
                yield chunk
        finally:
            await upstream.aclose()

    return StreamingResponse(body(), status_code=status, headers=out_headers)

from multisource import router, open_upstream, run_provider_blocking, close_clients, ExtractionLogger
app.include_router(router)

@app.on_event("shutdown")
async def shutdown():
    await close_clients()
OPENTUNE_EOF_main_py
cat > requirements.txt <<'OPENTUNE_EOF_requirements_txt'
fastapi==0.115.12
uvicorn[standard]==0.34.2
httpx[socks]==0.28.1
httpcore==1.0.9
yt-dlp[default]==2026.10.26
OPENTUNE_EOF_requirements_txt
cat > Dockerfile <<'OPENTUNE_EOF_Dockerfile'
FROM python:3.12-slim

# Deno: yt-dlp lo usa para resolver los desafíos JS de YouTube
COPY --from=denoland/deno:bin /deno /usr/local/bin/deno

RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY main.py multisource.py upstream_security.py run.sh ./
RUN chmod +x run.sh

EXPOSE 8787
CMD ["./run.sh"]
OPENTUNE_EOF_Dockerfile
cat > run.sh <<'OPENTUNE_EOF_run_sh'
#!/bin/sh
set -eu
exec uvicorn main:app --host 0.0.0.0 --port "${PORT:-8787}" --workers 1 --no-access-log
OPENTUNE_EOF_run_sh
chmod +x run.sh
if [ ! -f .env ]; then echo "API_KEY=$(openssl rand -hex 24)" > .env; fi
sed -i '/^DOMAIN=/d' .env; echo "DOMAIN=$DOMAIN" >> .env
chmod 600 .env

if [ -n "$DOMAIN" ]; then
cat > Caddyfile <<'CADDY_EOF'
{$DOMAIN} {
    reverse_proxy opentune-resolver:8787 {
        flush_interval -1
    }
}
CADDY_EOF
cat > docker-compose.yml <<'COMPOSE_EOF'
services:
  opentune-resolver:
    build: .
    container_name: opentune-resolver
    restart: unless-stopped
    read_only: true
    tmpfs:
      - /tmp:size=64m,mode=1777
    expose:
      - "8787"
    environment:
      API_KEY: "${API_KEY:?falta API_KEY}"
      AUDIUS_API_KEY: "${AUDIUS_API_KEY:-}"
      PEERTUBE_INSTANCE: "${PEERTUBE_INSTANCE:-https://video.blender.org}"
      AUDIUS_R2_HOSTS: "${AUDIUS_R2_HOSTS:-}"
      HOME: /tmp
      XDG_CACHE_HOME: /tmp
      DENO_DIR: /tmp/deno
      PYTHONDONTWRITEBYTECODE: "1"
  caddy:
    image: caddy:2
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    environment:
      DOMAIN: "${DOMAIN}"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    depends_on:
      - opentune-resolver
volumes:
  caddy_data:
  caddy_config:
COMPOSE_EOF
  ufw allow OpenSSH >/dev/null; ufw allow 80/tcp >/dev/null; ufw allow 443/tcp >/dev/null
  URL="https://$DOMAIN"
fi
ufw --force enable >/dev/null

echo "==> Construyendo y arrancando (tarda unos minutos)"
docker compose up -d --build

echo "Instalacion preparada. Consulta la configuracion local .env; las credenciales no se imprimen."
