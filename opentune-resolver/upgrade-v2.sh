#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
release_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
target_dir="$(cd -- "${RESOLVER_DIR:-/opt/opentune-resolver}" && pwd -P)"
if [[ "$release_dir" == "$target_dir" ]]; then
  printf 'Ejecuta la actualización desde una copia de la revisión, separada de la instalación.\n' >&2
  exit 1
fi
files=(main.py multisource.py upstream_security.py requirements.txt Dockerfile run.sh docker-compose.yml)
for file in main.py multisource.py upstream_security.py requirements.txt Dockerfile run.sh; do
  test -f "$release_dir/$file"
done
cd -- "$target_dir"
test -f .env
test -f docker-compose.yml
command -v docker >/dev/null
command -v python3 >/dev/null
docker compose config --quiet
old_image="$(docker compose images -q opentune-resolver | head -n1)"
test -n "$old_image"
backup="$target_dir/backup-v2-$(date -u +%Y%m%d-%H%M%S)"
mkdir -m 700 -- "$backup"
for file in "${files[@]}" .env Caddyfile; do
  if [[ -f "$file" ]]; then cp -p -- "$file" "$backup/$file"; fi
done
rollback_image="opentune-resolver:rollback-$(date -u +%Y%m%d-%H%M%S)"
docker image tag "$old_image" "$rollback_image"
printf 'services:\n  opentune-resolver:\n    image: %s\n' "$rollback_image" > "$backup/rollback.yml"
restore() {
  trap - ERR
  for file in "${files[@]}"; do
    if [[ -f "$backup/$file" ]]; then cp -p -- "$backup/$file" "$file"; else rm -f -- "$file"; fi
  done
  docker compose -f docker-compose.yml -f "$backup/rollback.yml" up -d --no-build --no-deps opentune-resolver
  printf 'Restaurada la revisión y la imagen anterior. Respaldo: %s\n' "$backup" >&2
}
trap restore ERR
for file in main.py multisource.py upstream_security.py requirements.txt Dockerfile run.sh; do
  cp -- "$release_dir/$file" "$file"
done
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
values = {
    "AUDIUS_API_KEY": "\"${AUDIUS_API_KEY:-}\"",
    "PEERTUBE_INSTANCE": "\"${PEERTUBE_INSTANCE:-https://video.blender.org}\"",
    "AUDIUS_R2_HOSTS": "\"${AUDIUS_R2_HOSTS:-}\"",
    "HOME": "\"/tmp\"", "XDG_CACHE_HOME": "\"/tmp\"", "DENO_DIR": "\"/tmp/deno\"",
    "PYTHONDONTWRITEBYTECODE": "\"1\"", "RESOLVE_CONCURRENCY": "\"${RESOLVE_CONCURRENCY:-2}\"",
    "SEARCH_TIMEOUT": "\"${SEARCH_TIMEOUT:-5}\"", "RESOLUTION_TTL": "\"${RESOLUTION_TTL:-240}\"",
}
lines = contents.splitlines(keepends=True)
anchor = next((index for index, line in enumerate(lines) if line.startswith("      API_KEY:")), None)
if anchor is None:
    raise RuntimeError("Variable API_KEY no encontrada en compose")
for name, value in values.items():
    if not any(line.startswith(f"      {name}:") for line in lines):
        lines.insert(anchor + 1, f"      {name}: {value}\n")
contents = "".join(lines)
compose.write_text(contents)
PY

python3 - <<'PY'
import ast
from pathlib import Path
for name in ("main.py", "multisource.py", "upstream_security.py"):
    ast.parse(Path(name).read_text())
PY
chmod +x run.sh
docker compose config --quiet
docker compose build opentune-resolver
docker compose up -d --no-build --no-deps opentune-resolver
docker compose exec -T opentune-resolver python - <<'PY'
import json
import os
import time
import urllib.request
for attempt in range(10):
    try:
        request = urllib.request.Request("http://127.0.0.1:8787/health", headers={"X-API-Key": os.environ["API_KEY"]})
        with urllib.request.urlopen(request, timeout=3) as response:
            if json.load(response).get("ok"):
                break
    except Exception:
        time.sleep(1)
else:
    raise SystemExit("La comprobación local de salud falló")
PY
trap - ERR
printf 'Actualización aplicada; comprueba HTTPS, Range y proveedores antes de retirar el respaldo: %s\n' "$backup"
