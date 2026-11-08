# Mochy Music × GalaxOT

## Origen y licencia

Mochy Music: https://github.com/realidkroo/Mochy-Music (realidkroo y colaboradores).

Mochy Music usa GNU General Public License, versión 3. La integración debe conservar atribuciones y avisos de copyright de cualquier archivo trasplantado, y respetar las licencias de sus dependencias. GalaxOT también mantiene un archivo LICENSE con GPL-3.0.

## Componentes de código adaptados

- `ui/component/MochyPress3D.kt`: adaptación del gesto de inclinación y rebote `ui/component/Press3D.kt` de Mochy Music. Aplicado a tarjetas de selección de tema y resultados de búsqueda.
- `ui/component/MochyOdometer.kt`: adaptación del desplazamiento vertical de dígitos de `ui/component/OdometerText.kt` de Mochy Music. Usado en las escenas Bedside.
- `ui/player/mochy/`: implementaciones de diseño y adaptación de metadatos/control al host de GalaxOT, basadas en las categorías de estilos definidas en Mochy Music, sin importar sus servicios de reproducción.

Enlace al código de origen: https://github.com/realidkroo/Mochy-Music/tree/main/app/src/main/kotlin/com/mochyy/mochymusic/ui

## Límites de la integración

La barra inferior flotante, la musicminibar, las transiciones del dock, el motor GalaxLiquid/GalaxGlass, los estilos originales V1–V17, la cola dinámica, la VPS y los resolutores de audio continúan bajo la arquitectura GalaxOT. No se reemplazan los servicios Media3, la base de datos ni las credenciales guardadas.

## Primera integración funcional

Se incorporó una familia independiente de diez estilos visuales inspirados en Mochy Music, accesible desde el reproductor. La elección se guarda bajo `mochyPlayerStyle`; `playerDesignStyle` conserva el valor original V1–V17. El selector ofrece ambas familias, sin cambiar el formato de los ajustes antiguos.

Los estilos incluidos en el primer adaptador visual son Flagship, Classic, Turntable, Full Video (vista panorámica de carátula), iPod (rueda básica), Poster, Timeline, Lyrics Stage (acceso al panel de letras existente), Bedside Clock y Bedside Tiles. Todos reciben sus metadatos de `PlayerState` y usan acciones reales del reproductor; no se importa otro motor de streaming. Estas son adaptaciones iniciales de interfaz, no reproducciones completas de todos los comportamientos de Mochy.

## Funciones todavía no trasplantadas

La primera integración no copia las 525 clases de interfaz de Mochy, ni sus pantallas Home/Search/Library/Settings, módulos, importadores, ecualización avanzada, Crossmix, reconocimiento, video en movimiento, exportador de videos, interfaz TV ni el selector 3D original de tarjetas. Tampoco incorpora aún todas las personalizaciones específicas de los estilos iPod, Bedside y Flagship. Requieren migraciones por componentes, revisión de dependencias, compatibilidad con nuestra navegación y pruebas en dispositivo para no romper las funciones existentes.

Los pasos siguientes de una migración completa deben abarcar los módulos de navegación/pantallas, después controles y gestos, luego capacidades no duplicadas, y finalmente una comparación visual por pantalla con el repositorio de Mochy. Cada archivo trasplantado literalmente deberá mantener su atribución original.
