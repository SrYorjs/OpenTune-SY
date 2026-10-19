# GalaxOS — reconstrucción visual integral

Rama de trabajo exclusiva: `opentune-rebuild-v1`.

Base de esta intervención: `52ea8b215b836b6b3ba332e8b10d07f999f2d6e7`.

## Contrato del producto

Una nueva experiencia de música galáctica, limpia, premium y accesible. Jerarquía visual basada en imágenes grandes y tipografía clara; navegación eficiente; superficies de espacio profundo, nebulosa azul/violeta y acentos sutiles. Las portadas de canciones y álbumes son cuadradas con bordes redondeados, y las imágenes de artistas son circulares. No introducir recuadros genéricos con bordes pesados.

La barra inferior de tres destinos (Inicio, Buscar, Tu biblioteca) permanece **exactamente intacta**, al igual que los contratos y variantes del efecto Liquid Glass. No cambiar ni su posición ni el comportamiento de gestos existente. Mantener intactos MusicService, Media3, fuentes de búsqueda, descarga offline, bibliotecas, EQ y ajustes persistidos.

## Etapas ordenadas

1. **Sistema visual (primer avance publicado).** Sustituir fondo y superficies de destinos, jerarquía de encabezados y geometría común, sin introducir blur de pantalla completa. Respetar paletas personalizadas, modo claro y movimiento reducido. Código: `core/designsystem/theme/GalaxScreenStyle.kt`.
2. **Inicio y reproductor (avance parcial).** Nueva portada editorial con contenido real y colores extraídos en IO, shuffle principal, reconocimiento secundario, controles táctiles con spring y material galáctico. Perfilar reconstrucción de PlayerHost/Canvas/Pix​elCopy y transición mini/expandido; no marcar 60 FPS antes de medir. Código: `ui/screens/GalaxFeaturedTrack.kt`, `ui/screens/GalaxHomeActions.kt`, `ui/screens/HomeScreen.kt`. **Pendiente**: reescritura de la estructura general del reproductor y migración de todas sus variantes sin perder preferencias.
3. **Descubrimiento y colección (avance parcial).** Campo de búsqueda tipo cápsula y géneros; cabeceras editoriales de álbumes, artistas, playlists; después rehacer listados de resultados, filtros, filas, orden y detalles, manteniendo paginación, offline y acciones. Código: `ui/screens/GalaxMainPages.kt`, `ui/screens/GalaxMediaHeader.kt`. **Pendiente**: reemplazo completo de las estructuras internas de Buscar/Biblioteca.
4. **Pantallas secundarias (pendiente).** Menú de tres puntos, panel de cola, letras, EQ, temporizador, información de canción, playlists locales/remotas, descargas, álbumes, artistas, Music Recognition, estadísticas, historia, descubrimiento, catálogo, cuenta, ajustes y todas las subrutas de apariencia, privacidad, almacenamiento, contenido, integraciones, AOD, backup, autenticación y WebViews. Inventario completo mediante `ui/screens/NavigationBuilder.kt` y componentes respectivos. Rehacer su capa visual una ruta a la vez conservando ViewModels/estados.
5. **Rendimiento y control de calidad (en curso).** Prevenir trabajo por fotograma en componentes estáticos; conservar LazyColumn/LazyRow con claves estables; medir jank, GPU y recomposiciones en S24 FE; probar modo Canvas activo, blur, scroll, reanudación, Seek, offline, teléfono a 60/120 Hz. Safety Gate: `testUniversalDebugUnitTest`, `lintUniversalDebug`, `assembleUniversalDebug`.

## Estado verificable

| Requisito | Estado |
| --- | --- |
| Rama exclusiva | Aplicado |
| Mantener barra inferior intacta | Aplicado: no se han modificado sus archivos |
| Mantener Liquid Glass | Aplicado: no se han modificado renderer, tuning ni variantes |
| Sistema visual radical y compartido | Primera etapa implementada, pendiente auditoría visual |
| Tarjeta destacada con canción real, carátula y color | Implementado |
| Aleatorio activo y reconocimiento musical secundario | Implementado |
| Campo principal de descubrimiento con cristal | Implementado |
| Cabeceras de álbumes/artistas/playlists | Reconstruidas |
| Rehacer literalmente todas las pantallas y subpaneles | Pendiente |
| Reescritura integral de reproductor, FPS, transición mini | Pendiente / mejoras previas preservadas |
| Estabilidad de 60 FPS | No verificado sin profiling en dispositivo |
| Kotlin final sin comentarios en los nuevos componentes | Aplicado en esta etapa |
| Tests, lint, APK y comprobación S24 FE | GitHub Actions y teléfono deben verificarse para el commit final |

## Entrega de código completo

La implementación fuente está en:

- [GalaxScreenStyle.kt](../../app/src/main/kotlin/com/arturo254/opentune/core/designsystem/theme/GalaxScreenStyle.kt)
- [GalaxFeaturedTrack.kt](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxFeaturedTrack.kt)
- [GalaxHomeActions.kt](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxHomeActions.kt)
- [HomeScreen.kt](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/HomeScreen.kt)
- [GalaxMediaHeader.kt](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxMediaHeader.kt)
- [GalaxMainPages.kt](../../app/src/main/kotlin/com/arturo254/opentune/ui/screens/GalaxMainPages.kt)

Para actualizar Android Studio sin perder modificaciones locales: `git status --short`, `git fetch origin`, `git switch opentune-rebuild-v1`, `git pull --ff-only`, `git log -1 --oneline`.

Esta fase **no equivale a una reconstrucción literal completa**. Ninguna página pendiente se marcará como finalizada antes de reemplazar su composición, verificar funcionalidad y pasar la compilación.
