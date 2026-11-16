import io
import types
from contextlib import contextmanager
from pathlib import Path

import pytest

from test_multisource import H, client
import main as legacy
import multisource as m
import youtube_auth as auth


PAYLOAD = b"# Netscape HTTP Cookie File\n.example.test\tTRUE\t/\tTRUE\t9999999999\tfixture\tfixture\n"


@pytest.fixture
def configured(monkeypatch, tmp_path):
    source = tmp_path / "synthetic-session.txt"
    source.write_bytes(PAYLOAD)
    monkeypatch.setenv("COOKIES_FILE", str(source))
    return source


@pytest.fixture
def working_copy(monkeypatch, tmp_path):
    @contextmanager
    def copied(source):
        path = tmp_path / "synthetic-working.txt"
        path.write_bytes(Path(source).read_bytes())
        try:
            yield str(path)
        finally:
            path.unlink(missing_ok=True)
    monkeypatch.setattr(auth, "memory_cookiefile", copied)


def test_without_cookies_both_option_sets_remain_public(monkeypatch):
    monkeypatch.delenv("COOKIES_FILE", raising=False)
    assert "cookiefile" not in m.ydl_options()
    assert "cookiefile" not in legacy._ydl_opts()


def test_existing_configured_file_is_used_by_both_option_sets(configured):
    assert m.ydl_options()["cookiefile"] == str(configured)
    assert m.ydl_options(flat=True)["cookiefile"] == str(configured)
    assert legacy._ydl_opts()["cookiefile"] == str(configured)


def test_absent_file_is_optional_and_path_is_not_logged(monkeypatch, tmp_path, caplog):
    source = tmp_path / "not-for-logs.txt"
    monkeypatch.setenv("COOKIES_FILE", str(source))
    assert "cookiefile" not in m.ydl_options()
    assert "cookiefile" not in legacy._ydl_opts()
    assert "file_absent" in caplog.text
    assert str(source) not in caplog.text


def test_unreadable_file_does_not_block_public_requests(configured, monkeypatch, caplog):
    monkeypatch.setattr(auth.os, "access", lambda *args: False)
    assert "cookiefile" not in m.ydl_options()
    assert "cookiefile" not in legacy._ydl_opts()
    assert "file_unreadable" in caplog.text
    assert str(configured) not in caplog.text


def test_cookiejar_copy_is_anonymous_ram_only_with_restricted_mode(configured, monkeypatch):
    recorded = {}
    class Memory(io.BytesIO):
        def close(self):
            recorded["payload"] = self.getvalue()
            super().close()
    fake_os = types.SimpleNamespace(
        MFD_CLOEXEC=1,
        memfd_create=lambda name, flags: recorded.setdefault("descriptor", 123),
        fchmod=lambda descriptor, mode: recorded.update(mode=mode),
        dup=lambda descriptor: 124,
        fdopen=lambda descriptor, mode: Memory(),
        close=lambda descriptor: recorded.update(closed=descriptor),
    )
    monkeypatch.setattr(auth, "os", fake_os)
    with auth.memory_cookiefile(str(configured)) as path:
        assert path == "/proc/self/fd/123"
        assert recorded["payload"] == PAYLOAD
        assert recorded["mode"] == 0o600
    assert recorded["closed"] == 123
    assert configured.read_bytes() == PAYLOAD


@pytest.mark.skipif(not hasattr(auth.os, "memfd_create"), reason="Linux memfd required")
def test_linux_cookiejar_can_save_to_ram_without_touching_readonly_source(configured):
    from http.cookiejar import MozillaCookieJar
    with auth.memory_cookiefile(str(configured)) as path:
        jar = MozillaCookieJar(path)
        jar.load(ignore_discard=True, ignore_expires=True)
        assert len(jar) == 1
        jar.save(ignore_discard=True, ignore_expires=True)
        assert configured.read_bytes() == PAYLOAD
    assert not Path(path).exists()


@pytest.mark.parametrize("payload", [b"fixture-invalid", b"# Netscape HTTP Cookie File\nfixture-invalid\n", b"# Netscape HTTP Cookie File\n", b"# Netscape HTTP Cookie File\n.example.test\tFALSE\t/\tTRUE\t9999999999\tfixture\tfixture\n"])
def test_invalid_netscape_data_is_rejected_without_values(payload):
    with pytest.raises(auth.CookieConfigurationError, match="^file_invalid$"):
        auth.validate_netscape(payload)


def test_netscape_normalization_preserves_values_and_supports_crlf_http_only_and_fractional_expiry():
    payload = b"\xef\xbb\xbf# Netscape HTTP Cookie File\r\n  \r\n#HttpOnly_.example.test\tTRUE\t/\tTRUE\t9999999999.5\tfixture\tfixture\r\n"
    normalized = auth.validate_netscape(payload)
    assert normalized.startswith(b"# Netscape HTTP Cookie File\n")
    assert b"#HttpOnly_.example.test\tTRUE\t/\tTRUE\t9999999999\tfixture\tfixture\n" in normalized


def test_non_ascii_expiry_never_reaches_cookiejar_warning_output():
    payload = "# Netscape HTTP Cookie File\n.example.test\tTRUE\t/\tTRUE\t٩٩\tfixture\tfixture\n".encode()
    with pytest.raises(auth.CookieConfigurationError, match="^file_invalid$"):
        auth.validate_netscape(payload)


def test_oversized_integer_never_reaches_cookiejar_warning_output():
    payload = PAYLOAD.replace(b"9999999999", b"9" * 5000)
    with pytest.raises(auth.CookieConfigurationError, match="^file_invalid$"):
        auth.validate_netscape(payload)


def test_memory_storage_failure_falls_back_without_reading_original(configured, monkeypatch, caplog):
    monkeypatch.setattr(auth, "os", types.SimpleNamespace())
    with auth.private_cookie_options({"cookiefile": str(configured)}) as options:
        assert "cookiefile" not in options
    assert "memory_unavailable" in caplog.text
    assert str(configured) not in caplog.text


def test_permission_race_falls_back_without_exposing_exception(configured, monkeypatch, caplog):
    def denied(*args, **kwargs):
        raise PermissionError("not-for-logs")
    monkeypatch.setattr(auth, "os", types.SimpleNamespace(memfd_create=lambda *args: 123))
    monkeypatch.setattr(auth, "open", denied, raising=False)
    with auth.private_cookie_options({"cookiefile": str(configured)}) as options:
        assert "cookiefile" not in options
    assert "file_unreadable" in caplog.text
    assert "not-for-logs" not in caplog.text


def test_invalid_file_uses_public_extraction_without_values(configured, monkeypatch, caplog):
    configured.write_bytes(b"not-for-logs")
    monkeypatch.setattr(auth, "os", types.SimpleNamespace(memfd_create=lambda *args: 123))
    class Extractor:
        def __init__(self, options): assert "cookiefile" not in options
        def __enter__(self): return self
        def __exit__(self, *args): pass
        def extract_info(self, *args, **kwargs): return {"id": "Mcj75l2gJcY"}
    assert auth.extract_youtube_info("fixture", {"cookiefile": str(configured)}, Extractor)["id"] == "Mcj75l2gJcY"
    assert "file_invalid" in caplog.text
    assert "not-for-logs" not in caplog.text


@pytest.mark.parametrize("provider", ["youtube", "youtube_music", "legacy"])
def test_real_extractors_use_working_cookiejar_and_never_modify_source(configured, working_copy, monkeypatch, provider):
    calls = []
    class Extractor:
        def __init__(self, options):
            assert options["cookiefile"] != str(configured)
            self.path = options["cookiefile"]
        def __enter__(self): return self
        def __exit__(self, *args):
            with open(self.path, "wb") as target:
                target.write(b"synthetic-updated")
        def extract_info(self, url, download):
            calls.append(url)
            assert download is False
            return {"title": "Fixture", "duration": 10, "formats": [{"url": "https://rr1.googlevideo.com/fixture", "protocol": "https", "acodec": "mp4a.40.2", "vcodec": "none", "ext": "mp4", "abr": 128, "filesize": 1024, "format_id": "140"}]}
    monkeypatch.setattr(m.yt_dlp, "YoutubeDL", Extractor)
    if provider == "legacy":
        assert legacy._extract_sync("Mcj75l2gJcY").video_id == "Mcj75l2gJcY"
    else:
        assert m.video_info(provider, "Mcj75l2gJcY", "audio", "auto").content_type == "audio/mp4"
    host = "www.youtube.com" if provider == "youtube" else "music.youtube.com"
    assert calls == [f"https://{host}/watch?v=Mcj75l2gJcY"]
    assert configured.read_bytes() == PAYLOAD


@pytest.mark.parametrize("message,category,retries", [
    ("cookies are no longer valid", "cookies_invalid", 2),
    ("Sign in to continue", "session_required", 2),
    ("Sign in to confirm you're not a bot", "bot_verification_required", 2),
    ("Sign in to confirm your age", "age_verification_required", 1),
    ("Private video", "not_authorized", 1),
    ("HTTP Error 403", "http_403", 1),
    ("HTTP Error 429", "http_429", 1),
])
def test_session_failures_are_bounded_and_never_bypass_authorization(configured, working_copy, caplog, message, category, retries):
    calls = []
    class Extractor:
        def __init__(self, options): calls.append(options)
        def __enter__(self): return self
        def __exit__(self, *args): pass
        def extract_info(self, *args, **kwargs):
            if len(calls) == 1:
                raise RuntimeError(message + " not-for-logs")
            return {"id": "Mcj75l2gJcY"}
    if retries == 1:
        with pytest.raises(auth.YouTubeExtractionError, match=f"^{category}$"):
            auth.extract_youtube_info("fixture", {"cookiefile": str(configured)}, Extractor)
    else:
        assert auth.extract_youtube_info("fixture", {"cookiefile": str(configured)}, Extractor)["id"] == "Mcj75l2gJcY"
        assert "cookiefile" not in calls[1]
    assert len(calls) == retries
    assert category in caplog.text
    assert "not-for-logs" not in caplog.text


@pytest.mark.parametrize("kind,provider", [("audio", "audius"), ("video", "peertube")])
def test_youtube_session_errors_do_not_block_independent_providers(monkeypatch, kind, provider):
    def failed(*args): raise auth.YouTubeExtractionError("session_required")
    async def available(*args):
        return [{"provider": provider, "id": "fixture", "kind": kind}]
    monkeypatch.setattr(m, "youtube_search", failed)
    monkeypatch.setattr(m, "audius_search" if provider == "audius" else "peertube_search", available)
    monkeypatch.setattr(m, "PEERTUBE_INSTANCE", "https://video.blender.org")
    response = client.get(f"/v2/search?query=session-fixture-{kind}&kind={kind}&provider=all", headers=H)
    assert response.status_code == 200
    assert response.json()["partial"] is True
    assert [row["provider"] for row in response.json()["results"]] == [provider]


def test_updater_preserves_existing_cookie_variable_and_readonly_mount(tmp_path, monkeypatch):
    root = Path(__file__).parent
    script = (root / "upgrade-v2.sh").read_text(encoding="utf-8")
    program = script.split("python3 - <<'PY'\n", 1)[1].split("\nPY\n", 1)[0]
    (tmp_path / "main.py").write_text('"skip_download": True,\n')
    (tmp_path / "Dockerfile").write_text("COPY main.py multisource.py upstream_security.py run.sh ./")
    compose = '    restart: unless-stopped\n      API_KEY: "fixture"\n      COOKIES_FILE: "/already-available/session.txt"\n    volumes:\n      - /outside-git/fixture.txt:/already-available/session.txt:ro\n'
    (tmp_path / "docker-compose.yml").write_text(compose)
    monkeypatch.chdir(tmp_path)
    exec(compile(program, "upgrade-v2", "exec"), {})
    updated = (tmp_path / "docker-compose.yml").read_text()
    assert updated.count("COOKIES_FILE:") == 1
    assert 'COOKIES_FILE: "/already-available/session.txt"' in updated
    assert '    volumes:\n      - /outside-git/fixture.txt:/already-available/session.txt:ro\n' in updated
    assert "youtube_auth.py" in (tmp_path / "Dockerfile").read_text()


def test_cookie_mount_is_opt_in_and_secrets_are_excluded_from_image_context():
    root = Path(__file__).parent
    overlay = (root / "docker-compose.cookies.yml").read_text(encoding="utf-8")
    assert "YOUTUBE_COOKIES_PATH:?" in overlay
    assert "read_only: true" in overlay
    assert "create_host_path: false" in overlay
    assert "  caddy:" not in overlay
    permitted = [line[1:] for line in (root / ".dockerignore").read_text().splitlines() if line.startswith("!")]
    assert "youtube_auth.py" in permitted
    assert ".env" not in permitted
    assert not any("cookies" in filename for filename in permitted)
