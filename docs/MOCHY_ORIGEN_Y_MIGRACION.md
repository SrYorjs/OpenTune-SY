# Integración del diseño de Mochy Music en GalaxOT

## Código y derechos de origen

Proyecto original: https://github.com/realidkroo/Mochy-Music

Repositorio y autores: realidkroo y colaboradores de Mochy Music. Licencia original: GNU GPL-3.0. Los archivos incluidos en esta migración proceden del código fuente de Mochy y conservan su atribución mediante este documento y la licencia GPL-3.0 de GalaxOT.

## Cambios deshechos antes del trasplante

Se revirtió por completo la integración provisional que introducía reproductores inventados, selectores de estilos simulados, odómetros y cambios de búsqueda aislados. Commit de reversión: 8ffbc1acd276cb62a7e86d3bd71adfef895161c8. La rama de trabajo continúa siendo opentune-rebuild-v1 y no se modifica main.

## Código original incorporado y conectado

| Archivo en GalaxOT | Original Mochy | Integración |
| --- | --- | --- |
| ui/mochy/component/Press3D.kt | ui/component/Press3D.kt | Gesto visual de presión e inclinación |
| ui/mochy/component/TouchFeedback.kt | ui/component/TouchFeedback.kt | Iluminación de filas y retroalimentación táctil |
| ui/mochy/component/RevealOnAppear.kt | ui/component/RevealOnAppear.kt | Aparición escalonada de elementos |
| ui/mochy/component/SmoothCornerShape.kt | ui/component/SmoothCornerShape.kt | Curvas superelípticas de tarjetas |
| ui/mochy/component/OdometerText.kt | ui/component/OdometerText.kt | Animación de dígitos de Mochy |
| ui/mochy/component/ContainerTransform.kt | ui/component/ContainerTransform.kt | Transiciones compartidas entre carátula y pantalla |
| ui/mochy/component/HomeTopGlow.kt | ui/component/HomeTopGlow.kt | Brillos cromáticos desde las portadas |
| ui/mochy/component/BlurEffectCache.kt | ui/component/BlurEffectCache.kt | Caché de desenfoque |
| ui/mochy/component/PageWipe.kt | ui/component/PageWipe.kt | Transiciones visuales de página |
| ui/mochy/search/MochySearchDesign.kt | ui/screens/search/SearchDesign.kt | Campo, categorías, cabeceras, carátulas y filas |
| ui/mochy/library/MochyLibraryDesign.kt | ui/screens/library/LibraryDesign.kt | Tarjetas de colecciones y carátulas apiladas |
| ui/mochy/home/MochyHomeCards.kt | ui/component/HomeCards.kt | Tarjetas y filas de Inicio |
| ui/mochy/theme/MochyTheme.kt | ui/theme/Theme.kt y ui/theme/Type.kt | Paleta oscura y escala tipográfica |
| ui/mochy/detail/DetailActionRow.kt | ui/component/detail/DetailActionRow.kt | Acciones de reproducir, mezclar y descargar enlazadas a GalaxOT |
| ui/mochy/detail/DetailTrackRow.kt | ui/component/detail/DetailTrackRow.kt | Filas de canciones reales en Álbum |
| ui/mochy/detail/PlaylistScreenHeader.kt | ui/component/detail/PlaylistScreenHeader.kt | Cabecera inmersiva de portadas y degradados en Álbum/Playlists |
| ui/mochy/artist/MochyArtistDesign.kt | ui/screens/artist/ArtistDesign.kt | Cabeceras reales de artista, avatar, título y scrims |
| ui/mochy/settings/MochyCategoryBubble.kt | ui/screens/settings/MochySettingsScreen.kt | Tarjetas originales y acciones de Ajustes |
| ui/mochy/player/bedside/BedsideClock.kt | ui/player/bedside/BedsideClock.kt | Cuatro carátulas originales de reloj |
| ui/mochy/player/turntable/TurntableArt.kt | ui/player/customize/TurntableArt.kt | Geometría y trazos originales del vinilo y del brazo |
| ui/mochy/player/ipod/IpodArt.kt | ui/player/ipod/IpodArt.kt | Dibujo original del iPod |
| ui/mochy/player/ipod/IpodClickWheel.kt | ui/player/ipod/IpodClickWheel.kt | Gestos originales de la rueda |
| ui/mochy/player/ipod/IpodGeometry.kt | ui/player/ipod/IpodGeometry.kt | Tamaños y disposición original del equipo |
| ui/mochy/player/ipod/IpodHaptics.kt | ui/player/ipod/IpodHaptics.kt | Respuesta háptica de rueda |
| ui/mochy/player/ipod/IpodLook.kt | ui/player/ipod/IpodLook.kt | Acabados y colores de rueda |
| ui/mochy/player/ipod/IpodMenu.kt | ui/player/ipod/IpodMenu.kt | Árbol de menús, pila, selección y transición originales |
| ui/mochy/player/ipod/IpodScreen.kt | ui/player/ipod/IpodScreen.kt | Filas, resaltado, desplazamiento, sombreado y transiciones originales |
| ui/mochy/player/ipod/IpodCoverFlow.kt | ui/player/ipod/IpodCoverFlow.kt | CoverFlow 3D original con giro y reverso de pista |
| ui/mochy/player/ipod/IpodHeader.kt | ui/player/ipod/IpodHeader.kt | Cabecera y batería auténticas del iPod |

La integración de los reproductores utiliza escenas puente propias de GalaxOT en `ui/mochy/player/`; estas escenas conectan gráficos y gestos auténticos de Mochy a `PlayerState`, `PlayerActions` y Media3, sin importar la arquitectura de fuentes de Mochy.


Las modificaciones a estos archivos se limitan a sus paquetes, dependencias externas y rutas de recursos necesarias para construir dentro de GalaxOT. El servicio musical y los modelos de GalaxOT siguen siendo los originales.

## Áreas conectadas

- Inicio: brillo de portadas, tipografía, tarjetas, filas y títulos originales.
- Buscar: píldora de búsqueda original, fichas de categorías, carátulas y resultados de Mochy.
- Biblioteca: tarjetas de categorías, carátulas apiladas y vista de colores de Mochy.
- Ajustes: tarjetas de categorías originales conservando navegación y opciones de GalaxOT.
- Álbum: cabecera inmersiva, botones y lista de canciones originales de Mochy con las funciones GalaxOT.
- Playlists: cabecera inmersiva original de Mochy en listas locales y online.
- Artistas: cabecera, fondos, retrato y título original de Mochy; acciones y secciones de GalaxOT.
- Reproductor: modos Bedside, Turntable e iPod con código visual original de Mochy, junto a V1–V17; no se reemplaza la barra inferior.
- iPod: navegación por rueda, menús de selección y CoverFlow 3D de Mochy conectados a la cola real de reproducción y a las acciones Media3 existentes; se preserva la cabecera con batería del dispositivo.

## Componentes que deben conservarse

- MusicMiniBar y barra de navegación inferior de GalaxOT.
- Reproductores V1–V17 de GalaxOT.
- Media3, audio streaming, caché, descargas, resolutores y proveedores.
- VPS, claves, base de datos, entidades persistentes y rutas funcionales críticas.

## Operaciones no concluidas

La migración integral aún requiere los reproductores originales Flagship, Classic, Poster, Timeline, Lyrics Stage y variantes avanzadas de video; la totalidad del estudio/selector 3D de Mochy, ajustes avanzados por estilo y pantallas secundarias (cola, letras, detalles alternativos). El menú iPod y CoverFlow se han trasplantado y utilizan canciones, álbumes y artistas presentes en la cola de GalaxOT, pero aún falta la navegación completa de la biblioteca y playlists de Mochy. Los resultados de biblioteca no se simulan con datos ficticios: cuando la página de playlists no está conectada se informa expresamente. Bedside, Turntable e iPod ya tienen modos visuales funcionales, pero no todas sus funciones del original se han trasladado. El catálogo de fuentes, la VPS y las rutas de navegación importantes se mantienen en GalaxOT por petición expresa. No se debe afirmar que todas las pantallas de Mochy están copiadas o que los modos representan funciones aún ausentes.

Los recursos binarios y tipografías no se trasladan sin verificar primero sus permisos de distribución; la escala tipográfica y colores sí se copiaron.
