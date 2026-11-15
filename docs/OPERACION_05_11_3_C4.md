# 05/11-3 C4: activar reproductor original

Base: 7e1a3133, rama GalaxOTxMochyTEST.

## Textos antes de activar
Busqueda de literales Kotlin en todo ui/original y ui/player, ignorando comentarios y distinguiendo las claves/URLs que no son mensajes. La salida completa de literales antes de editar se guarda localmente en .artifacts/C4-brand-scan-before.json (no versionada).
Coincidencias mostrables:
- original/adapters/C1Capabilities.kt:6: "GalaxOT has no Mochy output/Cast backend" -> "Device output and casting are unavailable".
- original/adapters/C2Capabilities.kt:8: "GalaxOT has no active Mochy look-module runtime" -> "Look modules are unavailable".
- original/adapters/C2Capabilities.kt:9: "GalaxOT does not expose a PCM spectrum tap" -> "Audio spectrum is unavailable".
- original/adapters/C2Capabilities.kt:11: "Mochy layout editor cannot edit V1?V17; selection remains available" -> "Layout editing is unavailable for V1-V17; selection remains available".
- original/player/ipod/IpodMenu.kt:21: IpodPage("Mochy") -> IpodPage("Music").
El mensaje citado no estaba en Player.kt: estaba en C2Capabilities.kt. Se usa V1-V17 en ASCII para evitar otra conversion de codificacion. Tras la limpieza quedan nueve coincidencias tecnicas de esos literales: la ruta mochy_settings, el identificador galaxMochyEditor, las identidades/valor de familia mochy y la URL de manifiesto interno. No se cambia ninguna de esas identidades ni los datos de canciones/usuarios.

## Activacion y rollback
USE_ORIGINAL_PLAYER = true en ui/original/adapters/PlayerMountGate.kt. MainActivity ya llama a ese gate. Se activa el host original completo: mini, expansion, letras, cola, selector y Studio, con los 17 PlayerDesignStyle del catalogo unificado y sus componentes nativos reales. Se conservan playerDesignStyle/playerStyle y las selecciones. No se borra ni modifica el Player.kt actual ni el mini actual: la rama CURRENT conserva su llamada y sus argumentos. Volver la constante a false y recompilar restaura ese montaje.
Modulos, espectro PCM, Cast/output y traduccion paralela siguen ocultos o deshabilitados por las capacidades y los filtros del host; no se generan datos para simularlos. El editor de piezas solo se habilita para los layouts que puede editar. Las fuentes de fondo separadas y variante/tuning de Liquid Glass se conservan.

## Contrato de activacion, clasificacion 04-A
OriginalPlayerMountContractTest.safetySwitchKeepsTheCurrentMountByDefault exigia false en C3. Es una expectativa de fase obsoleta al autorizar C4, no un contrato de rollback que deba desaparecer. Se sustituye por activationSelectsTheOriginalAndRetainsRollback: exige true y montaje ORIGINAL por defecto, y conserva la comprobacion playerMount(false) == CURRENT. Se mantiene asimismo la prueba de la rama original con true, el cableado de MainActivity, los renderers/previews, los 17 temas y su retorno de video. No se omiten ni se debilitan contratos.

## Lint y verificacion
Antes de ejecutar Gradle se guarda el XML anterior: 113 Error, 0 Fatal, 593 Warning, 41 Hint. SHA-256 del XML base: 041dc5c2c2ca3469f1db069156184ecffab0f4a4423c1f7ae7db52c574902e61.
La comparacion es multiconjunto por regla, severidad, mensaje, archivo y codigo indicado (sin usar las lineas como identidad; se normalizan referencias a numeros de linea dentro del mensaje). Asi se detectan nuevos errores incluso si desaparece otro y el total se mantiene igual. No es un baseline de supresion ni se modifica lint.xml, abortOnError o reglas.
Primer intento completo: BUILD SUCCESSFUL en 26m 8s, pero la condicion adicional no se cumplio: 118 Error, 0 Fatal, 615 Warning, 42 Hint; comparacion exacta: 5 errores nuevos. Los cinco proceden del traslado C2 (38c6b49f, comprobado con git blame): dos NewApi por Shader.TileMode.DECAL en ExpressiveText, NonObservableLocale en BedsideInfoTile y dos StateFlowValueCalledInComposition en StyleKit/VideoModeControls.
Correcciones reales: antes de Android 12 se dibuja el contenido sin acceder a DECAL; el glow GPU exige API 31. El idioma se lee de LocalConfiguration observable. El progreso se recoge con collectAsState() y se deriva la duracion/segundos con derivedStateOf, manteniendo el valor inicial real sin StateFlow.value directo en composicion. Se agregan contratos sobre mensajes sin marcas/codificacion corrupta y titulo de iPod. No se agregan supresiones ni se cambian reglas.
Segundo intento completo: BUILD SUCCESSFUL en 27m 42s. :innertube:test (5 pruebas, sin fallos; UP-TO-DATE), :app:testUniversalDebugUnitTest (597 pruebas, sin fallos), :app:lintUniversalDebug y :app:assembleUniversalDebug correctos. Lint: 113 Error anteriores, 0 nuevos por comparacion multiconjunto, 0 Fatal, 615 Warning y 42 Hint. :app:lintVitalUniversalRelease: BUILD SUCCESSFUL en 9m 24s; aviso de configuration cache por git rev-parse existente, sin fallo. APK: app/build/outputs/apk/universal/debug/app-universal-debug.apk. Dos intentos completos de un maximo de tres. Se emplea el prefijo JAVA_HOME/ANDROID_HOME solicitado y se revisa el log como maximo cada diez minutos con Start-Sleep -Seconds 600.
No se afirma una prueba visual en dispositivo: platform-tools/adb.exe no esta disponible en el SDK configurado.
