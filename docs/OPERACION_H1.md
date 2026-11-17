# H1: conservar v2 pendiente

La regresión unavailableInnertubeDoesNotCancelPendingResolver falló contra 462c9818: InnerTube fallaba a los 9 s y cancelaba una respuesta válida de v2 prevista a los 12 s. El coordinador espera ahora la otra petición cuando la primera termina sin resultado válido; conserva el límite global de 20 s, una petición v2, cancelación de ambas al cambiar de pista y cancelación de la perdedora tras éxito.

Clasificación 04-A: la expectativa anterior hedgedFallbackDoesNotExtendFailureOrTheTwentySecondDeadline es un contrato obsoleto por el cambio explícito de decisión H1. Exigir terminar a los 15 s con v2 todavía pendiente contradice la espera solicitada. Se sustituye por una comprobación estricta del timeout a los 20 s y por la regresión de éxito a los 12 s. Se mantienen las comprobaciones de cancelación, una sola petición y ausencia de espera tras éxito temprano.
