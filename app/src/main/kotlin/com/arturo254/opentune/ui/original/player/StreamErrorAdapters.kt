package com.arturo254.opentune.ui.original.player

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.arturo254.opentune.ui.original.adapters.LocalOriginalPlayerConnection

val StreamFailureRed = Color(0xFFFF4D4F)

@Composable
fun rememberStreamFailureProgress(): State<Float> {
    val connection = LocalOriginalPlayerConnection.current
    val error = connection?.error?.collectAsState()
    return remember(error) { derivedStateOf { if (error?.value == null) -1f else 0f } }
}

@Composable
fun rememberStreamFailureLabel(progress: State<Float>): State<String?> {
    val error = LocalOriginalPlayerConnection.current?.error?.collectAsState()
    return remember(error) { derivedStateOf { error?.value?.errorCodeName } }
}
