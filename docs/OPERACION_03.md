# Operación 03 — Superficies y movimiento

Estado: integración de código publicada, validación de CI pendiente.

## Traslado

Doce archivos completos del origen fijado, con procedencia por blob en [el manifiesto](inventario-03/trasplante.json): GlassKit, FrostedGlass, BlurEffectCache, ProgressiveBlur, AgslShader, SmoothCornerShape, TouchFeedback, Press3D, RevealOnAppear, PageWipe, AppMotion y RubberBand.

Se conservan colores, radios, shaders, curvas, tiempos, constantes y algoritmos originales. Las adaptaciones específicas están declaradas por archivo. La licencia del origen está conservada en visual-foundation/LICENSE.

## Conexión real

- MainActivity instala el recompositor original con AppMotion y observa la escala de animación Android; libera el observador al destruirse.
- OriginalVisualHost conecta las preferencias originales y la reducción de movimiento existente. La onda de carga se detiene al salir del estado RESUMED, desactivar la escena o reducir movimiento.
- La superficie de la barra existente ejecuta FrostedChromeGlass y sus respuestas táctiles originales. Su estructura de navegación se traslada en la operación 04.
- El ramal de vidrio modular del origen se conecta al renderer Liquid Glass existente, conservando variante y tuning. Cuando Liquid Glass está desactivado se utiliza el vidrio esmerilado original.
- La fuente original se captura en NavHost; la barra que la lee queda fuera. El estado se renueva al cambiar ruta o abrir Buscar para no reutilizar la captura anterior. Los descendientes de la fuente no reciben el estado capturado ni el backdrop Liquid.
- La apertura de Buscar deja el vidrio original sin captura antes que reutilizar la ruta anterior. Su captura propia se integrará con la pantalla original de la operación 07.
- Las constantes de preferencias originales se aíslan sin sustituir las claves propias; DisableBlur utiliza la clave existente.

## Alcance y comprobaciones pendientes

Las transiciones de navegación, gestos del sheet y efectos específicos de pantallas/reproductores permanecen en sus operaciones 04–16. Los helpers originales están disponibles para esas composiciones; su presencia no certifica que ya estén activadas.

No se ha comprobado la fidelidad en dispositivo, ventanas compactas/expandidas ni escala de texto. CI debe aprobar pruebas, lint y APK antes de cerrar la validación de código.
