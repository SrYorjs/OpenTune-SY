package com.arturo254.opentune.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.arturo254.opentune.core.designsystem.component.OpenTuneSheet
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

val LocalMenuState = compositionLocalOf { MenuState() }

@Stable
class MenuState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)

    @OptIn(ExperimentalMaterial3Api::class)
    fun show(content: @Composable ColumnScope.() -> Unit) {
        isVisible = true
        this.content = content
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun dismiss() {
        isVisible = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState,
    background: Color = MaterialTheme.colorScheme.surface,
) {
    val focusManager = LocalFocusManager.current

    if (state.isVisible) {
        OpenTuneSheet(
            onDismissRequest = {
                focusManager.clearFocus()
                state.isVisible = false
            },
            containerColor = background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 15.dp, bottom = 13.dp)
                        .width(48.dp)
                        .height(5.dp)
                        .clip(GalaxScreenStyle.pillShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = .35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = .92f),
                                    GalaxScreenStyle.violet.copy(alpha = .62f),
                                ),
                            ),
                        ),
                )
            },
            modifier = modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .galaxScreenBackground()
                    .drawWithCache {
                        val halo = Brush.radialGradient(
                            0f to GalaxScreenStyle.blue.copy(alpha = .11f),
                            .7f to GalaxScreenStyle.violet.copy(alpha = .025f),
                            1f to Color.Transparent,
                            center = Offset(size.width * .82f, 0f),
                            radius = size.width * .93f,
                        )
                        onDrawBehind { drawRect(halo) }
                    }
                    .padding(horizontal = 16.dp)
            ) {
                state.content(this)
            }
        }
    }
}
