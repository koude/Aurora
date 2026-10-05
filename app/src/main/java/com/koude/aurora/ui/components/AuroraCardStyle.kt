package com.koude.aurora.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AuroraCardStyle {
    val ContentPadding: Dp = 18.dp

    @Composable
    fun groupShape(): Shape = MaterialTheme.shapes.large

    @Composable
    fun itemShape(): Shape = MaterialTheme.shapes.medium

    @Composable
    fun groupColor(): Color = MaterialTheme.colorScheme.surfaceContainer

    @Composable
    fun itemColor(): Color = MaterialTheme.colorScheme.surfaceContainerLow
}
