# Fase 3 — Cierre de Design System

| Subfase | Resultado |
| --- | --- |
| 3.1–3.13 | Auditoría, tokens, Glass, componentes, navegación y overlays |
| 3.14 | Estados reutilizables loading/empty/error/shimmer |
| 3.15 | Integración gradual en Home/Search/Library/Settings |
| 3.16 | Deduplicación de empty/Settings y contrato de regresión |

**Criterios para marcar completo:** `testUniversalDebugUnitTest`,
`lintUniversalDebug`, `assembleUniversalDebug` en verde y checklist
de dispositivo verificada por la persona que instala la aplicación.

**Criterios fuera del alcance del gate:** comparar capturas lado a lado
en teléfono real; consistencia de sombras Glass en hardware real;
comportamiento de características que necesitan cuentas/proveedores.
No afirmar que pruebas de código prueban identidad visual de píxeles.

**Limpieza deliberadamente diferida:** wrappers y componentes con
consumidores funcionales aún activos (media item/Player/nav/Glass).
Removerlos solo tras migración y regresión individual.
