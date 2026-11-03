# GalaxOT: diagnóstico de Piped, InnerTube y NewPipeExtractor

## Alcance

Este experimento **no es un proveedor nuevo en producción**, no cambia la búsqueda de
GalaxOT, no cambia la cola ni Media3 y no ofrece nuevas descargas. Su objetivo es
comparar qué tan accesible es la **información** de videos públicos desde tres rutas.

- **InnerTube:** usa `YouTube.player(..., setLogin = false, authState = PlaybackAuthState.EMPTY)`.
  No utiliza cookies, visitorData, PO tokens ni la sesión de GalaxOT.
- **NewPipeExtractor:** utiliza la dependencia que ya tiene el proyecto
  (`v0.26.3`) y `StreamInfo.getInfo()`.
- **Piped:** llama a `GET /streams/{videoId}` y `GET /search?q=...&filter=...`
  sobre la instancia HTTPS indicada. No envía cabeceras de autorización.

Piped utiliza NewPipeExtractor en su backend, por lo que los resultados de
Piped y NewPipeExtractor **no son fuentes completamente independientes**.

## Cómo ejecutarlo

1. Actualiza la rama `opentune-rebuild-v1` y compila la APK de depuración.
2. En la aplicación entra en **Ajustes → Experimentales → Prueba de motores musicales**.
3. Deja la instancia de ejemplo (`https://pipedapi.kavin.rocks`) o cambia a otra
   URL API HTTPS publicada en la lista de instancias oficial:
   https://github.com/TeamPiped/Piped/wiki/Instances
4. Introduce uno o dos IDs de videos públicos (por defecto `jNQXAC9IVRw` y
   `aqz-KE-bpKQ`) y un término de búsqueda (por defecto `lofi`).
5. Pulsa **Ejecutar diagnóstico**.

Los resultados aparecen en pantalla: disponibilidad de metadatos, número y
descripción de formatos anunciados, errores HTTP de la API cuando procedan y
número de resultados de los filtros musicales de Piped.

### Interpretación

- `OK` significa que la consulta devolvió datos y formatos; **no** significa
  que Media3 pueda reproducir el contenido.
- `SIN_FORMATOS_AUDIO` significa que hay metadatos, pero no formatos
  de audio anunciados en esa respuesta.
- `LOGIN_REQUIRED`, `UNPLAYABLE` o similar indican restricciones expuestas
  por la respuesta pública de InnerTube.
- `HTTP_403`, `HTTP_429`, `HTTP_5xx`, `TIMEOUT` o
  `ERROR_<tipo>` describen fallos sin exponer mensajes de servidor.
- `RESPUESTA_INESPERADA` indica que la API no devolvió el formato esperado.
- El experimento no realiza una prueba HTTP de las URLs de audio, por lo
  que no hay afirmaciones de acceso al CDN.

### Privacidad, licencias y límites

El informe **no conserva ni imprime** URLs firmadas de audio, cookies,
tokens, claves de API, cabeceras, ni textos completos de excepciones.
Solo se muestra mientras la pantalla permanece en memoria.

Al tratarse de interfaces no oficiales, la disponibilidad de resultados
puede cambiar y algunas instancias Piped pueden estar fuera de línea.
Ninguna de ellas concede licencia o autorización para extraer audio,
almacenar música offline o incumplir las condiciones de YouTube.

## Próximo criterio de decisión

Solo evaluar una integración adicional de búsqueda o metadatos si:
1. Dos o más videos públicos devuelven datos consistentes.
2. Los cuatro filtros musicales responden correctamente.
3. Los errores de red se representan sin romper el reproductor.
4. No hay dependencia obligatoria de una instancia pública inestable.
5. Se comprueba la compatibilidad técnica y las condiciones de uso.

**No convertir este diagnóstico en una ruta de reproducción o descargas
sin una revisión separada.**

API de Piped: https://docs.piped.video/docs/api-documentation/
Javadoc NewPipeExtractor: https://teamnewpipe.github.io/NewPipeExtractor/javadoc/
