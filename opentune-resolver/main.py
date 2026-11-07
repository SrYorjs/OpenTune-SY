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
URL_TTL_SECS = int(os.environ.get("URL_TTL_SECS", str(4 * 3600)))

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
        "no_warnings": True,
        "skip_download": True,
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
            res = await asyncio.to_thread(_extract_sync, video_id)
        except Exception as e:  # yt-dlp lanza DownloadError, etc.
            log.warning("fallo al resolver %s: %s", video_id, e)
            raise HTTPException(status_code=502, detail=f"no se pudo resolver: {str(e)[:300]}")
        _cache[video_id] = res
        if len(_cache) > 500:  # limpieza básica
            for k in sorted(_cache, key=lambda k: _cache[k].created_at)[:100]:
                _cache.pop(k, None)
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


_client = httpx.AsyncClient(
    timeout=httpx.Timeout(connect=10, read=30, write=10, pool=10),
    follow_redirects=True,
    proxy=PROXY_URL,
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

    for attempt in (0, 1):
        res = await resolve(video_id, force=(attempt == 1))
        fmt = pick_format(res.formats, q)

        headers = {k: v for k, v in fmt.headers.items() if k.lower() in ("user-agent", "accept", "accept-language", "referer", "origin")}
        rng = request.headers.get("range")
        headers["Range"] = rng if rng else "bytes=0-"

        upstream_req = _client.build_request("GET", fmt.url, headers=headers)
        upstream = await _client.send(upstream_req, stream=True)

        if upstream.status_code in (403, 410) and attempt == 0:
            await upstream.aclose()
            log.info("URL caducada/denegada para %s, re-resolviendo", video_id)
            continue
        break

    if upstream.status_code >= 400:
        await upstream.aclose()
        raise HTTPException(status_code=502, detail=f"googlevideo respondió {upstream.status_code}")

    out_headers = {"Accept-Ranges": "bytes", "Cache-Control": "private, max-age=3600"}
    for h in ("content-length", "content-range", "content-type"):
        if h in upstream.headers:
            out_headers[h.title()] = upstream.headers[h]
    out_headers.setdefault("Content-Type", fmt.mime_type)

    status = upstream.status_code
    if not rng and status == 206:
        # El cliente no pidió Range: respondemos 200 con la longitud total.
        status = 200
        total = (out_headers.pop("Content-Range", "").rsplit("/", 1) + [""])[-1]
        if total.isdigit():
            out_headers["Content-Length"] = total

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
