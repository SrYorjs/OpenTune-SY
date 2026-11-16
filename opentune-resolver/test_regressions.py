from pathlib import Path

import httpx
import pytest
from fastapi.testclient import TestClient

from test_multisource import H, client
import main as legacy
import multisource as m


@pytest.mark.parametrize("kind,expected", [("audio", "audio/webm"), ("video", "video/webm")])
def test_webm_mime_matches_the_resolved_media(monkeypatch, kind, expected):
    def info(*args):
        return {"formats": [{"url": "https://rr1.googlevideo.com/a", "protocol": "https", "acodec": "opus", "vcodec": "none" if kind == "audio" else "vp9", "ext": "webm", "tbr": 100, "filesize": 1024}]}
    monkeypatch.setattr(m, "ydl_info", info)
    assert m.video_info("youtube", "abcdefghijk", kind, "auto").content_type == expected


def test_peertube_does_not_claim_audio_only_resolution(monkeypatch):
    monkeypatch.setattr(m, "PEERTUBE_INSTANCE", "https://video.blender.org")
    response = client.get("/v2/resolve/peertube/8f65c679-68f2-4ba8-9368-ca73699955a7?kind=audio", headers=H)
    assert response.status_code == 422


@pytest.mark.parametrize("status,requested", [(200, None), (206, "bytes=2-4"), (206, None), (416, "bytes=100-")])
def test_legacy_stream_preserves_actual_range_contract(monkeypatch, status, requested):
    closed = []
    async def resolved(*args, **kwargs):
        return legacy.Resolved("abcdefghijk", "Title", 10, [legacy.AudioFormat(140, "https://rr1.googlevideo.com/a", "audio/mp4", "mp4a", 128000, 6, None, None, {}, 9999999999)], 1)
    class Body(httpx.AsyncByteStream):
        async def __aiter__(self):
            yield b"234" if status == 206 else b"012345"
        async def aclose(self):
            closed.append(True)
    async def upstream(provider, url, headers):
        assert headers.get("Range") == requested
        output = {"Content-Length": "3" if status == 206 else "6", "Content-Type": "audio/mp4"}
        if status in (206, 416):
            output["Content-Range"] = "bytes 2-4/6" if status == 206 else "bytes */6"
        return httpx.Response(status, headers=output, stream=Body())
    monkeypatch.setattr(legacy, "resolve", resolved)
    monkeypatch.setattr(legacy, "open_upstream", upstream)
    headers = dict(H)
    if requested:
        headers["Range"] = requested
    response = TestClient(legacy.app).get("/audio/abcdefghijk", headers=headers)
    assert response.status_code == status
    if status == 206:
        assert response.headers["content-range"] == "bytes 2-4/6"
        assert response.headers["content-length"] == "3"
        assert response.content == b"234"
    if status == 416:
        assert response.headers["content-range"] == "bytes */6"
    assert closed


def test_legacy_errors_do_not_publish_upstream_secrets(monkeypatch, caplog):
    async def fail(*args):
        raise RuntimeError("https://user:private-credential@example.test")
    monkeypatch.setattr(legacy, "run_provider_blocking", fail)
    response = TestClient(legacy.app).get("/resolve/otherId1234", headers=H)
    assert response.status_code == 502
    assert "private-credential" not in response.text
    assert "private-credential" not in caplog.text


def test_extractor_diagnostics_never_log_credentials_or_signed_urls(caplog):
    import logging
    for options in (m.ydl_options(), legacy._ydl_opts()):
        logger = options["logger"]
        for method in (logger.debug, logger.warning, logger.error):
            method("https://user:private-credential@example.test/file?token=private-token")
    assert "private-credential" not in caplog.text
    assert "private-token" not in caplog.text
    assert logging.getLogger("httpx").level >= logging.WARNING
    assert logging.getLogger("httpcore").level >= logging.WARNING


def test_installer_writes_exact_sources_inside_installation_only():
    root = Path(__file__).parent
    script = (root / "setup-opentune-public.sh").read_text(encoding="utf-8")
    position = script.index("cd /opt/opentune-resolver")
    for name, tag in [("main.py", "OPENTUNE_EOF_main_py"), ("multisource.py", "OPENTUNE_V2_SOURCE"), ("upstream_security.py", "OPENTUNE_V2_SOURCE"), ("requirements.txt", "OPENTUNE_EOF_requirements_txt"), ("Dockerfile", "OPENTUNE_EOF_Dockerfile"), ("run.sh", "OPENTUNE_EOF_run_sh")]:
        marker = f"cat > {name} <<'{tag}'\n"
        assert script.index(marker) > position
        embedded = script.split(marker, 1)[1].split(f"\n{tag}", 1)[0]
        assert embedded == (root / name).read_text(encoding="utf-8").rstrip()
    assert 'URL="http://' not in script
    assert "Ya existe una instalación" in script


def test_updater_adds_missing_settings_to_existing_v2_without_replacing_values(tmp_path, monkeypatch):
    script = (Path(__file__).parent / "upgrade-v2.sh").read_text(encoding="utf-8")
    program = script.split("python3 - <<'PY'\n", 1)[1].split("\nPY\n", 1)[0]
    (tmp_path / "main.py").write_text('"skip_download": True,\n')
    (tmp_path / "Dockerfile").write_text("COPY main.py multisource.py run.sh ./")
    compose = '    restart: unless-stopped\n      API_KEY: "test"\n      PEERTUBE_INSTANCE: "https://existing.example"\n'
    (tmp_path / "docker-compose.yml").write_text(compose)
    monkeypatch.chdir(tmp_path)
    exec(compile(program, "upgrade-v2", "exec"), {})
    value = (tmp_path / "docker-compose.yml").read_text(encoding="utf-8")
    assert value.count("PEERTUBE_INSTANCE:") == 1
    assert 'PEERTUBE_INSTANCE: "https://existing.example"' in value
    assert "AUDIUS_R2_HOSTS:" in value
    assert "HOME:" in value
    assert "read_only: true" in value
    assert 'docker image tag "$old_image" "$rollback_image"' in script
    assert '"$backup/rollback.yml" up -d --no-build' in script
