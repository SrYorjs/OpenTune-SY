# 05/11-2: piezas visuales del mini

Referencia verificada en build/mochy-reference:
f8a3593fd0c37edca3da9012c0eb4bc37e907cc7.

Se trasladan completos MorphingCover.kt (2120 líneas originales),
MorphingSongInfo.kt (202) y MiniLyricsTicker.kt (618) a ui/original/player.
Se conservan cuerpos, transiciones, gestos y variantes. Diferencias de compatibilidad:
paquetes, imports, R de visual-foundation, conexión real, proveedores canvas existentes
y fila del ticker medida con altura mínima natural para escala de fuente aumentada.

OriginalMiniPiecesHost recibe PlayerConnection, página activa e idioma opcional
de traducción. Solo compone en lifecycle RESUMED y página activa, y cancela su
scope al salir. OriginalMorphingCover y OriginalMorphingSongInfo reciben el
track y transporte de PlayerUiState; usan constraints Compose medidos.
OriginalMiniLyricsTicker recibe la letra del mismo host y usa progreso y offset
real del adaptador. No hay montaje en Player.kt ni en el mini actual.

OriginalPlayerConnection expone el mismo player, errores, servicio, letras y
cola atómica; no crea un servicio de audio. El offset se lee de LyricsSyncOffsetKey.
El skip de GalaxOT no tiene el retardo de transport de Mochy: el adaptador lo
declara inmediato y delega seekToNext/Previous al PlayerConnection existente.

Letras: currentLyrics de PlayerConnection/MusicDatabase, alimentado por
PlaybackRuntimeController y LyricsHelper con los proveedores existentes. Parsing
con LyricsUtils de GalaxOT (LRC, palabras y TTML), conservando tiempos, voces y
romanizedTextFlow. Romanización japonesa/coreana usa sus funciones y preferencias
reales. No se inventan tiempos para texto plano: se conserva como una entrada
sin sincronización por palabras. El renderer mantiene sus reglas temporales de
Mochy, incluido su fallback visual para líneas sin un fin proporcionado.

Traducción: la app ya traduce mediante me.bush.translator.Translator en LyricsMenu
y guarda el resultado reemplazando la letra. Ese resultado llega como la letra
actual. Para segunda línea, el host acepta un idioma Language real; únicamente
si se proporciona pide traducciones al mismo Translator y publica sus resultados
en translatedTextFlow. No hay una traducción paralela persistida que se pueda
recuperar automáticamente ni un idioma paralelo activo global. La opción más
simple en el paso 3 es pasar la selección explícita del usuario. Sin ella el
campo permanece null; los fallos de traducción no repiten el original como si
fuera traducción. RubyToken/rubyFlow se conservan del contrato original, pero
GalaxOT no tiene lecturas ruby con rangos de caracteres: quedan null. Para
añadirlas después se necesita tokenización y lecturas reales por palabra.

Dependencias adelantadas de 11-14: carrusel y gestos SongCarousel, controles
TransportIcons de Flagship, CardMorphState/PlayerPageTransition, CardExtraLines,
portadas DiscCover/DiscCoverStack y TurntableArm, geometría y estilos de portada,
fondos Ambient/BlurredCover/CoverVeil/CoverGradientBackdrop, reflejos y canvas.
El ticker requiere KaraokeSweepText y sus auxiliares de LyricsGlowLine,
LyricsSteadyWrap, LyricsInterludeDots y PlaybackClock (operación 12).
Se extraen solo LyricsLoadingDots y las constantes/modelo de geometría de tarjeta
de sus archivos mayores, sin adelantar LyricsScreen ni PlayerBottomCardStack.
Los tres archivos pedidos sí se trasladan completos. Los helpers de índices y
fin de línea conservan el algoritmo de Mochy, separado del parser de GalaxOT.

Canvas usa fetchCanvasArtworkForPlayback con CanvasSourceKey y OpenTuneCanvasKey
reales, álbum y artistas reales; se conserva el renderer silencioso original.
Las dependencias de vídeo se trasladan para que compile el MorphingCover completo:
consulta de vídeo oficial con YouTube de GalaxOT, formatos directos reales y
reproductor visual original con volumen cero, sin sustituir el audio del servicio.
El modelo de formatos local no expone rangos init/index: se usan fuentes
progresivas, no se inventan rangos DASH. No existe el backend de correlación
audio/captions SongVideoMap de Mochy: positionAt queda explícitamente ausente y
se conserva el comportamiento original de vídeo sin ese mapa. Tampoco existe
un deadline de fallo del stream: se muestra el error Media3 real sin inventar
cuenta atrás. La opción más simple para ambos es conservar esa ausencia en 11.

Los relojes ambientales, giro de discos y canvas automático respetan movimiento
reducido; el host limita todos sus consumidores a página activa y RESUMED.
No se alteran los proveedores de GalaxOT, el audio, el player actual ni sus menús.
No se ha probado en dispositivo; la fidelidad visual y los tamaños compactos,
expandidos, bajos y fuente grande requieren validación al montar el paso 3.

Validación del 14 de noviembre de 2026: completada en el tercer y último intento.
El primero detectó incompatibilidades de enum/imports, normalización duplicada,
metadatos de vídeo y dependencias de dibujo/DASH; se corrigieron con las fuentes
reales y el arte original TurntableArt. Se añade media3-exoplayer-dash con la
misma versión Media3 del proyecto, sin cambiar las versiones existentes.
El segundo completó las cuatro tareas y confirmó cero Fatal, pero introducía dos
errores no fatales de lint (indentación y StateFlow.value en composición); ambos
se corrigieron sin supresiones. La verificación final ejecutó conjuntamente
:innertube:test :app:testUniversalDebugUnitTest :app:lintUniversalDebug
:app:assembleUniversalDebug :app:lintVitalUniversalRelease: BUILD SUCCESSFUL
en 24 min 59 s, código 0.
Tests app: 578, cero fallos y errores. APK universal debug generado.
Lint final: 0 Fatal, 113 Error, 593 Warning y 41 Hint; cero errores en los archivos
de las nuevas piezas/adaptadores del mini. Los 113 Error pertenecen al código
preexistente: la tarea lint no implica un proyecto globalmente limpio.
Lint vital universal release final aprobado. No se cambió abortOnError ni reglas.
Lecturas de logs separadas con Start-Sleep -Seconds 300. No se han debilitado
pruebas de contrato ni se detectaron expectativas obsoletas en esta entrega.
El inventario con rutas y blobs está en INVENTARIO_05_11_2.json.
