# Operación 01 — Inventario completo de integración

Estado: inventario estático completado. No se modificó código de aplicación ni se ejecutó un APK.
Rama: `GalaxOTxMochyTEST`.
Origen: `realidkroo/Mochy-Music@f8a3593fd0c37edca3da9012c0eb4bc37e907cc7`.
Destino inspeccionado: `SrYorjs/OpenTune-SY@8cd731eade8ba364213f65e7ce3c0912fe1e87dc`, sobre base funcional `8ffbc1acd276cb62a7e86d3bd71adfef895161c8`.

## Cobertura y cómo utilizarla

- Árboles Git completos, sin truncamiento: 2.248 archivos de origen y 1.356 de destino.
- Contenido inspeccionado mediante extracción estática: 1.875 archivos Kotlin/configuración de ambos proyectos y 1.037 XML de recursos. Se leyeron 1.089 archivos Kotlin de producción del origen.
- 527 archivos en el árbol UI original. El índice incluye 400 archivos de pantallas, reproductores, menús, paneles y auxiliares, sin confundir archivos con pantallas únicas.
- 858 recursos/assets originales: 699 rutas nuevas, 98 con contenido diferente al destino y 61 idénticas.
- 167 coincidencias de declaraciones o llamadas de navegación con archivo, línea y contexto; incluyen wrappers y expresiones dinámicas, no 167 pantallas únicas.
- 186 archivos del backend, Liquid Glass y reproductores propios quedan identificados por SHA en la base protegida.

| Documento | Uso |
| --- | --- |
| [archivos.csv](inventario-01/archivos.csv) | Todos los archivos de origen, blob SHA, tamaño, operación, tratamiento, ruta propuesta y colisión |
| [pantallas-y-paneles.md](inventario-01/pantallas-y-paneles.md) | Índice completo de pantallas, estilos, menús y auxiliares |
| [rutas.json](inventario-01/rutas.json) | Declaraciones y expresiones de navegación, incluidas constantes y rutas dinámicas |
| [dependencias.json](inventario-01/dependencias.json) | Imports, referencias internas cualificadas y recursos utilizados por archivo Kotlin |
| [recursos.csv](inventario-01/recursos.csv) | Recursos y assets, símbolos XML, SHA, colisiones y coincidencias de marca |
| [compilacion.json](inventario-01/compilacion.json) | Catálogos completos y declaraciones Gradle de ambos repositorios |
| [base-protegida.csv](inventario-01/base-protegida.csv) | Archivos existentes que deben conservarse o modificarse solo mediante integración revisada |

Las rutas de destino del inventario son propuestas, no archivos ya trasladados. Para código de la app se conserva la jerarquía y se cambia `com/mochyy/mochymusic` por `com/arturo254/opentune`. Las filas marcadas como referencia, proveedor o contrato no autorizan copiar o reemplazar el backend. Una colisión significa comparar contenido; jamás sobrescribir automáticamente.

La extracción usa patrones estáticos, no un compilador Kotlin: no prueba resolución de símbolos, reflexión, generación de código, compatibilidad binaria ni rutas alcanzables en ejecución. Los archivos de pruebas y binarios se inventarían por SHA y tamaño; no se afirma haber ejecutado pruebas o inspeccionado visualmente cada imagen. Las referencias importadas y cualificadas hacen visibles las dependencias que deberán resolverse durante cada operación.

## Estructura original que debe permanecer unida

Prefijos de esta sección: origen `app/src/main/kotlin/com/mochyy/mochymusic/`; destino `app/src/main/kotlin/com/arturo254/opentune/`.

| Conjunto original | Dependencias y conexión necesaria | Operaciones |
| --- | --- | --- |
| MainActivity.kt, ui/component/AppNavigation.kt, ui/screens/Screens.kt, NavigationBuilder.kt | Sustituir montaje Compose y navegación actuales, conservar ciclo Android, enlaces externos y unión al servicio | 04 |
| ui/component/BottomSheet y geometría de cápsulas; ui/player/BottomSheetPlayer.kt | El mini no es una pantalla independiente: usa estado del sheet, carrusel, gestos laterales y transformación de portada | 05 y 11 |
| ui/screens/HomeScreen.kt y componentes Home/HeroCarousel | Adaptar HomeViewModel, datos locales/remotos y acciones conservando todas las variantes y orden | 06 |
| ui/screens/search completo | Adaptar modelos de búsqueda/sugerencias y navegación; enviar resultados a la búsqueda y reproducción existentes | 07 |
| ui/screens/library completo | Adaptar consultas, filtros, fijados, importados y paginación a MusicDatabase/DatabaseDao | 08 y 10 |
| AlbumScreen.kt, ui/screens/artist, playlist y ui/component/detail | Conservar composiciones y transiciones; adaptar identificadores, consultas, listas y menús | 09 |
| ui/player/BottomSheetPlayer.kt, flagship, controles y modelos visuales | Proveer contrato PlayerUiState desde el PlayerConnection existente | 11 |
| LyricsScreen.kt, QueueScreen.kt, lyrics, share y video | Adaptar letras, cola por identidad, vídeo y exportación; trasladar páginas y transiciones originales | 12 |
| ui/player/ipod, bedside, styles | Datos y acciones originales alimentados por la biblioteca y reproducción reales | 13 |
| ui/player/studio completo | Selector, cámara, captura, estado, editores, previews y preferencias forman un conjunto | 14 y 15 |
| ui/screens/settings, setup, update, audiolab, equalizer, recap, gesture; ui/menu y ui/tv | Conservar inventario completo; conectar capacidades adicionales sin botones vacíos | 16 |

### Corrección del orden de integración

En el origen, `BottomSheetPlayer` importa directamente estudio, selector, estilos y acciones. El mini, la expansión y el selector comparten estado. Por tanto:
1. La operación 02 debe preparar dependencias de todo ese conjunto, no solo de la primera pantalla.
2. Las operaciones 03–05 pueden trasladar dependencias por lotes, pero la activación del montaje original exige que su conjunto compile.
3. No reemplazar dependencias pendientes por una escena recreada, tarjetas falsas, callbacks vacíos o un reproductor genérico.
4. La operación 05 no podrá declararse terminada antes de resolver las dependencias requeridas de 11–14. Importar esas dependencias anticipadamente no equivale a cerrar sus pruebas funcionales.
5. El traslado de un archivo original completo puede ser grande: dividir los lotes por archivos/dependencias, no reescribir el archivo para reducir el diff.

El origen tiene cuatro pestañas: Inicio, Buscar, Biblioteca y Ajustes. No conservar como estructura final un paginador antiguo de tres pestañas. Los iconos de Ajustes con marca se incluyen en la limpieza solicitada, manteniendo geometría y comportamiento.

## Catálogo exacto de reproductores

Extraído de `constants/PreferenceKeys.kt`, `PlayerStyle` y `selectorColumns`.

| Columna original | Estilos y variantes | Dependencia |
| --- | --- | --- |
| 1 | FLAGSHIP → CLASSIC | BottomSheetPlayer y flagship |
| 2 | TURNTABLE | Familia flagship, disco y brazo |
| 3 | VIDEO | VideoModeScreen, fuente de vídeo y sincronización |
| 4 | IPOD | Rueda, menús, Cover Flow y biblioteca completa |
| 5 | BEDSIDE_TILES → BEDSIDE | Bedside, reloj, letras y reglas de pantalla bloqueada |
| 6 | POSTER | AlternatePlayerStyles y letras |
| 7 | TIMELINE → LYRICS | AlternatePlayerStyles; Lyrics stage es variante vertical |
| Condicional | MODULE | Solo cuando ActiveSkin aporta un reproductor; no es una tarjeta permanente |

Hay 10 estilos nativos permanentes y 1 condicional. Añadir V1–V17 conserva los diez: 27 estilos permanentes y 28 con MODULE activo. Las 17 implementaciones están en `ui/player/GalaxStudioPlayer/StudioThemeV*.kt`; V8 usa `StudioThemeV8Apple.kt` y V17 `StudioThemeV17VinJax.kt`.

Las V1–V17 se incorporarán como columnas adicionales con el mismo renderizado de tarjeta, gestos y persistencia del selector, manteniendo las columnas nativas. Los nombres serán V1…V17 y no tendrán descripciones. No activar audio al explorar previews. Conservar migraciones originales de preferencias: DEFAULT → FLAGSHIP, SIMPLE/SIMPLE_COMPACT → TIMELINE, LYRICS_ABSTRACT → LYRICS.

## Contratos y adaptadores necesarios

| Contrato del origen | Destino existente verificado | Adaptación necesaria y prueba de aceptación |
| --- | --- | --- |
| PlayerUiState.transportState/trackInfo/progressState | playback/PlayerConnection.kt, ui/player/state/PlayerState.kt | Proyectar estado real a modelos originales, distinguir reproducción efectiva/buffering y pausar relojes invisibles |
| QueueState y QueueItemData.uid | PlayerConnection.queueWindows/currentWindowIndex y MusicService | Identidades estables por entrada, incluidas canciones duplicadas; mover/eliminar/reproducir por UID sin confundir índices ni aleatorio |
| play/pause/seek y acciones del selector | PlayerConnection.player, extensiones y MusicService | Puente de comandos sobre el único reproductor, sin iniciar un servicio de Mochy paralelo |
| Solicitud de estudio y preferencias de estilo | DataStore/rememberPreference y temas V1–V17 existentes | Añadir estado/solicitud de estudio y migración de claves sin sobrescribir selección propia |
| Búsqueda y páginas online | sources/search/UnifiedSearchRepository.kt, UniversalSearchViewModel | search/loadMore, errores parciales, agrupación y traducción a modelos de presentación originales |
| Selección y reproducción de resultados | SourceResolver.kt, OriginalRecordingSelection.kt, SourcePlaybackCoordinator.kt | Conservar selección automática y playSearchResults/playFirstAvailable; no mostrar proveedores o versiones |
| HomeViewModel original | viewmodels/HomeViewModel.kt y fuentes existentes | Comparar campos y secciones, mapear contenido por identidad; no sustituir secciones por listas arbitrarias |
| Entidades, favoritos y biblioteca | db/MusicDatabase.kt, DatabaseDao.kt y entidades existentes | Adaptar consultas y relaciones; conservar IDs/datos; migraciones explícitas si hacen falta |
| Metadata del origen | models/MediaMetadata.kt, core/model | Mapear artista/álbum/carátula/duración y procedencia; mantener campos adicionales fuera de datos visuales cuando corresponda |
| Importación local/SAF | sources/local/LocalMusicSource.kt y biblioteca existente | Importador completo pendiente en esta base: selector Android, URI persistente, metadatos y eliminación de registros sin borrar archivos |
| Descarga/caché | OfflineDownloadCoordinator.kt, DownloadUtil.kt, ExoDownloadService.kt | Usar plan/enqueue y registros existentes; estados originales ligados al progreso real y almacenamiento del dispositivo |
| Letras y ajustes temporales | lyrics/LyricsHelper.kt y almacenamiento actual | Mapear líneas, posiciones, offsets y proveedores disponibles; ampliar capacidad cuando una acción original no tenga soporte |
| Vídeo/Canvas y exportación | módulo canvas existente y reproducción Media3 | No confundir stream de audio con vídeo. Resolver vídeo, permisos, exportación y componentes Media3 faltantes |
| Liquid Glass original + excepción del usuario | core/designsystem/liquid, core/designsystem/glass y ui/component/LiquidGlass* | Conservar variante/ajustes propios; conectar captura de fondo separada y mantener límites/animaciones originales |
| Cast, TV link, módulos, scrobbling, audio lab y crossmix | No existe equivalencia completa demostrada por nombres de archivo | Dependencias funcionales adicionales, no simples cambios de ruta. Implementar contratos o integración original compatible antes de cerrar esas pantallas |

Métodos públicos/callbacks detectados en PlayerConnection de origen sin método homónimo en la clase destino:
`streamFacts`, `requestPlayerStudio`, `testCrossmix`, `setInfiniteQueueEnabled`, `playQueueItem`, `moveQueueItem`, `playQueueItemNext`, `removeQueueItems`, `stopRecommendingCurrent`, `toggleMute`, `setMuted`, `toggleLibrary`, `togglePlayPause`, `play`, `pause`, `seekTo`, `onTracksChanged`, `onMediaMetadataChanged`.
Esta diferencia nominal no significa ausencia total de capacidad: algunas acciones existen mediante Player, extensiones u otros servicios; el adaptador debe resolver cada una.

MusicSourceModule registra YouTube, Audius, Apple Music, Deezer, Spotify, Tidal, Local y Offline. Ser proveedor registrado no demuestra que todos entreguen streams: respetar SourceRegistry y su matriz de capacidades. No reemplazar fuentes propias por los módulos de proveedores del origen.

## Compilación y dependencias

| Dependencia | Origen fijado | Destino fijado |
| --- | --- | --- |
| AGP | 9.0.0 | 9.3.0 |
| kotlin | "2.3.10" | "2.3.20" |
| compose | "1.10.2" | "1.11.0-rc01" |
| material3 | "1.5.0-alpha18" | "1.5.0-alpha18" |
| media3 | "1.7.1" | "1.10.0" |
| coil | "3.3.0" | "3.4.0" |
| hilt | "2.59.1" | "2.59.2" |
| ksp | "2.3.5" | "2.3.6" |
| ktor | "3.4.0" | "3.4.2" |
| room | "2.8.4" | "2.8.4" |
| datastore | "1.2.0" | "1.2.1" |
| Java | 21 | 21 |
| compileSdk / minSdk / targetSdk | 36 / 24 / 36 | 37 / 26 / 36 |

No realizar downgrade global. Resolver las incompatibilidades de las APIs usadas por el original sobre el proyecto destino y verificar dependencias transitivas.
- Origen declara variantes foss/fdroid/gms además de ABI; destino usa ABI. Cast y manifest de GMS requieren una decisión de integración técnica, no copiar flavors y applicationId.
- Identidad y autoridades del destino, firma, permisos y datos existentes deben conservarse.
- Inventario de módulos originales en settings.gradle: providers de letras, Canvas, vídeo/artista y módulos api/bridge/applemusic/monochrome/jiosaavn. Mapear proveedores coincidentes a los existentes, sin importar otro motor musical.
- Revisar dependencias visuales adicionales: MaterialKolor, smooth-corner, Lottie, Coil SVG, adaptive layout/navigation y uCrop.
- Revisar dependencias de funciones adicionales: Media3 DASH/transformer/effect, FFmpeg, Cast, protobuf y WebKit. El binario FFmpeg del origen está vinculado a su versión de Media3; no trasladarlo ciegamente al destino 1.10.0.
- Mantener configuración Compose de estabilidad, recursos raw, shaders y assets que consumen los originales; ver catálogo por archivo.
- Las versiones son las declaradas en las revisiones, no una recomendación de actualizar paquetes.

## Recursos, marcas y conservación

Los recursos no se reemplazan por iconos o fuentes “parecidos”. Se registran SHA incluso para binarios. Revisar las 98 colisiones por ruta y, durante integración, nombres de símbolos de resources XML entre archivos y qualifiers antes de fusionar. Los manifiestos y recursos de identidad merecen tratamiento distinto de los recursos visuales.

Las coincidencias Mochy/GalaxOT del CSV son búsquedas textuales: incluyen nombres internos y referencias que no necesariamente son visibles. Revisar texto renderizado, logos, splash, about, ajustes, pantallas de desarrolladores y traducciones. Conservar licencias y atribuciones obligatorias en sus archivos legales; no borrar paquetes de origen de la documentación de procedencia.

## Cierre y siguiente operación

Inventario cerrado por cobertura de archivos, configuración, rutas/expresiones, recursos, catálogo de estilos y contratos de integración. Las capacidades sin equivalencia están documentadas como trabajo de implementación, no como funcionalidades existentes.
No se ha certificado compilación, fidelidad visual ni rendimiento de la futura app.
Siguiente: operación 02, recursos y dependencias originales, atendiendo primero el conjunto necesario para el montaje completo.
