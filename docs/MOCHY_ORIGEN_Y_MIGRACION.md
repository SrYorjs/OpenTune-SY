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

Las modificaciones a estos archivos se limitan a sus paquetes, dependencias externas y rutas de recursos necesarias para construir dentro de GalaxOT. El servicio musical y los modelos de GalaxOT siguen siendo los originales.

## Áreas conectadas

- Inicio: brillo de portadas, tipografía, tarjetas, filas y títulos originales.
- Buscar: píldora de búsqueda original, fichas de categorías, carátulas y resultados de Mochy.
- Biblioteca: tarjetas de categorías, carátulas apiladas y vista de colores de Mochy.

## Componentes que deben conservarse

- MusicMiniBar y barra de navegación inferior de GalaxOT.
- Reproductores V1–V17 de GalaxOT.
- Media3, audio streaming, caché, descargas, resolutores y proveedores.
- VPS, claves, base de datos, entidades persistentes y rutas funcionales críticas.

## Operaciones no concluidas

La migración visual integral aún requiere portar los módulos de reproduccion originales de Mochy (Flagship, Classic, Turntable, iPod, Poster, Timeline, Lyrics, Bedside), su selector y personalización reales, así como las pantallas de álbum, artista, playlist, ajustes, cola, letras y secundarias. El código original de esos módulos tiene dependencias con el motor de reproducción, rutas y entidades de Mochy que no deben importarse sin adaptadores verificables. No se atribuye a esta primera operación una migración completa.

Los recursos binarios y tipografías no se trasladan sin verificar primero sus permisos de distribución; la escala tipográfica y colores sí se copiaron.
