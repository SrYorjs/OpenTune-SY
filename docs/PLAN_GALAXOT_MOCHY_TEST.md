# Trasplante original — GalaxOTxMochyTEST

## Alcance y estado
Se reinicia el trabajo por petición del usuario. La entrega inicial crea la rama y este plan; no implementa pantallas ni certifica fidelidad visual.
Rama exclusiva: GalaxOTxMochyTEST.
Base OpenTune-SY: 8ffbc1acd276cb62a7e86d3bd71adfef895161c8, anterior a las dos migraciones descartadas.
Fuente visual fija: realidkroo/Mochy-Music@f8a3593fd0c37edca3da9012c0eb4bc37e907cc7.
No se fusionarán las ramas de migración previas. Se conservan como respaldo.
No se actualizará silenciosamente el origen durante el trasplante.

## Arquitectura y contrato
La estructura Compose original de Mochy será la interfaz activa: montaje raíz, navegación, pantallas, componentes, recursos, estados visuales, animaciones y gestos originales.
OpenTune-SY aporta identidad de instalación, datos existentes, proveedores, resolución automática, reproducción, cola y descargas.
Los contratos de datos y comandos se adaptan por debajo de la interfaz. No crear pantallas sustitutas, escenas simplificadas, previews ficticias ni controles vacíos.
Copiar archivos originales completos con su jerarquía de dependencias. Limitar diferencias a paquetes/imports, recursos, compatibilidad necesaria, conexiones de datos/acciones y excepciones solicitadas.
Excepciones explícitas: conservar Liquid Glass y sus ajustes, añadir V1–V17, quitar descripciones del selector y referencias de marca visibles. No alterar geometría ni tiempos de animación por criterio estético propio.
Conservar avisos legales de procedencia y licencias en sus archivos correspondientes.
No añadir comentarios al código. No eliminar avisos originales obligatorios.
Mantener un solo servicio de audio y evitar duplicar bases de datos o perder preferencias.
Las reglas de AGENTS.md sobre geometría adaptable, insets y captura de fondos se respetan; sus plantillas Pulse/Prism no reemplazarán las composiciones originales solicitadas por el usuario.

## Hallazgos que motivan el reinicio
La rama anterior sigue montando GalaxMainPages con OriginalHomeScreen, OriginalSearchScreen y OriginalLibraryHomeScreen.
El origen utiliza su propio montaje y NavigationBuilder; BottomSheetPlayer integra directamente selector, estudio, estilos y transiciones.
Reunir componentes copiados en una escena distinta no demuestra equivalencia con esa estructura.
La base elegida contiene fuentes, resolución automática, archivos StudioThemeV1–V17 y GalaxLiquid.
La comparación de árboles con la reconstrucción anterior detecta LocalAudioImporter.kt añadido después: recuperar su comportamiento útil durante Biblioteca solo tras revisión, sin fusionar la interfaz descartada.
La revisión es arquitectónica y de archivos centrales; no constituye una auditoría exhaustiva de todas las líneas ni una prueba de ejecución.

## Operaciones
Cada número es una entrega delimitada. Si excede una sesión, se divide en suboperaciones y permanece abierto.

| Operación | Trabajo | Criterio de cierre |
| --- | --- | --- |
| 00 | Crear rama limpia, fijar revisiones y registrar este contrato | Rama y plan publicados. Completada; sin cambios de interfaz |
| 01 | Inventario completo de rutas, recursos, dependencias y variantes del origen; contratos de datos de destino | Mapa archivo origen → destino, SHA y lista de adaptaciones; todas las pantallas y estilos contabilizados |
| 02 | Dependencias de compilación, tipografías, iconos, recursos y tema originales | Recursos disponibles y compilación; sin rediseñar componentes |
| 03 | Primitivas de superficies, desenfoque y movimiento originales; integración de Liquid Glass existente | Captura correcta del fondo, ajustes conservados y geometría original |
| 04 | Montaje raíz original, navegación, pestañas y transiciones | AppNavigation y navegación reales activos; rutas trazadas desde MainActivity |
| 05 | Minirreproductor original y apertura/cierre del reproductor | Gestos y trayectoria originales, controles conectados al audio real |
| 06 | HomeScreen y todas sus variantes y secciones originales | Distribución, orden, menús y movimientos del origen con datos reales |
| 07 | SearchScreen, exploración, sugerencias y resultados originales | Búsqueda real, selección automática de fuente, paginación y estados completos |
| 08 | Biblioteca original: pestañas, filtros, orden, selección y fijados | Persistencia y navegación reales; no reemplazar colecciones por la cola |
| 09 | Detalles originales de álbum, artista y playlist, menús y transiciones | Acciones de reproducir, guardar, seleccionar, ordenar y descargar verificadas |
| 10 | Archivos locales, caché, importaciones y descargas en sus pantallas originales | Permisos y persistencia tras reiniciar; archivos del usuario preservados |
| 11 | BottomSheetPlayer original completo con Flagship y Classic | Montaje original y controles, portada, fondos y orientación conectados |
| 12 | Letras, cola y páginas del reproductor originales | Gestos y transiciones originales, sincronización, reordenamiento y acciones reales |
| 13 | Estilos nativos restantes | Una suboperación por familia: Turntable, iPod, Bedside y restantes estilos del inventario; cada estilo abre su implementación auténtica |
| 14 | Estudio, selector 3D y editores originales completos | Estados, cámara, tarjetas y personalización originales; estilos nativos funcionales |
| 15 | Incorporar V1–V17 al selector original | Una suboperación por grupo pequeño; cada tarjeta activa su tema real y persiste; sin descripciones |
| 16 | Ajustes, perfiles, paneles y rutas secundarias originales | Orden y navegación completos; dependencias musicales conectadas sin acciones simuladas |
| 17 | Limpieza de marcas visibles y recorrido de todas las rutas | Ninguna pantalla residual del diseño descartado; avisos legales preservados |
| 18 | Validación integral visual, funcional y de rendimiento | Comparación con Mochy en dispositivo, APK probado, pruebas pertinentes y lint aprobados |

## Comprobación por entrega
1. Registrar rutas y blobs originales, archivos de destino y cada diferencia permitida.
2. Verificar que la ruta activa ejecuta el código trasladado; tener un archivo en el repositorio no cierra la operación.
3. Compilar el bloque integrado y ejecutar pruebas relevantes; lint y APK en los hitos de integración.
4. Comparar capturas y grabaciones con la revisión fija de Mochy en igual tamaño, escala, orientación y estado. Registrar por separado comprobación de código, CI y comprobación visual.
5. No marcar una operación completa si quedan gestos, variantes, menús o acciones de su alcance pendientes. No llamar idéntica o 100% eficiente a una integración sin evidencia.
6. Medir arranque de reproducción, fluidez y uso de recursos con pruebas repetibles; no prometer tiempos instantáneos.
7. Commits en español, coherentes y revisables. Evitar borrados masivos y divisiones artificiales en decenas de commits diminutos. Un archivo original puede superar 2.000 líneas: conservarlo completo, declarar el tamaño y separar sus dependencias, sin recortarlo para aparentar un cambio pequeño.

## Estado inicial de validación
Solo se creó una rama desde un commit existente y se agregó documentación. No se compiló una app nueva ni se realizó una comparación visual en dispositivo.
La siguiente entrega es la operación 01. Ninguna operación de interfaz se hereda como completada de las migraciones descartadas.
