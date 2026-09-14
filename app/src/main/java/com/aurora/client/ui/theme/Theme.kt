package com.aurora.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuroraColors = lightColorScheme(
    primary = Color(0xFF5D8F7D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDECE6),
    background = Color(0xFFF7F9F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF3F0)
)

@Composable
fun AuroraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AuroraColors, content = content)
}
