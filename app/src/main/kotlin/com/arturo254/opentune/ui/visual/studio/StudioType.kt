package com.arturo254.opentune.ui.visual.studio

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.ui.visual.theme.InterFontFamily

internal object StudioType {
    
    val Tracking = (-0.05).em

    
    val Title = 16.sp
    
    val Subtitle = 11.sp
    
    val Button = 14.sp
    
    val Body = 14.sp
    
    val Caption = 12.sp
    
    val Label = 10.sp
    
    val Name = 20.sp
}

internal fun studioText(size: TextUnit, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = InterFontFamily,
    fontSize = size,
    fontWeight = weight,
    letterSpacing = StudioType.Tracking,
)
