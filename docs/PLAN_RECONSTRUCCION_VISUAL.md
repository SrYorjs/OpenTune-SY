# Plan de reconstrucción visual integral

## Decisión de arquitectura

**Objetivo:** utilizar la interfaz original de [Mochy Music](https://github.com/realidkroo/Mochy-Music) como referencia de código y experiencia completa, e insertar debajo los servicios musicales y los datos existentes de [OpenTune-SY](https://github.com/SrYorjs/OpenTune-SY). No decorar las pantallas antiguas con tarjetas, colores o animaciones parecidas.

**Rama exclusiva:** `rebuild/trasplante-visual-integral`.

**Base limpia:** `8ffbc1acd276cb62a7e86d3bd71adfef895161c8`, anterior a los 66 commits de la migración visual descartada. `main` y `opentune-rebuild-v1` se conservan como respaldo y no se fusionarán automáticamente. La nueva rama no contiene los 54 archivos alterados por la integración desechada.

**Fuente de interfaz auditada:** ZIP suministrado `Mochy-Music-main(1).zip`, con 2.760 entradas, junto con el repositorio original (revisión GitHub `580b50667e3027b2314f8d9d3104597fc8b604e1`). Para cambios posteriores, comparar primero la fuente. Conservar avisos de derechos y obligaciones GPL-3.0 en los archivos legales y documentación, sin marcas o menciones a ninguno de los dos proyectos dentro de la interfaz visible.

## Reglas de implementación

1. **La interfaz original manda:** tomar sus composables reales, recursos, orden de pantallas, movimientos, navegación, gestos, transiciones, hojas, mini reproductor, barra inferior y paneles. No recrear ni simular componentes para que «se parezcan».
2. **La lógica musical existente manda:** conservar Media3, MusicService, fuentes/proveedores, VPS, selección automática de fuente, cola progresiva, búsqueda y descargas locales; sustituir únicamente las capas visuales y crear adaptadores de datos/acciones donde sea indispensable.
3. **Sin marcas en pantalla:** retirar encabezados, descripciones, créditos promocionales o rótulos visibles que contengan «Mochy» o «GalaxOT». Los avisos de licencia, copyright y procedencia del código no se borran.
4. **Reproductores:** conservar todas las variantes nativas de la fuente e insertar V1–V17 en el selector original `PlayerSelectorOverlay` con sus gestos, transiciones, disposición y tarjetas. Quitar textos explicativos de cada reproductor; no sustituir el selector original por un diálogo genérico.
5. **Cambios pequeños y trazables:** una operación concreta por lote, commits en español, preferentemente de 1 a 5 archivos por commit; no crear grandes parches de borrado ni cambios masivos de miles de líneas por conveniencia. El tamaño literal de un archivo de origen puede obligar a un trasplante mayor: registrar tamaño y justificarlo antes de integrarlo.
6. **Código sin comentarios nuevos.** Resolver importaciones, nombres de paquete, referencias de recursos y compatibilidad Compose/Gradle con adaptadores mínimos; no introducir dobles motores de audio, rutas paralelas, funciones ficticias ni controles inactivos.
7. **Verificaciones reales:** compilar, ejecutar pruebas unitarias y lint en cada operación que toque interfaz, y probar manualmente gestos/animaciones en un dispositivo antes de declarar concluida la integración. No confundir CI aprobado con una prueba física.

## Fases y aceptación

| Fase | Código visual original a integrar | Resultado que permite cerrarla |
| --- | --- | --- |
| 00 · Preparación | Inventario y mapeo de componentes, fuentes y recursos; árbol base limpio | Nueva rama independiente, diferencias descartadas ausentes y plan versionado |
| 01 · Sistema visual | `ui/theme`, `ui/component` (glass, superficies, tarjetas, desenfoques, movimiento, shared transitions), recursos | Colores, tipografía, esquinas, fondos, efectos y animaciones de la fuente, sin reconstrucciones |
| 02 · Estructura global | `MainActivity.kt` (solo capa Compose), `ui/component/AppNavigation.kt`, `ui/screens/Screens.kt`, navegación y mini reproductor originales | Orden de pestañas, barra inferior, transiciones, insets, gestos y mini reproductor de la fuente; backend musical existente |
| 03 · Inicio | `ui/screens/HomeScreen.kt` y componentes de tarjetas/secciones de portada originales | Inicio con distribución, movimiento y acciones auténticos alimentados por los datos actuales |
| 04 · Buscar | `ui/screens/search` completo y sus sugerencias/categorías | Campo, exploración y resultados originales conectados a la búsqueda existente; elección automática de proveedor, sin exponer nombres de fuentes ni selector de versiones |
| 05 · Biblioteca y colecciones | `ui/screens/library`, álbum, artista, playlist y `ui/component/detail` | Secciones, jerarquía, cabeceras, gestos y menús reales con canciones/favoritos/listas persistentes |
| 06 · Reproductor base | `ui/player/BottomSheetPlayer.kt`, fondos, cubierta, controles, carrusel, transiciones, paneles | Reproductor original completo, con comandos enviados al Media3 actual; no recreación intermedia |
| 07 · Estilos y selector | `ui/player/studio/PlayerSelector.kt`, `PlayerStudioState.kt`, `studio`, `flagship`, `ipod`, `bedside`, `styles`, `customize` | Selector tridimensional original con todos los estilos funcionales de la fuente y V1–V17 añadidos, sin descripciones |
| 08 · Pantallas del reproductor | `ui/player/LyricsScreen.kt`, `QueueScreen.kt`, video, compartir, letras, ajustes por estilo | Controles, listas, páginas, gestos y transiciones de la fuente sin modificar proveedores/VPS |
| 09 · Ajustes y secundarios | `ui/screens/settings`, perfil, búsqueda avanzada, descargas, otras pantallas relevantes | Navegación/orden/estilo originales; funciones conectadas o identificadas explícitamente como incompatibles, nunca falsas |
| 10 · Cierre técnico | Recursos, traducciones, accesibilidad, medidas de rendimiento, tests y revisión de rutas | Pruebas, lint y APK universal aprobados; comprobación física de navegación, reproductores, descargas, búsqueda y cola |

La aceptación de una fase exige **copiar el código real, conectar datos y acciones, validar y documentar las diferencias concretas con el origen**. La presencia de un archivo sin conexión funcional no constituye una pantalla terminada.

## Puntos técnicos identificados antes de comenzar

| Código original | Importancia y riesgo |
| --- | --- |
| `ui/component/AppNavigation.kt` | Orden, barra inferior, animación y propiedad de pestañas: no conservar el dock visual anterior |
| `ui/screens/HomeScreen.kt` | Más de 1.800 líneas; migrar sus componentes por suboperaciones sin reescribir la composición |
| `ui/screens/search/SearchScreen.kt` | Componer la pantalla nativa con el buscador/proveedores existentes, sin crear una segunda búsqueda |
| `ui/screens/library/LibraryHomeScreen.kt` | Adaptadores hacia la biblioteca local y sus listas actuales |
| `ui/player/BottomSheetPlayer.kt` | Respetar el control Media3 de OpenTune; solo incorporar su interfaz |
| `ui/player/studio/PlayerSelector.kt` y `PlayerStudioState.kt` | El selector verdadero posee su propio estado de cámara, movimientos y etapas; los V1–V17 se insertarán allí |
| `ui/screens/settings/MochySettingsScreen.kt` | Reproducir orden, componentes y gestos, conectando solo los ajustes que existen o que se implementen realmente |

## Estado

- [x] Fase 00: crear rama a partir de la base limpia y registrar ruta de migración.
- [x] Fase 01: cimientos visuales originales integrados y verificados mediante tests, lint y APK universal.
- [x] Fase 02: navegación inferior, vidrio, indicador de descargas, cápsula mini y distribución horizontal originales conectados a la infraestructura existente; validación automatizada en ejecución.
- [x] Fase 03: Inicio activado con sus tres distribuciones, carrusel, ruleta Gacha, Canvas, datos y rutas reales. Suite automatizada aprobada en GitHub Actions; validación visual en dispositivo todavía pendiente.
- [x] Fase 04: Buscar y resultados originales activados; CI con tests, lint y APK aprobados en la ejecución 42746516408. Comprobación de dispositivo pendiente.
- [ ] Fase 05: código visual y funcional integrado para Biblioteca, Local/Caché/Importados, fijados y detalles; tests, lint y APK de la versión definitiva por confirmar; falta comprobar gestos y rutas en dispositivo antes de aceptar su cierre físico.
- [ ] Fase 06: reproductor expandido original activado y conectado a Media3; está pendiente el CI del último commit y la inspección física antes de aceptar su cierre.
- [ ] Fases 07–10: pendientes. El estudio tridimensional y las variantes V1–V17 se integran en la fase 07; las pantallas completas de cola/letras, vídeo y otras vistas del reproductor corresponden a la fase 08.

**Prohibido:** añadir pantallas de apariencia semejante, renombrar un tema vacío como terminado, inventar proveedores, romper los enlaces a la VPS o realizar fusiones automáticas a otras ramas.

## Operación 01 · Integración realizada

- Código visual original copiado a `ui/visual/theme`, `ui/visual/component` y `ui/visual/utils`, separado de los servicios musicales. Se han conservado las animaciones, expresiones de dibujo y motores de desenfoque de la fuente y se han ajustado únicamente las referencias necesarias para compilar en este proyecto.
- Tipografía general Inter en siete pesos y fuente adicional BBH Bartle, originales de los recursos; tema Material oscuro, estilos de texto originales, paleta tonal de carátulas, color de fondo extraído de la parte inferior de las cubiertas y extracción original con Material Kolor 4.1.1.
- Superficies e interacción: `SmoothCornerShape`, `Press3D`, `TouchFeedback`, `DeviceTilt`, `RevealOnAppear`, `OdometerText`, `HomeTopGlow`, `ContainerTransform`, `SharedElementTransition` y `PageWipe`.
- Vidrio: `GlassKit`, `BlurEffectCache`, `FrostedGlass`, `ProgressiveBlur` y `AgslShader`, con `RenderNode`, captura de fondo, máscaras, desenfoque descendente y grano originales. Preferencias de apariencia, calidad y acento adaptadas sin modificar Media3 ni sus fuentes.
- La paleta oscura y la tipografía se suministran desde `MainActivityThemeHost`. Se fuerza apariencia de iconos clara sobre barras del sistema por coherencia con la interfaz oscura original.
- Las barras, pantallas y superficies que consumen `glassRoot` se incorporarán desde el código original al sustituir el árbol de navegación en la fase 02. La implementación adicional `ModuleChromeGlass`, ligada a `ActiveSkin` de los estilos externos, se traslada con el sistema de estilos en la fase 07, no mediante un efecto ficticio.
- La compilación, lint y tests se ejecutan por GitHub Actions en esta rama, gracias a su incorporación explícita a `rebuild-safety.yml`. El resultado de CI no se da por aprobado antes de concluir los tres pasos.

## Operación 02 · Navegación y minirreproductor

**Fuente original:** `ui/component/AppNavigation.kt`, `WideBar.kt`, `LibraryDownloadIcon.kt`, `ui/utils/StableInsets.kt`, vectoriales `transport_*.xml` y los componentes animados del minirreproductor.

1. `ui/visual/component/AppNavigation.kt` conserva indicador elástico, gestos y navegación táctil de la fuente. `MainActivityChrome.kt` conecta las pestañas Inicio, Buscar y Biblioteca a `Screens.MainScreens`, `pagerState` y `NavController` reales.
2. `GlassState` se suministra por `LocalOriginalGlassState` en `MainActivityScaffold.kt`. `glassRoot` captura solamente el contenido de rutas; la cápsula y el minirreproductor lo consumen para evitar autorrefracción.
3. `WideBar.kt` usa ancho disponible, insets estables y cutouts para conservar la barra horizontal fusionada de la fuente, en lugar del `NavigationRail` previo. `LocalMiniPillFrame` comparte coordenadas entre pestañas y minirreproductor.
4. `OriginalMiniPlayer.kt` conserva cubierta, título, autor, vidrio, presión, transiciones de iconos, play/pausa/siguiente y anterior en panorámica. Se conecta exclusivamente a `PlayerState` y `rememberPlayerActions` de la aplicación, sin segundo servicio Media3 ni ticker.
5. El gesto de deslizamiento horizontal existente se conserva mediante `SwipeableMiniPlayerBox`; la nueva opción `contentOwnsSurface` evita márgenes dobles. Los insets horizontales del sistema los aplica el contenedor únicamente en la distribución móvil.
6. `LibraryDownloadIcon.kt` toma `DownloadUtil.downloads` para estado, avance y pila de descargas, sin inventar un gestor o un progreso ficticio.
7. `PlayerHost.kt` reserva los anclajes originales: 80 dp de navegación y 64 dp de minirreproductor en móvil; distribución compartida de 72 dp en horizontal. Se preservan los listeners y restauración de estado del reproductor.
8. El `NavHost` utiliza las transiciones originales de un octavo de ancho y fundido de 200 ms, con `TabOwnership` para elegir dirección al entrar, salir y regresar. `AppMotion.enabled` está asociado a la preferencia existente de reducción de movimiento. `SharedTransitionLayout` proporciona el contexto de transiciones de carátulas para las pantallas posteriores.
9. Se actualizan las pruebas de contrato de presentación para comprobar los componentes originales sin eliminar las comprobaciones de gestos, estado compartido, límites de Glass ni acciones de reproducción.

**Verificaciones:** pruebas unitarias, lint y ensamblado universal desde `.github/workflows/rebuild-safety.yml` en la rama de trabajo. La validación de tacto, diseño en dispositivo y transiciones a pantalla expandida todavía no se declara realizada. El traslado completo del reproductor expandido corresponde a la operación 06.

## Operación 03 · Inicio original

- La pantalla activa es `OriginalHomeScreen.kt` desde `GalaxMainPages.kt`. La pantalla anterior permanece como referencia y no se llama desde la pestaña principal.
- `HomeCards.kt`, `OnDeviceMarks.kt`, `HomeTopGlow.kt`, `PageWipe.kt`, `ScrollAwayCover.kt`, `RevealOnAppear.kt` y el ajuste original de cuadrículas aportan las medidas, portadas, colores, desvanecimiento y movimientos.
- Los modos `MOCHYY`, `LISTEN_NOW` y `HOP_IN` son composiciones separadas del original, ligadas a la preferencia `homeStyle`; la selección desde ajustes se completará al trasladar Ajustes.
- Las secciones de reproducción frecuente, historial y favoritos consumen `MusicDatabase`; los accesos directos, listas, contenidos remotos y tendencias usan `HomeViewModel`. Menús y colas utilizan los servicios existentes.
- `OriginalHomeSectionDetailScreen.kt` registra la ruta `section_detail/{sectionId}` que faltaba en el grafo de OpenTune-SY. Usa las tarjetas, búsqueda, gestos y colas existentes y mantiene la navegación sin pantallas fingidas.
- La ruleta Gacha, Canvas y sus recursos originales están integrados y se alimentan de canciones reales. Los canales adicionales de contenido no disponibles en el backend siguen identificados como diferencias; no se inventan resultados.
- Validación exigida para cierre técnico: tests, lint y APK universal de la rama, más verificación manual de toques y diseño en dispositivo.

## Operación 03 · Pantalla Inicio

**Fuentes originales auditadas:** `ui/screens/HomeScreen.kt` (1.898 líneas), `ui/component/HomeCards.kt`, `HeroCarousel.kt`, `HomeTopGlow.kt`, `CoverTint.kt`, `RevealOnAppear.kt`, `ScrollAwayCover.kt`, `PageWipe.kt`, vectores de reproducción/pausa y sonidos de la ruleta.

1. `GalaxMainPages.kt` invoca `OriginalHomeScreen` para la primera pestaña. El Inicio anterior se conserva como respaldo técnico, pero no es la pantalla activa.
2. Las tres distribuciones nativas (`MOCHYY`, `LISTEN_NOW`, `HOP_IN`) utilizan la preferencia persistente `HomeStyleKey`; el control visible de selección en Ajustes corresponde a la operación 09.
3. `HomeCards.kt` conserva tipografía, tamaños 137 dp, separación 15 dp, márgenes 36 dp, portadas, encabezados, filas, tarjetas de vídeo, mosaicos compactos y esqueletos. `CoverTint.kt` y `OriginalHomeFeatureCards.kt` recuperan las tarjetas grandes teñidas por carátulas reales.
4. `OriginalHomeHero.kt` conserva el paginador, desenfoques progresivos superior e inferior, atenuación original, zoom y desvanecimiento por desplazamiento, curva de gesto y reproducción automática de la fuente. `OriginalHomeCanvas.kt` reutiliza el proveedor visual Apple Music y el componente Canvas existentes; deja la carátula estática si no se dispone de vídeo o la red es de consumo medido. Solo construye el reproductor Canvas en una tarjeta activa.
5. `OriginalHomeGacha.kt` proviene del bloque de ruleta original de `HeroCarousel.kt`, separado por su tamaño y complejidad. Incluye malla AGSL, ruta alternativa sin shader en dispositivos antiguos, desplazamiento tridimensional, extracción cromática, táctiles, movimientos de carátulas, vibración y sonidos originales `gacha_tick.wav` / `gacha_win.wav`. El ganador procede de canciones reales y se reproduce con el `ListQueue` / `PlayerConnection` ya instalado.
6. Los apartados de escuchas recientes, más reproducidas, historial, marcadores rápidos, favoritos olvidados, playlists, artistas, catálogo y vídeos disponibles se conectan al `HomeViewModel` y las rutas y menús vigentes. Se reservan las tarjetas de vídeo para filas con artículos reales identificados como vídeos o conciertos; no se muestran títulos ni ejemplos inventados.
7. `OriginalHomeSectionDetailScreen.kt` enlaza las secciones con datos, menús, navegación y cola reales. El Inicio utiliza las mismas funciones de reproducción y resolución de fuentes musicales que antes del trasplante. Las pulsaciones prolongadas de playlists locales abren `PlaylistMenu`, sin acciones vacías.
8. Las filas utilizan la precarga anidada y el desplazamiento con ajuste de las cuadrículas originales. `OriginalHomeIntegrationTest.kt` verifica los puntos de integración, dependencias, recursos y ausencia de motores de audio paralelos.

**Diferencias condicionadas por el backend existente:** los canales propios de contenido comunitario, recomendaciones diarias y clasificaciones enriquecidas que la versión original obtiene mediante su `HomeViewModel` no se pueden declarar equivalentes cuando OpenTune-SY no dispone de esas señales. Se usan los datos y secciones reales disponibles, sin copiar proveedores ni crear resultados ficticios. El selector gráfico de estilos de Inicio se conectará desde Ajustes en la operación 09. La validación de composición, gestos y vídeo en un dispositivo físico sigue pendiente.

**Criterio de verificación:** `testUniversalDebugUnitTest`, `lintUniversalDebug` y `assembleUniversalDebug` deben aprobarse en un mismo run correspondiente al commit final de esta operación.

## Operación 04 · Buscar y resultados originales

- Se traslada `SearchDesign.kt` como `ui/visual/search/OriginalSearchDesign.kt`, conservando los composables originales de barra Glass, colapso, campo, avatar, tarjetas Mood, encabezados, filas, carátulas y estados de carga; adaptaciones de paquete y dependencias de vidrio existentes.
- `OriginalSearchScreen.kt` sustituye la pestaña de búsqueda previa. Conserva transición entre exploración y entrada activa, historial real, teclado y doble toque desde la navegación. No crea un motor de búsquedas paralelo: consulta `UniversalSearchViewModel` y abre `search/{query}`.
- `MoodAndGenresViewModel` recupera el patrón original de cachear hasta dos carátulas por género con un semáforo de tres solicitudes, utilizando `YouTube.browse` existente. El backend vigente entrega una lista plana de géneros; por eso se preservan las tarjetas dobles originales, pero no se inventan encabezados de secciones no recibidos.
- `OriginalSearchResultsScreen.kt` usa la composición Glass de resultados, categorías originales adaptadas a Todo, Canciones, Álbumes y Playlists, tarjeta principal y filas de la fuente. Las variantes se ordenan por `originalRecordingOptions`, y las acciones se conectan a `playSearchResults`, `playAudiusCollection` y `enqueueOffline` existentes.
- Las pistas se agrupan mediante `groupSearchAlbums`; las playlists vienen de la búsqueda Audius que ya existía. No hay selector visible de proveedores, versiones, fuentes o artistas, ni resultados ficticios.
- `MainActivitySearchBar` abandona el overlay visual anterior: el doble toque abre la búsqueda original con foco y la ruta de resultados ya no queda cubierta por la barra antigua.
- Verificaciones pendientes al actualizar este documento: aprobación definitiva de tests, lint y APK universal; validación física del desplazamiento, las transiciones, teclado, vidrio y estado de la mini barra.
- Diferencias no cerradas: la consulta de sugerencias se sirve desde la búsqueda unificada disponible, no desde un segundo motor; el menú contextual y los gestos especiales de las filas de resultados aún deben contrastarse con las acciones originales y la biblioteca real. No se declaran como trasplantados por el mero hecho de existir las tarjetas.


## Operación 05 · Biblioteca, álbumes, artistas y playlists

**Fuentes originales auditadas:** `ui/screens/library/LibraryHomeScreen.kt`, `LibraryTabs.kt`, `LibraryLocalTab.kt`, `LibraryCachedTab.kt`, `LibraryImportedTab.kt`, `LibraryProgressCard.kt`, `LibraryDesign.kt`, `LibraryScaffold.kt`, `LibraryGridScreen.kt`, `LibraryEntityListScreen.kt`, `LibraryManageSheets.kt`; `ui/component/detail/DetailCollapsingTopBar.kt`, `DetailCoverBackground.kt` y `PlaylistDetail.kt`.

1. **Pantalla activa y navegación.** `OriginalLibraryHomeScreen` es el contenido real de la pestaña Biblioteca. Las tarjetas, la jerarquía y la búsqueda interna emplean componentes copiados del código original en `ui/visual/library`. `LibraryTabChips` conserva la distribución adaptable y la animación de selección de las cuatro pestañas: Todo, Local, Caché e Importados. Las subpantallas respetan `library/{category}?shelf={shelf}`, conservan búsqueda y no mezclan los distintos orígenes.
2. **Persistencia y listas.** `MusicDatabase`, `LibraryMixViewModel`, `SpotifyLibraryViewModel` y `DownloadUtil` proporcionan datos reales. Local distingue canciones importadas y descargas terminadas; Caché usa `CachePlaylistViewModel` únicamente cuando hay un observador activo; Importados muestra archivos importados y playlists sincronizadas realmente presentes. Los álbumes/artistas y las playlists se filtran por las canciones concretas de cada origen, mediante `playlistIdsContainingSongs`.
3. **Archivos locales.** `LocalAudioImporter` usa `ActivityResultContracts.OpenMultipleDocuments`, URI persistibles, `MediaMetadataRetriever`, la base de datos actual y el mismo `Media3`. No se sube audio al servidor ni se incorpora un segundo proveedor. `StreamResolver` sirve directamente las URI `content://` y `file://`; los álbumes y artistas locales no invocan consultas remotas cuando están marcados como locales. Los indicadores `OnDeviceMarks` reconocen los identificadores importados.
4. **Gestión de música del dispositivo.** `LibraryLocalManageMenu` traslada el esquema de tarjetas de gestión original con opciones efectivas de añadir, seleccionar y retirar importaciones. La retirada requiere confirmación, borra registros locales y ajusta los álbumes asociados sin borrar los archivos originales; se revocan las autorizaciones de lectura persistidas cuando corresponde. Los resultados de importar y retirar se comunican en la UI; las descargas y el caché conservan sus gestores existentes.
5. **Fijados.** `LibraryPins` guarda tipos e identificadores estables en las preferencias existentes. Los menús de canciones, playlists, álbumes y artistas permiten fijar y desfijar; la cuadrícula `LibraryPinnedTile` procede de la biblioteca original. Una pulsación prolongada retira el elemento fijado, sin tocar el dato subyacente. El formato de persistencia tiene prueba de ida y vuelta, corrupción y límite de capacidad.
6. **Detalles.** `DetailCollapsingTopBar` y `DetailCoverBackground` proceden de la fuente original; `OriginalLibraryDetailHeader` adapta las carátulas y las coordenadas del morph a los modelos existentes. `PlaylistTrackRow` conserva las medidas originales de 39 dp, tipografía Inter, banda de selección, pulsación y marcadores de dispositivo, con `OriginalDetailSongRow` como adaptador. Álbum, artista y playlist muestran esos componentes con menús, colas, compartir, selección, búsqueda, eliminación y reordenamiento ya existentes en OpenTune-SY.
7. **Compatibilidad del backend.** `MusicService`, cola, resolución musical, proveedores, VPS y sistema de descargas no se reemplazan. `SongMenu` omite radio/descarga remota en archivos importados y permite compartir su URI real. Los nuevos componentes de detalle no sustituyen el servicio ni la lógica de pantalla.
8. **Pruebas.** `LibraryPinsTest` y `Operation05LibraryIntegrationTest` cubren persistencia, rutas, conexión de orígenes, importación y eliminación local, fijados, filas, detalles y preservación de comandos originales. Exigir `testUniversalDebugUnitTest`, `lintUniversalDebug` y `assembleUniversalDebug` en una única ejecución del commit final.

**Diferencias justificadas con la fuente:** el módulo original de importación masiva desde proveedores, sus sesiones y el escaneo automático de carpetas no están presentes en OpenTune-SY; por eso se eligió el selector de documentos de Android y la sincronización existente, no botones ficticios. Los detalles reutilizan acciones y ViewModels anteriores detrás de componentes gráficos originales, sin trasplantar todo el coordinador de pantallas/colecciones del otro proyecto. Estas diferencias deben seguirse revisando durante el QA físico.

**Criterio de aceptación pendiente:** confirmar el CI del commit de cierre y probar en un teléfono real la importación y reproducción después de reiniciar la app, retirada sin borrar archivos, playlists filtradas, desplazamiento y morph, pulsación larga, edición/selección, reproducción desde caché y menús. El éxito de CI permite cerrar la implementación técnica, pero no sustituye esa prueba física.


## Operación 06 · Reproductor principal, movimientos y controles

**Fuente original auditada:** `ui/player/BottomSheetPlayer.kt` (2.191 líneas), `MorphingCover.kt` (2.121), `PlayerControls.kt`, `PlayerHandle.kt`, `SongCarousel.kt`, `PlayerProgressSlider.kt`, `SongInfoRow.kt`, `PlayerBottomCardStack.kt`, `PlayerVolumeBar.kt`, `PlaybackClock.kt`, `CoverReflection.kt`, `BackdropBlurTransformation.kt`, `ui/player/flagship/TransportIcons.kt`, `ui/component/SeekBarStyles.kt`, `ButtonStyles.kt` y `ui/player/customize/PlayerCoverLayout.kt`.

1. **Montaje y arquitectura.** `ui/player/Player.kt` mantiene el único `BottomSheetPlayer` / `PlayerHost` y delega la superficie ampliada a `OriginalExpandedPlayer` con `originalExpandedPlayer = true` como opción predeterminada interna. La rama visual anterior permanece como respaldo para incorporar las variantes V1–V17 al selector original en la operación 07. `MusicService`, `PlayerConnection`, `PlayerActions`, Media3, búsqueda, descargas, cola progresiva y VPS no se duplican ni se sustituyen.
2. **Portada y transición.** `SongCarousel.kt` conserva la fila nativa de canciones con `CarouselPictures`, `CarouselCrossfade`, física de muelle, interpolación visual, ranuras estables y movimiento por la velocidad de deslizamiento; `OriginalCoverGestures.kt` enlaza el gesto con `PlayerActions.previous/next`, el doble toque con favoritos y la pulsación larga con el menú existente. `OriginalCoverMotion.kt` reproduce las curvas de viaje y crecimiento tardío originales entre la mini portada de 48 dp y la ampliada; los puntos de partida leen `LocalMiniPillFrame`. La portada edge-to-edge se activa por defecto en Android 12+ y la encuadrada se conserva en Android anteriores; `OriginalWidePlayerContent` adapta la composición a horizontal.
3. **Fondo y reflejos.** `CoverReflection.kt` conserva la malla de colores y reflejo de la fuente, y `BackdropBlurTransformation.kt` realiza el desenfoque a baja resolución; `OriginalExpandedPlayer` combina la portada real, el fondo reactivo a la cubierta y los degradados. No aplica efectos falsos ni usa portadas de ejemplo.
4. **Controles y reloj.** Los cuatro recursos vectoriales `avd_play_to_pause`, `avd_pause_to_play`, `avd_skip_previous` y `avd_skip_next` se trasladan del original, junto con `TransportIcons.kt`, `PlayerGestures.kt` y `OriginalPlayerControls.kt`. Play/pausa, anterior/siguiente, aceleración al mantener el botón, favoritos, aleatorio y repetición envían acciones a `PlayerActions`/Media3. `PlaybackClock.kt`, `SeekBarStyles.kt` y `PlayerProgressSlider.kt` reutilizan reloj por fotograma, pista elástica, respuesta al tacto, sweep de carga y seek al destino real. `PlayerVolumeBar.kt` adapta el control original de volumen al `playerVolume` del servicio.
5. **Título, asa y panel inferior.** `SongInfoRow.kt` conserva tipografía, texto con desvanecido y botones con presión/rebote; el menú corresponde al panel original ya conectado. `PlayerHandle.kt` conserva forma móvil asa/chevrón, respuesta a la luz detrás mediante PixelCopy, inactividad y gesto de cierre. `OriginalPlayerBottomCards.kt` recupera medidas, rellenos, superficies, el apilamiento, la alternancia y el gesto vertical del mazo original; pulsar lleva a la página real de letras o cola, que sigue conectada al `BottomSheetPlayer` actual.
6. **Compatibilidad funcional.** El minirreproductor y la barra fusionada de la operación 02 no se reemplazan. La misma `BottomSheetState` controla el abrir/cerrar, la misma sesión Media3 mantiene el audio y las acciones de la cola, el menú de reproducción y letras se conectan a sus rutas/paneles reales. Los estilos adicionales de Mochy y V1–V17 quedan explícitamente fuera de esta fase; su integración pertenece a la operación 07.
7. **Verificación.** `OriginalExpandedPlayerIntegrationTest.kt` comprueba que el montaje usa `PlayerConnection`, `PlayerActions`, la cola actual, los paneles reales, el carrusel y todos los controles, y no crea un segundo ExoPlayer. `OriginalCoverMotionTest.kt` valida miniatura inicial, posición final y curva de crecimiento. Los pasos obligatorios son `testUniversalDebugUnitTest`, `lintUniversalDebug` y `assembleUniversalDebug`, todos en la misma ejecución del último commit.

**Límites de la fase:** los paneles de letras y cola todavía presentan las pantallas secundarias heredadas; el código completo de esas páginas, transiciones de tarjeta a página y vídeo se traslada en la operación 08. `OriginalExpandedPlayer` es un adaptador de la composición nativa, no un copiado literal de las 2.191 líneas de `BottomSheetPlayer.kt` que incluyen dependencias de estudio y de vistas secundarias aún pendientes. La fidelidad de transiciones, deslizamiento y tacto se debe comprobar físicamente en el dispositivo, sin presentar un CI exitoso como esa prueba visual.
