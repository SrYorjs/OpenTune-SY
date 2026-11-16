from __future__ import annotations

import asyncio
import hmac
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
    if provider == "audius" and kind == "video":
        raise HTTPException(status_code=422, detail="Audius solo ofrece audio")


def ydl_options(flat: bool = False) -> dict:
    result = {
        "quiet": True,
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
    mime = "audio/webm" if chosen.get("ext") == "webm" else "audio/mp4" if kind == "audio" else "video/mp4" if chosen.get("ext") == "mp4" else "video/webm"
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
