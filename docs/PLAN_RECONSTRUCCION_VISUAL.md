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
- [ ] Fase 03: Inicio original activado y enlazado a los datos existentes; verificación automatizada en curso, faltan efectos complementarios del carrusel.
- [ ] Fases 04–10: pendientes. La pantalla ampliada del reproductor y su transición de portada pertenecen a la fase 06.

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
- Se conservan los componentes visuales del origen; la ruleta Gacha y el vídeo Canvas del carrusel original requieren adaptar recursos y datos de otros módulos antes de poder afirmar equivalencia total. No se declara que estén implementados.
- Validación exigida para cierre técnico: tests, lint y APK universal de la rama, más verificación manual de toques y diseño en dispositivo.
