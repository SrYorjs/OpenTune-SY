# 05/11, paso 1: adaptadores de estado

Fuente fija: `realidkroo/Mochy-Music@f8a3593fd0c37edca3da9012c0eb4bc37e907cc7`.

Traslado completo a `ui/original/player` de `PlayerUiState.kt` (blob
`e528338ea7de9d03be356361576affd3be432c05`) y `models/PlayerModels.kt`
(blob `6c6815d31f1df355062caa77365d56418a9f1326`). Se conservan todos los
campos y modelos. Adaptaciones: paquetes, import de la conexión adaptada,
estado inicial de transporte y cola leído de Media3, y disponibilidad explícita
de `QueueItemData.isAutoplay` mediante `Boolean?`.

`OriginalPlayerConnection` recibe la conexión existente y un scope del consumidor;
no crea servicio ni reproductor. Debe construirse en el hilo de aplicación de
Media3 y recibir una señal real que combine página activa y lifecycle RESUMED.
No se instancia todavía en ninguna pantalla.

- TrackInfo: `MusicService.currentMediaMetadata`, a través de PlayerConnection,
  y `MusicDatabase.song(id)` para liked, conservando la proyección original.
- TransportState: playWhenReady, playbackState, shuffle y repeat del único
  reproductor; disponibilidad de comandos next/previous consultada en Media3,
  incluso en el estado inicial. El significado original de isPlaying conserva
  intención de reproducción mientras buffering; playbackState lo distingue.
- QueueState: timeline real en orden de reproducción/shuffle, índice del mismo
  reproductor, título del servicio, uid, metadatos y duración de cada ventana.
- ProgressState: currentPosition y duration de Media3, normalizadas como en
  Mochy. Sondeo de 66 ms solo con suscripción y página activa/resumida;
  se cancela al perder cualquiera de esas condiciones.

Dato ausente: GalaxOT no conserva en MediaItem la procedencia Autoplay.
Se representa con null. La alternativa futura más simple es registrar esa
procedencia en extras al insertar elementos automáticos y conservarla al
restaurar la cola; no se puede inferir de la posición ni del título.
No existe el backend Cast de Mochy en esta conexión: el transporte representa
el reproductor de GalaxOT, sin simular una sesión remota.
Los valores vacíos iniciales de TrackInfo son los del contrato original mientras
se resuelve la consulta de base de datos; no son metadatos de una pista ficticia.

Player.kt y el minirreproductor actual no se modifican. Este paso no certifica
integración visible ni fidelidad visual.

Validación del 14 de noviembre de 2026: `:innertube:test` aprobado (UP-TO-DATE),
`:app:testUniversalDebugUnitTest` aprobado (575 pruebas, cero fallos y errores),
`:app:lintUniversalDebug` y `:app:assembleUniversalDebug` aprobados.
Resultado conjunto: BUILD SUCCESSFUL en 24 min 8 s. Se utilizó el JBR de
Android Studio y el SDK local indicado por el usuario. El primer lanzamiento
fue rechazado por el sandbox (Acceso denegado); la repetición autorizada
completó las cuatro tareas. No hubo errores Kotlin ni pruebas de contrato
obsoletas; no se modificaron pruebas. Gradle avisó del proceso git usado
por la configuración de compilación al guardar la caché de configuración;
no impidió ninguna tarea.
