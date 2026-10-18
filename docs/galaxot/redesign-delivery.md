# GalaxOS — entrega y verificación del rediseño

Rama exclusiva: `opentune-rebuild-v1`. Base: `ac1233f0637c718bcb89f146667ae788f8e87256`.

Esta entrega es una etapa del rediseño, no una reconstrucción terminada de todas las pantallas. La migración del tema y de los componentes compartidos alcanza rutas secundarias, pero no equivale a rehacer su estructura individual. No se ha realizado validación visual en un teléfono ni se han medido 60 FPS.

## Sistema visual

Fondos oscuros `#060912`, superficies `#0D1220`–`#26334B`, acentos ambientales azules y violetas, margen de página de 24 dp, portadas de radio 20 dp y paneles de 28 dp. Se conservan las fuentes y el acento principal configurados por el usuario. El tema se aplica dentro del NavHost, fuera de la barra inferior. Los controles del reproductor conservan su renderizador Liquid Glass y sus variantes.

## Lista de verificación

| Requisito | Estado y evidencia |
| --- | --- |
| Trabajar solo en la rama indicada | Cumplido; todos los commits se publican en `opentune-rebuild-v1`. |
| Plan anterior al código y trabajo por etapas | Cumplido en la conversación: inventario, sistema/Inicio/reproductor, paneles y validación. |
| Reconstruir desde cero absolutamente todas las pantallas | Pendiente de completar. Se conserva estructura previa en distintas rutas de álbumes, artistas, listas, biblioteca y ajustes especializados. |
| Sistema de diseño espacial consistente | Implementado en `GalaxScreenStyle` y aplicado a los destinos de navegación y componentes compartidos. Falta revisión visual integral en dispositivo. |
| Portadas cuadradas, redondeadas y sin contenedor con borde | Implementado en el carrusel de Inicio, tarjetas globales y `GridItem`; los artistas conservan sus miniaturas circulares. |
| Miniaturas de vídeo integradas | Las tarjetas de canciones usan formato cuadrado y `ContentScale.Crop`; falta comprobar recortes de todos los proveedores. |
| Tarjeta principal real y degradado de portada | Implementado con la primera recomendación disponible, extracción de paleta fuera del hilo principal y reproducción real. No se inventa una canción si no hay datos. |
| Aleatorio principal y Music Recognition secundario activos | Botón principal de reproducción aleatoria y secundario tonal. Aleatorio se deshabilita cuando no hay canciones reproducibles; no se simula una acción disponible. |
| Chips ligeros de cristal | Se conserva el componente de cristal configurado; los chips de ecualizador no tienen borde y el temporizador reutiliza el componente compartido. |
| Encabezado limpio y transición inferior | Cabeceras compactas y degradado inferior de 88 dp que no intercepta gestos. |
| Barra inferior exacta | `FloatingNavigationToolbar`, `MainActivityChrome` y `MainActivityNavigationState` no tienen cambios respecto a la base. El tema de destinos no envuelve la barra. |
| Menú de tres puntos | Filas compartidas reconstruidas, interacción spring y panel con fondo galáctico. Se conservan las acciones existentes. |
| Ecualizador | Fondo, secciones y gráfica real de ganancias actualizados; se conserva la edición de bandas, perfiles, importación y efectos. No se ha reescrito todo su flujo. |
| Ajustes | Estructura adaptable reconstruida con LazyColumn, reutilización por tipo y sin entradas escalonadas. Preferencias compartidas reconstruidas. Pantallas especializadas conservan parte de su estructura. |
| Temporizador | Panel reconstruido con accesos 15/30/60/90 min, control de 5–120 min y fin de canción. Reutilizado desde cola y reproductor. |
| Music Recognition | Nuevo control orbital, animación leída en la fase de dibujo, movimiento reducido y fondo unificado. Se conservan permisos, grabación, resultados y errores. |
| Buscar | Se mantienen el campo que abre teclado y la búsqueda concurrente con resultados parciales. Se rediseñan cabecera y géneros. Pendiente validación de teclado y proveedores en teléfono. |
| Letras | Pantalla reconstruida con encabezado de canción, disposición adaptable, controles de cristal y reloj de progreso aislado. Conserva Lyrics/LyricsV2, menú, sincronización y volumen. La carga de letras se cancela al cambiar de canción. |
| Información de canción | Pantalla reconstruida con portada, detalles copiables, estadísticas adaptables y estados de carga/reintento; se evita el indicador perpetuo cuando falla la petición. |
| Biblioteca, listas, cola y otras secundarias | Migración visual compartida parcial. Biblioteca adopta transiciones con movimiento reducido y portadas Spotify sin contenedor. No se presentan todas como pantallas enteramente reconstruidas. |
| Encontrar y reducir costes del reproductor | Eliminados `TextureView.getBitmap` síncrono y blur de 64 dp a pantalla completa. PixelCopy asíncrono, imagen acotada a 192×384 y estado del fotograma aislado del árbol de controles. |
| Imágenes, listas y hilo principal | Paleta del hero en IO/Default a 96 px; imágenes ajustadas al contenedor; Settings usa carga diferida; se corrige la captura obsoleta de la continuación en Inicio. Auditoría exhaustiva de toda la app pendiente. |
| 60 FPS estables | No verificado. Requiere trazas de frame timing en dispositivo real, incluyendo Canvas, cambios de canción, scroll y las variantes de cristal. |
| Animaciones spring y gestos | Se conservan expansión y gestos existentes; se usan las políticas spring compartidas en tarjetas/menús. No se afirma haber reconstruido todas las transiciones. |
| Código final sin comentarios | Los archivos Kotlin modificados se entregan sin comentarios; las atribuciones originales se conservan en `redesign-source-notices.md`. |
| Proyecto listo para Android Studio | Código completo en la rama; comprobar el resultado del Safety Gate para el commit exacto. No se incluyen snippets sustitutivos. |

## Compilación y validación

Abrir la raíz del repositorio con Android Studio y JDK 21. El workflow `Rebuild Safety Gate` ejecuta:

```sh
./gradlew testUniversalDebugUnitTest --no-daemon --stacktrace
./gradlew lintUniversalDebug --no-daemon --stacktrace
./gradlew assembleUniversalDebug --no-daemon --stacktrace
```

El APK se publica como artefacto `OpenTune-rebuild-universal-debug` en la ejecución satisfactoria del workflow. El entorno de edición no dispone de SDK/emulador Android; compilación y pruebas se ejecutan en GitHub Actions. Las pruebas de contratos del código no demuestran FPS ni sustituyen una prueba visual.

PixelCopy permite solicitar copias asíncronas de Surface: https://developer.android.com/reference/android/view/PixelCopy. La copia de fondo es acotada y solicita una muestra cada 67 ms después de terminar la anterior; no modifica la cadencia del vídeo visible. Cuando el dispositivo rechaza una copia, se conserva el último fondo válido o la portada. La compatibilidad de esta ruta con los dispositivos afectados necesita una prueba real.

## Archivos completos modificados o creados

Los enlaces abren cada archivo completo de esta rama. Para obtener el proyecto completo: https://github.com/SrYorjs/OpenTune-SY/archive/refs/heads/opentune-rebuild-v1.zip

- [`app/src/main/kotlin/com/arturo254/opentune/MainActivityScaffold.kt`](../../app/src/main/kotlin/com/arturo254/opentune/MainActivityScaffold.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/core/designsystem/theme/GalaxScreenStyle.kt`](../../app/src/main/kotlin/com/arturo254/opentune/core/designsystem/theme/GalaxScreenStyle.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/BottomSheetMenu.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/BottomSheetMenu.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/Items.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/Items.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/LiquidGlassControls.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/LiquidGlassControls.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/NewMenuComponents.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/NewMenuComponents.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/Preference.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/Preference.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/menu/PlayerMenu.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/menu/PlayerMenu.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/player/CanvasArtworkPlayer.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/player/CanvasArtworkPlayer.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/player/CanvasFrameCopy.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/player/CanvasFrameCopy.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/player/LyricsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/player/LyricsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/player/Player.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/player/Player.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/player/Queue.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/player/Queue.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/player/QueueComponents.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/player/QueueComponents.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/player/panels/GalaxEqualizerCurve.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/player/panels/GalaxEqualizerCurve.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/player/v8/V8PlayerScene.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/player/v8/V8PlayerScene.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/player/visual/PlayerArtworkAtmosphere.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/player/visual/PlayerArtworkAtmosphere.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxFeaturedTrack.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxFeaturedTrack.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxListeningDeck.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxListeningDeck.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxMainPages.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxMainPages.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/HomeScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/HomeScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/HomeScreenComponents.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/HomeScreenComponents.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/MoodAndGenresScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/MoodAndGenresScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryMixScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryMixScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/musicrecognition/MusicRecognitionScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/musicrecognition/MusicRecognitionScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/SettingsLayout.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/SettingsLayout.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/utils/ShowMediaInfo.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/utils/ShowMediaInfo.kt)
- [`app/src/test/kotlin/com/arturo254/opentune/ui/player/visual/PlayerImmersiveGlassIntegrationTest.kt`](../../app/src/test/kotlin/com/arturo254/opentune/ui/player/visual/PlayerImmersiveGlassIntegrationTest.kt)
