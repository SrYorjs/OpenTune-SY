# 04-D: arranque

Comparación de código con `1b910969`: el NavHost anterior abría Inicio por
defecto, respetaba la pestaña configurada y los accesos directos; no exigía
login ni setup. Mochy, revisión fija f8a3593, sí monta SetupWizardContainer
con setupCompleted y marcador de instalación. 04-A3 copió esa condición pero
su adaptador mostraba AccountSettings. Una instalación limpia cumplía siempre
la condición y sustituía todo el contenido por ajustes de cuenta.

Opciones: conservar ese editor como onboarding provisional, o abrir directamente
la navegación existente. Se elige la segunda por coincidir con el comportamiento
anterior. Se elimina el adaptador obligatorio y su escritura de preferencias de
setup; no se marca ficticiamente completado el futuro asistente de operación 16.
Se mantienen las rutas voluntarias de ajustes de cuenta. El scheduler de soporte
se habilita después de la intro, sin depender de un asistente inexistente.

La configuración anterior aplicaba atributos android de splash solo desde API 31,
sin Theme.SplashScreen ni atributos compatibles ni postSplashScreenTheme. Por ello
installSplashScreen no recibía el contrato completo de AndroidX; antes de API 31
el icono de su splash de compatibilidad no estaba configurado. Desde API 31 el
splash del sistema sí tenía un PNG de launcher, pero con tamaño/encuadre distinto
al logo de la intro. Su fondo #071021 era azul; LaunchIntro dibujaba negro.
Esto explica por código un tramo inicial azul y la discontinuidad. No se ha
confirmado en dispositivo qué contribución concreta causaba el icono ausente
reportado en ese APK ni se afirma que todo Android 12+ lo ocultase.

MainActivity usa ahora Theme.GalaxOT.Starting, basado en Theme.SplashScreen,
con atributos AndroidX y los de plataforma para API 31+. postSplashScreenTheme
restaura Theme.GalaxOT. Splash e intro comparten el recurso negro de fondo.
El drawable específico usa el icon.png ya trasladado, en viewport 288dp y bounds
equivalentes a (58,48)-(186,176) del viewport 235 de LaunchMarkArt: misma escala,
posición y fase inicial de la intro (AppIcon.usesLauncherSplash sigue siendo false).
No se cambia el launcher ni se activa un prelude nuevo. La ventana posterior usa
fondo negro para evitar otro logo estático detrás de Compose. Se conserva el
handoff de 90/200 ms, sin hold nuevo, y los tiempos originales de vuelo, rise,
prelude, easing y espera de esenciales. La espera original ya dibuja el logo
desde el primer frame: no se añade un retraso ni una pantalla.

Clasificación 04-A de contratos: las expectativas de la 04-B que exigían usar el
PNG de launcher como splash y el drawable antiguo como fondo de ventana son
expectativas estructurales obsoletas. Se reemplazan por comprobaciones de tema
de entrada real en el manifest, tema posterior, atributos de ambas API, bitmap
original, bounds, fondo compartido, tiempos y señal de salida. Se añade contrato
para evitar el retorno de AccountSettings/setup obligatorio. No se omiten tests
ni se relajan los contratos de identidad o animación.

Verificación del 14 de noviembre de 2026: una sola ejecución conjunta de
`:innertube:test :app:testUniversalDebugUnitTest :app:lintUniversalDebug
:app:assembleUniversalDebug`, BUILD SUCCESSFUL en 24 min 29 s. Consultas del log
separadas por Start-Sleep -Seconds 300. Sin errores Kotlin ni fallos de contrato;
no fue necesario un segundo intento. Se utilizó JAVA_HOME del JBR de Android
Studio y ANDROID_HOME del SDK local indicados por el usuario.
Tests de app: 576, cero fallos y errores. La tarea lint finalizó con éxito por
la configuración existente `abortOnError=false`; su informe completo contiene
127 errores, 577 advertencias y 41 hints. Por tanto, BUILD SUCCESSFUL no significa
lint limpio. No se cambió dicha configuración ni se desactivaron reglas. Entre
las incidencias están NewApi en navigationBuilder y RestrictedApi en los accesos
al backstack, líneas de navegación no modificadas en esta tarea. Los recursos
nuevos de splash no figuran como errores en ese informe.

Sin prueba visual en dispositivo; la alineación descrita
es la del contrato de recursos y Compose, pendiente de validar en Android 26-30,
31-32 y 33+ (incluido encuadre del sistema, insets y arranque frío/caliente).
