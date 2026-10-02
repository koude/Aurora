package com.koude.aurora.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = AuroraPrimary,
    onPrimary = AuroraOnPrimary,
    primaryContainer = AuroraPrimaryContainer,
    onPrimaryContainer = AuroraOnPrimaryContainer,
    secondary = AuroraSecondary,
    onSecondary = AuroraOnSecondary,
    secondaryContainer = AuroraSecondaryContainer,
    onSecondaryContainer = AuroraOnSecondaryContainer,
    surface = AuroraSurface,
    surfaceContainer = AuroraSurfaceContainer,
    onSurface = AuroraOnSurface,
    onSurfaceVariant = AuroraOnSurfaceVariant,
    outline = AuroraOutline,
    outlineVariant = AuroraOutlineVariant,
    background = AuroraSurface,
    onBackground = AuroraOnSurface,
)

private val DarkColors = darkColorScheme(
    primary = AuroraDarkPrimary,
    onPrimary = AuroraDarkOnPrimary,
    primaryContainer = AuroraDarkPrimaryContainer,
    onPrimaryContainer = AuroraDarkOnPrimaryContainer,
    secondary = AuroraDarkSecondary,
    onSecondary = AuroraDarkOnSecondary,
    secondaryContainer = AuroraDarkSecondaryContainer,
    onSecondaryContainer = AuroraDarkOnSecondaryContainer,
    surface = AuroraDarkSurface,
    surfaceContainer = AuroraDarkSurfaceContainer,
    onSurface = AuroraDarkOnSurface,
    onSurfaceVariant = AuroraDarkOnSurfaceVariant,
    outline = AuroraDarkOutline,
    outlineVariant = AuroraDarkOutlineVariant,
    background = AuroraDarkSurface,
    onBackground = AuroraDarkOnSurface,
)

private val AuroraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AuroraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AuroraTypography,
        shapes = AuroraShapes,
        content = content,
    )
}
