package com.arturo254.opentune.ui.original.adapters
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
/** C3 must mount the entire selector/editor subtree here; leaving it cancels ambient work. */
@Composable fun OriginalStudioHost(pageActive: Boolean, content: @Composable () -> Unit) {
 val lifecycle = LocalLifecycleOwner.current.lifecycle
 var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
 DisposableEffect(lifecycle) {
  val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
  lifecycle.addObserver(observer)
  onDispose { lifecycle.removeObserver(observer) }
 }
 if (pageActive && resumed) content()
}
