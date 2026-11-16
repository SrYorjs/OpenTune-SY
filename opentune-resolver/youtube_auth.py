import logging
import os
import re
from contextlib import ExitStack, contextmanager


log = logging.getLogger("opentune-resolver.youtube")
MAX_COOKIE_BYTES = 1024 * 1024


class CookieConfigurationError(Exception):
    pass


class YouTubeExtractionError(Exception):
    def __init__(self, category):
        self.category = category
        super().__init__(category)


def youtube_error_category(error):
    if isinstance(error, YouTubeExtractionError):
        return error.category
    text = str(error).lower()
    for fragments, category in (
        (("confirm your age", "age-restricted"), "age_verification_required"),
        (("not a bot",), "bot_verification_required"),
        (("cookies are no longer valid", "invalid cookie", "expired cookie"), "cookies_invalid"),
        (("http error 429", "status code 429"), "http_429"),
        (("http error 403", "status code 403"), "http_403"),
        (("private video", "members-only", "not authorized"), "not_authorized"),
        (("sign in", "login required", "authentication required"), "session_required"),
        (("no video formats", "requested format", "no formats"), "no_formats"),
        (("javascript runtime", "challenge solving failed"), "javascript_challenge"),
        (("timed out", "timeout"), "timeout"),
    ):
        if any(fragment in text for fragment in fragments):
            return category
    return "extraction_failed"


def configure_cookiefile(options):
    source = os.environ.get("COOKIES_FILE", "").strip()
    if not source:
        return options
    if not os.path.isfile(source):
        log.warning("stage=cookies category=file_absent")
    elif not os.access(source, os.R_OK):
        log.warning("stage=cookies category=file_unreadable")
    else:
        options["cookiefile"] = source
    return options


def validate_netscape(payload):
    try:
        lines = payload.decode("utf-8-sig").splitlines()
        if not lines or lines[0] not in ("# Netscape HTTP Cookie File", "# HTTP Cookie File"):
            raise ValueError()
        count = 0
        normalized = [lines[0]]
        for line in lines[1:]:
            prefix = ""
            if line.startswith("#HttpOnly_"):
                prefix = "#HttpOnly_"
                line = line[len("#HttpOnly_"):]
            elif not line.strip() or line.startswith("#"):
                normalized.append(line)
                continue
            fields = line.split("\t")
            if len(fields) != 7:
                raise ValueError()
            domain, subdomains, path, secure, expires, name, value = fields
            if not domain or any(char.isspace() for char in domain) or not path.startswith("/"):
                raise ValueError()
            if subdomains not in ("TRUE", "FALSE") or secure not in ("TRUE", "FALSE"):
                raise ValueError()
            if (subdomains == "TRUE") != domain.startswith(".") or (expires and not re.fullmatch(r"[0-9]{1,20}(?:\.[0-9]{1,9})?", expires)):
                raise ValueError()
            fields[4] = expires.partition(".")[0]
            normalized.append(prefix + "\t".join(fields))
            count += 1
        if not count:
            raise ValueError()
        return ("\n".join(normalized) + "\n").encode("utf-8")
    except (UnicodeError, ValueError):
        raise CookieConfigurationError("file_invalid") from None


@contextmanager
def memory_cookiefile(source):
    if not hasattr(os, "memfd_create"):
        raise CookieConfigurationError("memory_unavailable")
    try:
        with open(source, "rb") as original:
            payload = original.read(MAX_COOKIE_BYTES + 1)
        if len(payload) > MAX_COOKIE_BYTES:
            raise CookieConfigurationError("file_too_large")
        payload = validate_netscape(payload)
    except OSError:
        raise CookieConfigurationError("file_unreadable") from None
    try:
        descriptor = os.memfd_create("youtube-session", os.MFD_CLOEXEC)
    except OSError:
        raise CookieConfigurationError("memory_unavailable") from None
    try:
        os.fchmod(descriptor, 0o600)
        with os.fdopen(os.dup(descriptor), "wb") as target:
            target.write(payload)
        yield f"/proc/self/fd/{descriptor}"
    finally:
        os.close(descriptor)


@contextmanager
def private_cookie_options(options):
    configured = dict(options)
    source = configured.pop("cookiefile", None)
    with ExitStack() as stack:
        if source:
            try:
                configured["cookiefile"] = stack.enter_context(memory_cookiefile(source))
            except (CookieConfigurationError, OSError) as error:
                category = str(error) if isinstance(error, CookieConfigurationError) else "memory_unavailable"
                log.warning("stage=cookies category=%s", category)
        yield configured


def extract_youtube_info(url, options, factory):
    for attempt in range(2):
        requested = dict(options)
        if attempt:
            requested.pop("cookiefile", None)
        with private_cookie_options(requested) as configured:
            try:
                with factory(configured) as extractor:
                    info = extractor.extract_info(url, download=False) or {}
                log.info("stage=extract category=%s", "cookiejar_used" if "cookiefile" in configured else "anonymous_ok")
                return info
            except Exception as error:
                category = youtube_error_category(error)
                log.warning("stage=extract category=%s", category)
                if not attempt and "cookiefile" in configured and category in ("cookies_invalid", "session_required", "bot_verification_required"):
                    continue
                raise YouTubeExtractionError(category) from None
