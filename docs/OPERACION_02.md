# Operación 02 — Recursos y tema originales

Rama: `GalaxOTxMochyTEST`.
Fuente fija: `realidkroo/Mochy-Music@f8a3593fd0c37edca3da9012c0eb4bc37e907cc7`.

## Código integrado

- Módulo `visual-foundation`: 815 recursos y ocho archivos Kotlin originales del tema.
- Fuentes Inter, Google Sans Flex, Sans Flex y BBH Bartle, iconos, imágenes y recursos raw originales.
- Prefijo `original_` y paquete separado para evitar colisiones con los recursos existentes.
- `MainActivityThemeHost` ejecuta `OriginalTheme` y proporciona los tokens del sistema existente dentro de ese tema; conserva negro puro, offset de letras, radio de portada y movimiento reducido.
- Dependencias visuales: MaterialKolor, AppCompat, splashscreen, Coil SVG, Lottie, smooth-corner, adaptive/layout/navigation y uCrop. Sin downgrade global del proyecto.
- Licencia y procedencia conservadas en `visual-foundation/LICENSE` y `visual-foundation/NOTICE.md`.

## Verificación del 11 de noviembre de 2026

- 823 destinos del manifiesto presentes.
- 74 binarios con blob Git idéntico al origen fijado, incluidas fuentes.
- Ocho archivos del tema comparados completos: coinciden tras las transformaciones documentadas de paquetes, identificadores de recursos, nombre de entrada y retirada de comentarios no legales.
- Los 186 archivos de `inventario-01/base-protegida.csv` conservan sus blobs originales.
- La ejecución anterior compiló el código, pero falló una prueba estructural que exigía el antiguo host de tema. Se sustituyó esa expectativa obsoleta por comprobaciones de la conexión original, tokens, parámetros y conservación de claves de preferencias. No se omitieron pruebas.

## Límites

Esta operación prepara recursos y tema. No certifica fidelidad visual en dispositivo ni traslada las pantallas, navegación, gestos o selector. Estos siguen el orden del plan.
Los XML de configuración Android excluidos conservan identidad, servicios y permisos del destino; los assets de resolución musical del origen no sustituyen los proveedores existentes.

## Cierre

Operación 02 completada. Código validado: `af4cb71f7397d6d7b6d4651c8586a0115b65383a`.
[GitHub Actions: pruebas, lint y APK universal aprobados](https://github.com/SrYorjs/OpenTune-SY/actions/runs/42993794042).
El APK está disponible en el artefacto `OpenTune-rebuild-universal-debug` de esa ejecución.
No se ha realizado prueba visual en dispositivo.
Siguiente: operación 03, superficies y animaciones originales e integración del Liquid Glass existente.
