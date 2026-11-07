#!/bin/sh
pip install --no-cache-dir -U "yt-dlp[default]" >/dev/null 2>&1 || echo "aviso: no se pudo actualizar yt-dlp"
exec uvicorn main:app --host 0.0.0.0 --port "${PORT:-8787}"
