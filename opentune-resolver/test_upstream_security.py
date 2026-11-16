import asyncio
import socket

import httpx
import pytest
from fastapi import HTTPException
import multisource as m
from upstream_security import PublicNetworkBackend, public_addresses


@pytest.mark.parametrize("address", ["127.0.0.1", "10.1.2.3", "169.254.169.254", "192.168.1.1", "172.16.0.1", "0.0.0.0", "224.0.0.1", "::1", "fe80::1", "::ffff:127.0.0.1"])
def test_private_dns_is_rejected(monkeypatch, address):
    async def run():
        async def lookup(*args, **kwargs):
            return [(socket.AF_INET6 if ":" in address else socket.AF_INET, socket.SOCK_STREAM, 6, "", (address, 443))]
        monkeypatch.setattr(asyncio.get_running_loop(), "getaddrinfo", lookup)
        with pytest.raises(httpx.ConnectError):
            await public_addresses("api.audius.co",443)
    asyncio.run(run())


def test_connection_is_pinned_to_validated_ip(monkeypatch):
    async def run():
        async def lookup(*args, **kwargs):
            return [(socket.AF_INET,socket.SOCK_STREAM,6,"",("8.8.8.8",443))]
        monkeypatch.setattr(asyncio.get_running_loop(), "getaddrinfo", lookup)
        backend = PublicNetworkBackend()
        calls = []
        async def connect(host,port,*args):
            calls.append((host,port))
            return object()
        monkeypatch.setattr(backend.backend,"connect_tcp",connect)
        await backend.connect_tcp("api.audius.co",443)
        assert calls == [("8.8.8.8",443)]
    asyncio.run(run())


@pytest.mark.parametrize("url", ["https://api.audius.co.evil.test/a", "https://evil.r2.cloudflarestorage.com/a", "http://api.audius.co/a", "https://user:password@api.audius.co/a", "https://api.audius.co:1234/a", "https://api.audius.co./a", "https://api.audius.co/a\n", "https://127.0.0.1/a", "https://[::1]/a"])
def test_allowlist_is_not_an_open_proxy(url):
    assert not m.valid_upstream("audius",url)


def test_verified_audius_redirect_chain_strips_credentials(monkeypatch):
    urls = ["https://api.audius.co/v1/tracks/abc/stream", "https://creatornode.audius.co/content", "https://validator.eeba4a6ca56a0d87af802270217c2a51.r2.cloudflarestorage.com/object"]
    calls = []
    responses = []
    class Client:
        def build_request(self, method, url, headers):
            return httpx.Request(method,url,headers=headers)
        async def send(self, request, stream):
            calls.append(request)
            assert str(request.url) == urls[len(calls)-1]
            response = httpx.Response(302,headers={"Location":urls[len(calls)]}) if len(calls)<3 else httpx.Response(206,content=b"abc",headers={"Content-Range":"bytes 2-4/6","Content-Length":"3"})
            responses.append(response)
            return response
    async def public(*args): return ["8.8.8.8"]
    monkeypatch.setattr(m,"CLIENT",Client())
    monkeypatch.setattr(m,"public_addresses",public)
    async def run():
        response = await m.open_upstream("audius",urls[0],{"Range":"bytes=2-4","Authorization":"Bearer private","X-API-Key":"private","Cookie":"private"})
        assert response.status_code == 206
        assert await response.aread() == b"abc"
        await response.aclose()
    asyncio.run(run())
    assert all(response.is_closed for response in responses)
    for request in calls:
        assert request.headers["Range"] == "bytes=2-4"
        assert "authorization" not in request.headers
        assert "x-api-key" not in request.headers
        assert "cookie" not in request.headers


def test_redirect_loop_is_bounded_and_responses_are_closed(monkeypatch):
    responses = []
    class Client:
        def build_request(self,method,url,headers): return httpx.Request(method,url,headers=headers)
        async def send(self,request,stream):
            response = httpx.Response(302,headers={"Location":"https://api.audius.co/again"})
            responses.append(response)
            return response
    async def public(*args): return ["8.8.8.8"]
    monkeypatch.setattr(m,"CLIENT",Client())
    monkeypatch.setattr(m,"public_addresses",public)
    async def run():
        with pytest.raises(HTTPException):
            await m.open_upstream("audius","https://api.audius.co/start",{})
    asyncio.run(run())
    assert len(responses) == 5
    assert all(response.is_closed for response in responses)


def test_concurrent_resolution_is_shared_without_caching_media(monkeypatch):
    calls = []
    async def resolve(*args):
        calls.append(args)
        await asyncio.sleep(.02)
        return m.Stream("https://rr1.googlevideo.com/file",{},"audio/mp4",100,128000,9999999999,None)
    monkeypatch.setattr(m,"_get_stream",resolve)
    async def run():
        results = await asyncio.gather(*(m.get_stream("youtube","abcdefghijk","audio","auto") for _ in range(6)))
        assert len(calls) == 1
        assert len(results) == 6
    asyncio.run(run())
    assert not m.IN_FLIGHT


def test_legacy_and_runtime_have_no_media_disk_cache():
    from pathlib import Path
    root = Path(__file__).parent
    main = (root/"main.py").read_text()
    assert '"cachedir": False' in main
    assert '"private, no-store"' in main
    assert "app.include_router(router)" in main
    assert '"/audio/{video_id}"' in main
    assert '"/resolve/{video_id}"' in main
    run = (root/"run.sh").read_text()
    assert "pip install" not in run
    assert "--no-access-log" in run
    compose = (root/"docker-compose.yml").read_text()
    assert "read_only: true" in compose
    assert "/tmp:size=64m" in compose


def test_stream_disconnect_closes_upstream(monkeypatch):
    closed = []
    class Bytes(httpx.AsyncByteStream):
        async def __aiter__(self):
            yield b"abc"
            yield b"def"
        async def aclose(self): closed.append(True)
    async def resolved(*args, **kwargs):
        return m.Stream("https://api.audius.co/file",{},"audio/mpeg",6,None,9999999999,None)
    async def upstream(*args):
        return httpx.Response(200,stream=Bytes(),headers={"Content-Length":"6","Content-Type":"application/octet-stream"})
    monkeypatch.setattr(m,"get_stream",resolved)
    monkeypatch.setattr(m,"open_upstream",upstream)
    async def run():
        from starlette.requests import Request
        request = Request({"type":"http","method":"GET","headers":[]})
        response = await m.stream("audius","abc",request,x_api_key="test-server-key")
        assert response.headers["Content-Type"] == "audio/mpeg"
        iterator = response.body_iterator
        assert await iterator.__anext__() == b"abcdef"
        await iterator.aclose()
    asyncio.run(run())
    assert closed


@pytest.mark.parametrize("code", [401,403,404,429,502])
def test_upstream_failures_never_expose_credentials(monkeypatch, code):
    from test_multisource import client,H
    async def resolved(*args, **kwargs):
        return m.Stream("https://api.audius.co/file",{},"audio/mpeg",6,None,9999999999,None)
    async def upstream(*args): return httpx.Response(code,content=b"private-upstream-value")
    monkeypatch.setattr(m,"get_stream",resolved)
    monkeypatch.setattr(m,"open_upstream",upstream)
    response = client.get("/v2/stream/audius/abc",headers=H)
    assert response.status_code == 502
    assert "private-upstream-value" not in response.text
    assert "test-server-key" not in response.text


def test_timeout_returns_transport_error_without_secret(monkeypatch):
    from test_multisource import client,H
    async def resolved(*args, **kwargs):
        return m.Stream("https://api.audius.co/file",{},"audio/mpeg",6,None,9999999999,None)
    async def upstream(*args): raise httpx.ReadTimeout("private-upstream-value")
    monkeypatch.setattr(m,"get_stream",resolved)
    monkeypatch.setattr(m,"open_upstream",upstream)
    response = client.get("/v2/stream/audius/abc",headers=H)
    assert response.status_code == 502
    assert "private-upstream-value" not in response.text


@pytest.mark.parametrize("copy", ["COPY main.py run.sh ./", "COPY main.py multisource.py run.sh ./", "COPY main.py multisource.py upstream_security.py run.sh ./"])
def test_upgrade_accepts_legacy_and_current_dockerfiles(tmp_path, monkeypatch, copy):
    from pathlib import Path
    script = (Path(__file__).parent / "upgrade-v2.sh").read_text()
    program = script.split("python3 - <<'PY'\n", 1)[1].split("\nPY\n", 1)[0]
    (tmp_path / "main.py").write_text('"skip_download": True,\n')
    (tmp_path / "Dockerfile").write_text(copy)
    (tmp_path / "docker-compose.yml").write_text('    restart: unless-stopped\n      API_KEY: "test"\n')
    monkeypatch.chdir(tmp_path)
    exec(compile(program, "upgrade-v2", "exec"), {})
    assert (tmp_path / "Dockerfile").read_text() == "COPY main.py multisource.py upstream_security.py run.sh ./"
    assert "read_only: true" in (tmp_path / "docker-compose.yml").read_text()
