# OpenTune Resolver

Servidor personal (FastAPI + yt-dlp) que resuelve y retransmite el audio de YouTube Music
para tu app. Pensado para uso privado entre dos personas.

## 1. Arrancarlo (en tu PC, Raspberry o VPS)

```bash
cd opentune-resolver
echo "API_KEY=$(openssl rand -hex 24)" > .env     # guarda esta clave, la usarás en la app
docker compose up -d --build
```

Sin Docker: `pip install -r requirements.txt`, instala **deno** (yt-dlp lo necesita), y
`API_KEY=tuclave uvicorn main:app --host 0.0.0.0 --port 8787`.

## 2. Probarlo

```bash
curl -H "X-API-Key: TU_CLAVE" http://localhost:8787/health
curl -H "X-API-Key: TU_CLAVE" "http://localhost:8787/resolve/dQw4w9WgXcQ?quality=high"
```

Si `/resolve` devuelve JSON con `bitrate`, `codecs` y `audioPath`, el servidor funciona.
Si da error 502, el mensaje dice qué falló en yt-dlp (pégamelo y lo vemos).

## 3. Hacerlo accesible desde el teléfono

- **Misma wifi:** `http://IP-de-tu-PC:8787`
- **Fuera de casa (recomendado):** instala **Tailscale** en el servidor y en los móviles
  y usa `http://100.x.x.x:8787`. No abres puertos ni expones nada a internet.
- Alternativa: Cloudflare Tunnel (da una URL https).

## 4. Configurar la app

Ajustes → Contenido → **Servidor propio: URL** y **Servidor propio: clave**.
Con eso, cada canción se pide primero al servidor; si no responde, la app usa su método anterior.

## Mantenimiento

- `run.sh` actualiza yt-dlp en cada arranque. Programa un reinicio diario:
  `0 4 * * * docker restart opentune-resolver`
- Si un día "deja de sonar": mira `docker logs opentune-resolver`; casi siempre se arregla
  reiniciando (actualiza yt-dlp).
- Cuenta Premium (≈256 kbps): exporta cookies en formato Netscape, móntalas en el contenedor y
  define `COOKIES_FILE=/data/cookies.txt`. Usa una cuenta secundaria si te preocupa.
- Mantén el servidor en una IP residencial; las IPs de datacenter suelen toparse con bloqueos.
