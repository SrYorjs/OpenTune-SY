# GalaxOS — entrega y verificación del rediseño

Rama exclusiva: `opentune-rebuild-v1`. Base: `ac1233f0637c718bcb89f146667ae788f8e87256`.

Esta entrega reúne el nuevo sistema visual, la reconstrucción de las vistas principales y de los paneles indicados abajo, y la migración de las rutas secundarias a componentes comunes. No equivale a eliminar y reescribir literalmente cada pantalla: los ajustes especializados, algunos flujos de selección y las interfaces web de autenticación conservan estructura funcional existente. No se ha realizado validación visual en un teléfono ni se han medido 60 FPS.

## Sistema visual

Fondos oscuros `#060912`, superficies `#0D1220`–`#26334B`, acentos ambientales azules y violetas, margen de página de 24 dp, portadas de radio 20 dp y paneles de 28 dp. Se conservan las fuentes y el acento principal configurados por el usuario. El tema se aplica dentro del NavHost, fuera de la barra inferior. Los controles del reproductor conservan su renderizador Liquid Glass y sus variantes.

## Lista de verificación

| Requisito | Estado y evidencia |
| --- | --- |
| Trabajar solo en la rama indicada | Cumplido; todos los commits se publican en `opentune-rebuild-v1`. |
| Plan anterior al código y trabajo por etapas | Cumplido en la conversación: inventario, sistema/Inicio/reproductor, paneles y validación. |
| Reconstruir desde cero absolutamente todas las pantallas | Cumplimiento parcial del requisito literal. Inicio, catálogo, cuenta, AOD y resumen anual tienen nuevas composiciones; biblioteca, álbumes, artistas y listas tienen nuevas cabeceras, tarjetas y secciones. Ajustes especializados, algunos flujos de selección y autenticación conservan estructura funcional. |
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
| Buscar | Se mantienen el campo que abre teclado y la búsqueda concurrente con resultados parciales. Cabecera, géneros y filas de resultados reconstruidos; procedencia, calidad, versión de catálogo y descarga explícitas. Pendiente validación de teclado y proveedores en teléfono. |
| Letras | Pantalla reconstruida con encabezado de canción, disposición adaptable, controles de cristal y reloj de progreso aislado. Conserva Lyrics/LyricsV2, menú, sincronización y volumen. La carga de letras se cancela al cambiar de canción. |
| Información de canción | Pantalla reconstruida con portada, detalles copiables, estadísticas adaptables y estados de carga/reintento; se evita el indicador perpetuo cuando falla la petición. |
| Biblioteca, listas, cola y otras secundarias | Nuevas cabeceras reales para ocho rutas de álbum/artista/listas; biblioteca con mosaico y accesos; reordenación por claves estables; cabecera de cola reconstruida; cuenta, descubrimiento, charts y catálogo con carga/error/reintento; AOD de cinco variantes y ocho páginas del resumen anual reconstruidos. Historial y subpáginas de artista adoptan nueva cabecera y componentes compartidos. |
| Encontrar y reducir costes del reproductor | Eliminados `TextureView.getBitmap` síncrono y blur de 64 dp a pantalla completa. PixelCopy asíncrono, imagen acotada a 192×384 y estado del fotograma aislado del árbol de controles. |
| Imágenes, listas y hilo principal | Paleta del hero en IO/Default a 96 px; imágenes ajustadas al contenedor; Settings usa carga diferida; se corrige la captura obsoleta de la continuación en Inicio. Se eliminan fondos ligados a cada píxel de scroll en ocho rutas; se acotan imágenes AOD/resumen anual y se detienen relojes al salir del estado STARTED. Se conserva selección de biblioteca entre recomposiciones y se reordena por identidad en lugar de offsets de cabecera. Pendiente perfilado integral en dispositivo. |
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

## Actualizar Android Studio

Ejecutar desde la raíz del clon local. Si el primer comando muestra cambios propios, guardarlos en un commit o stash antes de continuar; no sobrescribirlos.

```sh
git status --short
git fetch origin
git switch opentune-rebuild-v1
git pull --ff-only
git log -1 --oneline
```

Después, sincronizar Gradle con JDK 21 y ejecutar la variante `universalDebug`.

## Validación que requiere dispositivo

En el Samsung S24 FE: abrir/cerrar el reproductor con Canvas activo; probar play/pause, anterior/siguiente en cada variante de cristal; desplazar Inicio/Biblioteca/cola y cambiar pestaña mediante gesto; escribir y borrar consultas rápidas; cambiar proveedor; probar selección/reordenación, descargas y reproducción offline; abrir letras, ecualizador, temporizador, AOD y exportar el resumen anual. Medir frame timing con Android Studio Profiler/Perfetto en una compilación de rendimiento: presupuesto de 16,67 ms para 60 Hz. No se marca este objetivo cumplido sin las trazas.

## Archivos completos modificados o creados

Cada enlace abre el archivo completo. Proyecto: https://github.com/SrYorjs/OpenTune-SY/archive/refs/heads/opentune-rebuild-v1.zip

- [`app/src/main/kotlin/com/arturo254/opentune/MainActivityScaffold.kt`](../../app/src/main/kotlin/com/arturo254/opentune/MainActivityScaffold.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/core/designsystem/theme/GalaxScreenStyle.kt`](../../app/src/main/kotlin/com/arturo254/opentune/core/designsystem/theme/GalaxScreenStyle.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/BottomSheetMenu.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/BottomSheetMenu.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/Items.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/Items.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/Library.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/Library.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/LiquidGlassControls.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/LiquidGlassControls.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/NewMenuComponents.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/NewMenuComponents.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/Preference.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/Preference.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/component/SpotifyLibraryItems.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/component/SpotifyLibraryItems.kt)
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
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/AccountScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/AccountScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/AlbumScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/AlbumScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/AlwaysOnDisplayScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/AlwaysOnDisplayScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/BrowseScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/BrowseScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/ChartsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/ChartsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/DownloadQueueScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/DownloadQueueScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/ExploreScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/ExploreScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxFeaturedTrack.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxFeaturedTrack.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxListeningDeck.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxListeningDeck.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxMainPages.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxMainPages.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxMediaHeader.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxMediaHeader.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxRemoteShelf.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxRemoteShelf.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxScaffold.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxScaffold.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxTopAppBar.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxTopAppBar.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/HistoryScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/HistoryScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/HomeScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/HomeScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/HomeScreenComponents.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/HomeScreenComponents.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/LoginScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/LoginScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/MoodAndGenresScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/MoodAndGenresScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/NewReleaseScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/NewReleaseScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/StatsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/StatsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/YearInMusicScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/YearInMusicScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/YouTubeBrowseScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/YouTubeBrowseScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/artist/ArtistAlbumsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/artist/ArtistAlbumsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/artist/ArtistItemsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/artist/ArtistItemsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/artist/ArtistScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/artist/ArtistScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/artist/ArtistSongsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/artist/ArtistSongsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryAlbumsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryAlbumsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryArtistsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryArtistsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryMixScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryMixScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryOrdering.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryOrdering.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryPlaylistsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryPlaylistsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibraryScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibrarySongsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibrarySongsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibrarySpotifyPlaylistsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/library/LibrarySpotifyPlaylistsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/musicrecognition/MusicRecognitionScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/musicrecognition/MusicRecognitionScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/AutoPlaylistScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/AutoPlaylistScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/CachePlaylistScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/CachePlaylistScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/LocalPlaylistScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/LocalPlaylistScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/OnlinePlaylistScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/OnlinePlaylistScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/SpotifyPlaylistScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/SpotifyPlaylistScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/TopPlaylistScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/playlist/TopPlaylistScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/search/UniversalSearchScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/search/UniversalSearchScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AboutScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AboutScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AccountSettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AccountSettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AlwaysOnDisplaySettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AlwaysOnDisplaySettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AndroidAutoSettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AndroidAutoSettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AppearanceSettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/AppearanceSettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/BackupAndRestore.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/BackupAndRestore.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/ChangelogScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/ChangelogScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/ContentSettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/ContentSettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/CustomizeBackground.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/CustomizeBackground.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/DebugSettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/DebugSettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/DiscordExperimental.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/DiscordExperimental.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/DiscordLoginScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/DiscordLoginScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/DiscordSettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/DiscordSettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/GalaxLiquidSettingsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/GalaxLiquidSettingsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/IntegrationScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/IntegrationScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/LastFMSettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/LastFMSettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/MusicTogetherScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/MusicTogetherScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/PalettePickerScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/PalettePickerScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/PlayerSettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/PlayerSettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/PoTokenScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/PoTokenScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/PrivacySettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/PrivacySettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/SettingsLayout.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/SettingsLayout.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/SettingsScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/SettingsScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/StorageSettings.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/StorageSettings.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/ThemeCreatorScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/ThemeCreatorScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/UpdateScreen.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/settings/UpdateScreen.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/ui/utils/ShowMediaInfo.kt`](../../app/src/main/kotlin/com/arturo254/opentune/ui/utils/ShowMediaInfo.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/viewmodels/AccountViewModel.kt`](../../app/src/main/kotlin/com/arturo254/opentune/viewmodels/AccountViewModel.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/viewmodels/BrowseViewModel.kt`](../../app/src/main/kotlin/com/arturo254/opentune/viewmodels/BrowseViewModel.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/viewmodels/ChartsViewModel.kt`](../../app/src/main/kotlin/com/arturo254/opentune/viewmodels/ChartsViewModel.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/viewmodels/ExploreViewModel.kt`](../../app/src/main/kotlin/com/arturo254/opentune/viewmodels/ExploreViewModel.kt)
- [`app/src/main/kotlin/com/arturo254/opentune/viewmodels/YouTubeBrowseViewModel.kt`](../../app/src/main/kotlin/com/arturo254/opentune/viewmodels/YouTubeBrowseViewModel.kt)
- [`app/src/main/res/values-es/galax_media.xml`](../../app/src/main/res/values-es/galax_media.xml)
- [`app/src/main/res/values/galax_media.xml`](../../app/src/main/res/values/galax_media.xml)
- [`app/src/test/kotlin/com/arturo254/opentune/ui/player/visual/PlayerImmersiveGlassIntegrationTest.kt`](../../app/src/test/kotlin/com/arturo254/opentune/ui/player/visual/PlayerImmersiveGlassIntegrationTest.kt)
- [`app/src/test/kotlin/com/arturo254/opentune/ui/screens/library/LibraryOrderingTest.kt`](../../app/src/test/kotlin/com/arturo254/opentune/ui/screens/library/LibraryOrderingTest.kt)
