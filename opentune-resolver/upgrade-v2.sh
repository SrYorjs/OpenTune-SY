#!/usr/bin/env bash
set -Eeuo pipefail
cd /opt/opentune-resolver
for file in main.py Dockerfile run.sh docker-compose.yml .env multisource.py upstream_security.py; do
  if [[ ! -f "$file" ]]; then
    printf 'Falta %s en /opt/opentune-resolver\n' "$file" >&2
    exit 1
  fi
done
if ! command -v docker >/dev/null 2>&1; then
  printf 'Docker no esta instalado\n' >&2
  exit 1
fi
backup="backup-v2-$(date -u +%Y%m%d-%H%M%S)"
mkdir -m 700 "$backup"
for file in main.py Dockerfile run.sh docker-compose.yml; do
  cp -p "$file" "$backup/$file"
done
restore() {
  for file in main.py Dockerfile run.sh docker-compose.yml; do
    cp -p "$backup/$file" "$file"
  done
  printf 'Se restauraron los archivos anteriores desde %s\n' "$backup" >&2
}
trap restore ERR
python3 - <<'PY'
from pathlib import Path

main = Path("main.py")
source = main.read_text()
if "app.include_router(router)" not in source:
    source += "\nfrom multisource import router\napp.include_router(router)\n"
if '"cachedir": False' not in source:
    source = source.replace('"skip_download": True,', '"skip_download": True,\n        "cachedir": False,', 1)
source = source.replace('"Cache-Control": "private, max-age=3600"', '"Cache-Control": "private, no-store"')
main.write_text(source)

dockerfile = Path("Dockerfile")
contents = dockerfile.read_text().replace("COPY main.py run.sh ./", "COPY main.py multisource.py upstream_security.py run.sh ./").replace("COPY main.py multisource.py run.sh ./", "COPY main.py multisource.py upstream_security.py run.sh ./")
if "COPY main.py multisource.py upstream_security.py run.sh ./" not in contents:
    raise RuntimeError("Dockerfile sin COPY esperado")
dockerfile.write_text(contents)

run = Path("run.sh")
run.write_text('#!/bin/sh\nset -eu\nexec uvicorn main:app --host 0.0.0.0 --port "${PORT:-8787}" --workers 1 --no-access-log\n')
run.chmod(0o755)

compose = Path("docker-compose.yml")
contents = compose.read_text()
if "read_only: true" not in contents:
    anchor = "    restart: unless-stopped\n"
    if anchor not in contents:
        raise RuntimeError("Servicio resolver no encontrado en compose")
    contents = contents.replace(anchor, anchor + "    read_only: true\n    tmpfs:\n      - /tmp:size=64m,mode=1777\n", 1)
if "PEERTUBE_INSTANCE:" not in contents:
    lines = contents.splitlines(keepends=True)
    for index, line in enumerate(lines):
        if line.startswith("      API_KEY:"):
            lines.insert(index + 1, """      AUDIUS_API_KEY: "${AUDIUS_API_KEY:-}"
      PEERTUBE_INSTANCE: "${PEERTUBE_INSTANCE:-}"
      HOME: "/tmp"
      XDG_CACHE_HOME: "/tmp"
      DENO_DIR: "/tmp/deno"
      PYTHONDONTWRITEBYTECODE: "1"
      RESOLVE_CONCURRENCY: "${RESOLVE_CONCURRENCY:-2}"
      SEARCH_TIMEOUT: "${SEARCH_TIMEOUT:-5}"
      RESOLUTION_TTL: "${RESOLUTION_TTL:-240}"
""")
            break
    else:
        raise RuntimeError("Variable API_KEY no encontrada en compose")
    contents = "".join(lines)
compose.write_text(contents)
PY
python3 -m py_compile main.py multisource.py
docker compose config --quiet
docker compose up -d --build --no-deps opentune-resolver
trap - ERR
printf 'Actualizacion aplicada. Respaldo en: %s\n' "$backup"
printf 'Verifica con: docker compose ps && docker compose logs --tail=40 opentune-resolver\n'
