# Resolver v2

Base: ZIP `galaxot_vps_resolver_v2.zip` suministrado por el propietario. Se incorpora el destino Audius verificado `validator.eeba4a6ca56a0d87af802270217c2a51.r2.cloudflarestorage.com`. No se dispone de una copia completa de cambios posteriores del servidor desplegado.

## Configuración y despliegue posterior

`docker compose up -d --build` conserva `/resolve/{video_id}` y `/audio/{video_id}` y añade `/v2/search`, `/v2/providers`, `/v2/resolve/{provider}/{item_id}` y `/v2/stream/{provider}/{item_id}`. Requiere `API_KEY`; Audius admite `AUDIUS_API_KEY`. `PEERTUBE_INSTANCE` usa por defecto `https://video.blender.org`; `AUDIUS_R2_HOSTS` permite únicamente nombres R2 exactos adicionales. Conservar el `.env`, certificados y configuración HTTPS existentes; no ejecutar el instalador sobre una instalación operativa. No se ha desplegado esta revisión ni auditado la configuración real de Caddy, Docker o tareas programadas del VPS.

Los scripts de instalación y actualización son herramientas manuales, no se ejecutaron durante esta integración. El arranque ya no instala paquetes ni necesita reinicios diarios: las dependencias están fijadas en `requirements.txt` y se actualizan reconstruyendo la imagen de manera controlada. Caddy debe conservar TLS y transmitir sin caché de cuerpos; la plantilla usa `flush_interval -1`.

## Proveedores y rutas Android activas

YouTube y YouTube Music comparten identidad de catálogo. La búsqueda combina InnerTube con v2; la resolución conserva la ruta InnerTube y el video F5 (clientes, firmas, visitorData y DASH) antes del fallback progresivo v2. Audius usa búsqueda/resolución v2 y conserva su gateway alternativo. PeerTube tiene búsqueda y reproducción directa de MP4 públicos desde la instancia configurada; no depende de que el VPS actual habilite PeerTube. La navegación original consume estos resultados mediante UnifiedSearchRepository y SourcePlaybackCoordinator. No hay selector técnico de proveedores.

El APK local incorpora `OWN_RESOLVER_URL`, `OWN_RESOLVER_KEY` y `AUDIUS_API_KEY` desde `local.properties`; sus valores no se publican. `PEERTUBE_URL` es opcional y usa la instancia anterior. Las compilaciones de CI necesitan suministrar sus propios secretos.

## Caché y seguridad

El servidor guarda solo metadatos y URLs con límites y caducidad en RAM. yt-dlp no descarga medios ni guarda caché en disco; no hay transcodificación. Contenedor de solo lectura, temporales en tmpfs y transmisión por fragmentos de 64 KiB. Las redirecciones HTTPS se limitan a cinco y a destinos permitidos; DNS público validado y conexión fijada a esa IP. La clave del resolver no se transmite a proveedores externos. Se preservan Range, Content-Range, Content-Length y 200/206 reales, sin fabricar respuestas parciales.

Android reutiliza los SimpleCache existentes de reproducción y descargas: `filesDir/exoplayer` con expulsión LRU y límite configurado, y `filesDir/download` para descargas explícitas. Audio y video usan claves estables por contenido/variante. Los archivos de video locales no vuelven a copiarse a caché. La precarga solo toma hasta 256 KiB del siguiente elemento ya resuelto, con red no medida y condiciones de batería adecuadas; se cancela al cambiar de pista. No descarga la cola.

## Validación local y límites

40 pruebas Python cubren proveedores, autenticación, redirecciones, SSRF, respuestas parciales, errores, cancelación y actualización desde Dockerfiles anteriores. Ejecutar `python -m pytest opentune-resolver -q` con las dependencias del servidor y pytest. Android: 616 pruebas de app y 5 de InnerTube, APK universal debug y lint vital correctos; lint completo conserva los mismos 113 errores anteriores, 0 nuevos y 0 Fatal. No se modificaron reglas ni contratos visuales.

Las lecturas HTTP reales comprobaron búsquedas de YouTube/Audius y PeerTube; YouTube audio/video y PeerTube devolvieron 206 y 1024 bytes. La muestra Audius devolvió 200 y 1024 bytes con firma MP3 (el propietario había comprobado otra muestra 206); su MIME genérico se corrige en el proxy de esta revisión. Primer byte observado: YouTube audio 797 ms, video 673 ms, PeerTube 580 ms, Audius 4823 ms. Búsqueda inicial/repetida del servidor existente: Audius 1192/467 ms, YouTube 1223/532 ms. Son mediciones previas al despliegue, no una comparación antes/después de estos cambios ni tiempo hasta audio audible. Faltan reproducción, recuperación, navegación y comportamiento offline reales en dispositivo, y comprobación del backend actualizado tras un despliegue autorizado.
