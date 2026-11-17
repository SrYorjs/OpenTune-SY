# G1: resplandor original

Comparación con f8a3593: LyricsGlowLine conserva el shader, el bloom, los umbrales de notas sostenidas y las rutas de GPU originales. LyricsScreen conserva MOTION y highBloom=true por defecto. BORING/PLAIN desactivan el shader; la migración de lyricsWaveAnimation=false conserva esa elección. El bloom no es un halo permanente de toda línea activa: depende de la duración real por carácter. Se conserva el parseo TTML de F1, con tiempos y sílabas originales.

No se encontró una diferencia funcional demostrada que justifique recrear o intensificar el efecto. LyricsGlow registra únicamente estilo, supresión temporal, SDK, aceleración y cantidades de líneas temporizadas; no registra letras ni credenciales. El ahorro de datos no desactiva este shader. AGSL requiere API 33; las versiones anteriores usan el respaldo original.

ADB no detectó un dispositivo durante esta operación. Pendiente: reproducir el caso, comprobar LyricsGlow y observar una nota sostenida; confirmar preferencias guardadas, restauración tras arrastre/morph y renderizado GPU. G1 no se declara reparado visualmente sin esa evidencia. No se fuerzan preferencias ni se alteran los tiempos del proveedor.
