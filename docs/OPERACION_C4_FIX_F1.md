# C4-fix F1: letras TTML

Base: a3bd83af. Rama comprobada antes de trabajar: GalaxOTxMochyTEST.

## Recorrido y causa

- BetterLyrics.getLyrics/getAllLyrics devuelve el cuerpo TTML; BetterLyricsProvider lo entrega como String sin convertirlo a LRC. LyricsHelper.isEnhancedLyrics reconoce isTtml y prioriza su calidad. No se modifica ese comportamiento.
- C1LyricsScreenViewModel.ensureLyricsLoaded conserva una letra existente; si no hay fila, obtiene el cuerpo de LyricsHelper y hace upsert de LyricsEntity(id, raw). LyricsDao almacena y consulta ese mismo String; PlayerConnection.currentLyrics observa database.lyrics(mediaMetadata.id), y OriginalPlayerConnection expone ese Flow. El precargador tambien usa LyricsHelper. No hay cambios de entidades, consultas ni esquema Room.
- En 1b910969, ui/component/Lyrics.kt detectaba isTtml y llamaba a parseTtml; LyricsUtils.parseLyrics era exclusivamente LRC. El adaptador ui/original/lyrics/LyricsUtils llamaba siempre a ese parser LRC y devolvia una lista vacia para TTML. LyricsScreen lo trataba entonces como texto plano; tarjetas/estilos quedaban sin lineas sincronizadas. OriginalMiniLyrics repetia la llamada LRC incluso para cuerpos que empezaban con '<'. Esa es la regresion activada en C4; no depende de Android 16.
- El parser TTML existente ya admite m:ss.mmm y conserva texto contiguo entre spans y espacios reales entre palabras, con tiempos absolutos por segmento. No hay que inventar tiempos ni reconstruir el texto insertando espacios. Se reutiliza sin cambiarlo.

## Correccion y consumidores

ui/original/lyrics/LyricsUtils.parseLyrics selecciona parseTtml o parseLyrics de la app segun el formato. parseLyricsCached usa ese despacho; por tanto LyricsScreen, PlayerBottomCardStack, AlternatePlayerStyles y VideoModeLyrics reciben los mismos segmentos con sus tiempos. OriginalMiniLyrics usa tambien ese despacho para el mini/ticker. Se mantienen las silabas como segmentos individuales en words, tal como las entregaba parseTtml al reproductor anterior; el sweep usa entry.syllables ?: entry.words y el ticker usa las mismas palabras. No se fusiona ni se inventa una lectura ruby, traduccion, agente o tiempo.

## Regresion

OriginalTtmlLyricsRegressionTest incluye el fragmento recibido sin alterarlo, envuelto en un documento TTML con namespaces solo como andamiaje de prueba (no conocemos los atributos de la raiz original). Comprueba el fallo del antiguo despacho LRC, la equivalencia con parseTtml usado antes de C4, el texto 'Yo quiero hacertelo bien high' con su acento original en la fixture, agente v1 y los siete pares de tiempos exactos. Incluye una cancion de tres lineas, con vecinas sinteticas identificadas como tales, orden temporal, seleccion de linea y final del ultimo segmento. Una tercera prueba conserva LRC por palabra y texto sin tiempos.
No se sustituye ni debilita ninguna prueba de contrato; no hay contrato obsoleto que reclasificar como 04-A.

## Verificacion

Primer intento: BUILD SUCCESSFUL en 5m 26s. :innertube:test correcto (5 pruebas, UP-TO-DATE), :app:testUniversalDebugUnitTest correcto (600 pruebas, 0 fallos/errores; 3 regresiones nuevas), :app:assembleUniversalDebug correcto. APK: app/build/outputs/apk/universal/debug/app-universal-debug.apk. Prefijo JAVA_HOME/ANDROID_HOME solicitado; log local excluido en .artifacts/F1-attempt-1.log, revision cada diez minutos. Maximo tres intentos. Sin lint local en F1, segun el alcance solicitado.
No se dispone de prueba en dispositivo Android 16 ni se afirma un resultado visual; se valida el recorrido de datos y los tiempos mediante regresion automatizada. No se modifica F2-F5.
