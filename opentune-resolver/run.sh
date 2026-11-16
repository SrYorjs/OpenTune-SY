#!/bin/sh
set -eu
exec uvicorn main:app --host 0.0.0.0 --port "${PORT:-8787}" --workers 1 --no-access-log
