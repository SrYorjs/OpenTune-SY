# Propuesta de contingencia F5: /video/{id}

NO APLICADA. No se ha contactado el VPS. Solo considerar si los logs del telefono muestran agotamiento de todos los clientes directos. No se afirma que eso haya ocurrido. Este bloque es una propuesta para integrar/revisar en opentune-resolver/main.py, no un diff contra un archivo remoto que no hemos leido. No cambia /audio ni formatos de audio. La app actual NO llama este endpoint.

Requiere yt-dlp y httpx en el entorno existente. Reutilizar el middleware/autorizacion y limites de concurrencia de tu servicio antes de publicarlo; aqui no se inventa una clave ni se pide ninguna. Selecciona un unico formato progresivo MP4 h264 con audio <=720p, sin merge, sin ffmpeg ni descargas a disco. Si YouTube no ofrece un formato progresivo con esas propiedades, devuelve 502; no convierte una pista DASH solo-video en un progresivo ficticio. Cache breve en memoria por ID, URL nunca devuelta al cliente, Range reenviado a CDN.

```python
# Proposed additions to the existing FastAPI app (not a standalone server).
import asyncio
import re
import time
from fastapi import HTTPException, Request
from fastapi.responses import StreamingResponse
from starlette.background import BackgroundTask
import httpx
import yt_dlp

_video_cache = {}  # process-local only, (expiry_monotonic, url, upstream_headers)
_video_lock = asyncio.Lock()
_video_extract_limit = asyncio.Semaphore(2)
_VIDEO_ID = re.compile(r"^[A-Za-z0-9_-]{11}$")
_VIDEO_RANGE = re.compile(r"^bytes=(?:\d+-\d*|-\d+)$")


def _extract_progressive(video_id):
    with yt_dlp.YoutubeDL({
        "format": "best[ext=mp4][vcodec^=avc1][acodec!=none][height<=720]",
        "noplaylist": True, "quiet": True, "no_warnings": True,
        "socket_timeout": 15, "retries": 1,
    }) as ydl:
        info = ydl.extract_info("https://www.youtube.com/watch?v=" + video_id,
                                download=False)
    if (not info or not info.get("url") or info.get("ext") != "mp4"
        or not str(info.get("vcodec", "")).startswith("avc1")
        or info.get("acodec") in (None, "none")
        or not 0 < (info.get("height") or 0) <= 720
        or info.get("requested_formats")):
        raise ValueError("No progressive h264 <=720p format")
    # Preserve extractor's real CDN headers; do not log signed URLs or cookies.
    headers = dict(info.get("http_headers") or {})
    return info["url"], headers


async def _video_link(video_id):
    async with _video_lock:
        cached = _video_cache.get(video_id)
        if cached and cached[0] > time.monotonic():
            return cached[1:]
    async with _video_extract_limit:
        # Bound concurrent yt-dlp calls; socket/retry limits also apply inside extractor.
        try:
            url, headers = await asyncio.to_thread(_extract_progressive, video_id)
        except Exception as exc:
            raise HTTPException(502, "Video resolution unavailable") from exc
    async with _video_lock:
        if len(_video_cache) >= 64:
            _video_cache.pop(next(iter(_video_cache)))
        _video_cache[video_id] = (time.monotonic() + 60, url, headers)
    return url, headers


@app.api_route("/video/{video_id}", methods=["GET", "HEAD"])
async def video_stream(video_id: str, request: Request):
    # Apply the service's existing authorization dependency/middleware to this route.
    if not _VIDEO_ID.fullmatch(video_id):
        raise HTTPException(400, "Invalid video ID")
    requested_range = request.headers.get("range")
    if requested_range and not _VIDEO_RANGE.fullmatch(requested_range):
        raise HTTPException(416, "Only a single byte range is supported")
    url, original_headers = await _video_link(video_id)
    headers = {k: v for k, v in original_headers.items()
               if k.lower() not in ("range", "host", "content-length", "accept-encoding")}
    headers["Accept-Encoding"] = "identity"
    if requested_range:
        headers["Range"] = requested_range
    client = httpx.AsyncClient(follow_redirects=True, timeout=httpx.Timeout(20, connect=8))
    try:
        upstream = await client.send(client.build_request(request.method, url, headers=headers), stream=True)
    except Exception as exc:
        await client.aclose()
        raise HTTPException(502, "Video upstream unavailable") from exc
    if upstream.status_code not in (200, 206, 416):
        await upstream.aclose()
        await client.aclose()
        async with _video_lock:
            _video_cache.pop(video_id, None)
        raise HTTPException(502, "Video upstream refused the stream")
    # Forward actual status and range/length, never claim partial content for a full body.
    response_headers = {k: v for k, v in upstream.headers.items()
                        if k.lower() in ("content-type", "content-length", "content-range",
                                         "accept-ranges", "etag", "last-modified")}
    response_headers["Cache-Control"] = "private, no-store"

    async def close_upstream():
        await upstream.aclose()
        await client.aclose()

    return StreamingResponse(upstream.aiter_raw(), status_code=upstream.status_code,
                             headers=response_headers,
                             background=BackgroundTask(close_upstream))
```

Antes de aplicar: adaptar nombres/imports/lifespan y autorizacion reales de main.py; revisar version instalada de yt-dlp. Verificar GET, HEAD y Range bytes=0-1023, un rango cercano al final (206 con Content-Range correcto), sufijo y 416, cancelacion del cliente y que /audio siga sin cambios. No existe prueba remota realizada. Si se aplicara, haria falta un cambio de cliente posterior y explicito para usar /video; este commit no configura ni consume ese endpoint.
