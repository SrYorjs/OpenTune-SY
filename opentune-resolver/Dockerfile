FROM python:3.12-slim

# Deno: yt-dlp lo usa para resolver los desafíos JS de YouTube
COPY --from=denoland/deno:bin /deno /usr/local/bin/deno

RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY main.py run.sh ./
RUN chmod +x run.sh

EXPOSE 8787
# run.sh actualiza yt-dlp en cada arranque (YouTube cambia a menudo)
CMD ["./run.sh"]
