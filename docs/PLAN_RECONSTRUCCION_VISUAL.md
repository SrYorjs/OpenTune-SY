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
- [ ] Fase 01: iniciar el trasplante literal del sistema visual y sus dependencias.
- [ ] Fases 02–10: pendientes, sin fingir completitud.

**Prohibido:** añadir pantallas de apariencia semejante, renombrar un tema vacío como terminado, inventar proveedores, romper los enlaces a la VPS o realizar fusiones automáticas a otras ramas.
