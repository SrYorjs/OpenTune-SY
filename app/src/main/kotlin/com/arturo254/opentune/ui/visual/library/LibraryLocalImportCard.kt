package com.arturo254.opentune.ui.visual.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.ui.visual.search.SearchColors
import com.arturo254.opentune.ui.visual.theme.InterFontFamily

@Composable
fun LibraryLocalImportCard(count: Int, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LocalCard(onClick = onClick, modifier = modifier) {
        LocalCardTitle(title)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = count.toString(),
                color = SearchColors.Secondary,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Thin,
                    fontSize = 44.sp,
                    lineHeight = 44.sp,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = subtitle,
                color = SearchColors.Secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
    }
}

@Composable
private fun LocalCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SearchColors.Tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        content()
    }
}

@Composable
private fun LocalCardTitle(text: String) {
    Text(
        text = text,
        color = SearchColors.Primary,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp,
        fontWeight = FontWeight.Bold,
    )
}
