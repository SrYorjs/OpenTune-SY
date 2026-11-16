import importlib
import os
import sys
import types

os.environ["API_KEY"] = "test-server-key"
if "yt_dlp" not in sys.modules:
    sys.modules["yt_dlp"] = types.SimpleNamespace(YoutubeDL=object)
sys.path.insert(0, os.path.dirname(__file__))
import multisource as m

from fastapi import FastAPI
from fastapi.testclient import TestClient
import pytest

app = FastAPI()
app.include_router(m.router)
client = TestClient(app)
H = {"X-API-Key": "test-server-key"}


def test_auth_and_config():
    assert client.get("/v2/providers").status_code == 401
    response = client.get("/v2/providers", headers=H)
    assert response.status_code == 200
    assert "audius" in response.json()["providers"]
    assert response.json()["mediaCacheOnServer"] is False


def test_invalid_media_identifiers():
    assert client.get("/v2/resolve/youtube/../../etc/passwd", headers=H).status_code != 200
    assert client.get("/v2/resolve/youtube/bad", headers=H).status_code == 400
    assert client.get("/v2/resolve/audius/abc?kind=video", headers=H).status_code == 422


def test_search_checks_and_merge(monkeypatch):
    async def fake_audio(q, limit):
        return [{"provider": "audius", "id": "abc", "kind": "audio"}]
    def fake_youtube(q, limit, kind):
        return [{"provider": "youtube", "id": "dQw4w9WgXcQ", "kind": kind}]
    monkeypatch.setattr(m, "audius_search", fake_audio)
    monkeypatch.setattr(m, "youtube_search", fake_youtube)
    assert client.get("/v2/search?query=x", headers=H).status_code == 400
    response = client.get("/v2/search?query=music&kind=audio", headers=H)
    assert response.status_code == 200
    assert {x["provider"] for x in response.json()["results"]} == {"audius", "youtube"}


def test_url_allowlist():
    assert m.valid_upstream("youtube", "https://rr1.googlevideo.com/video")
    assert not m.valid_upstream("youtube", "http://127.0.0.1/")
    assert not m.valid_upstream("youtube", "https://googlevideo.com.evil.com/")
    assert not m.valid_upstream("audius", "https://127.0.0.1/")


def test_youtube_select_and_no_download(monkeypatch):
    def fake_info(url, flat=False):
        return {"title": "Audio", "formats": [
            {"url": "https://rr1.googlevideo.com/a", "protocol": "https", "acodec": "opus", "vcodec": "none", "abr": 96, "ext": "webm", "filesize": 55},
            {"url": "https://rr1.googlevideo.com/b", "protocol": "https", "acodec": "mp4a", "vcodec": "h264", "tbr": 1700, "ext": "mp4", "height": 360, "filesize": 200},
        ]}
    monkeypatch.setattr(m, "ydl_info", fake_info)
    res = m.video_info("youtube", "dQw4w9WgXcQ", "video", "highest")
    assert res.content_type == "video/mp4"
    assert res.content_length == 200
    assert res.bitrate == 1_700_000
    assert m.ydl_options()["skip_download"] is True
    assert m.ydl_options()["cachedir"] is False


def test_audio_resolve_and_range(monkeypatch):
    async def fake_stream(provider, item_id, kind, quality, force=False):
        return m.Stream("https://api.audius.co/v1/tracks/abc/stream", {}, "audio/mpeg", 6, 128000, 9000000000, "test")
    async def fake_upstream(provider, url, headers):
        assert headers.get("Range") == "bytes=2-4"
        class ByteStream(__import__("httpx").AsyncByteStream):
            async def __aiter__(self):
                yield b"234"
            async def aclose(self):
                pass
        return __import__("httpx").Response(206, stream=ByteStream(), headers={"Content-Range": "bytes 2-4/6", "Content-Length": "3", "Content-Type": "audio/mpeg"})
    monkeypatch.setattr(m, "get_stream", fake_stream)
    monkeypatch.setattr(m, "open_upstream", fake_upstream)
    response = client.get("/v2/stream/audius/abc", headers={**H, "Range": "bytes=2-4"})
    assert response.status_code == 206
    assert response.content == b"234"
    assert response.headers["Cache-Control"] == "private, no-store"
