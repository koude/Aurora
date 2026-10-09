package com.koude.aurora.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
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
    tertiary = AuroraTertiary,
    onTertiary = AuroraOnTertiary,
    tertiaryContainer = AuroraTertiaryContainer,
    onTertiaryContainer = AuroraOnTertiaryContainer,
    error = AuroraError,
    onError = AuroraOnError,
    errorContainer = AuroraErrorContainer,
    onErrorContainer = AuroraOnErrorContainer,
    surface = AuroraSurface,
    onSurface = AuroraOnSurface,
    surfaceVariant = AuroraSurfaceVariant,
    onSurfaceVariant = AuroraOnSurfaceVariant,
    surfaceDim = AuroraSurfaceDim,
    surfaceBright = AuroraSurfaceBright,
    surfaceContainerLowest = AuroraSurfaceContainerLowest,
    surfaceContainerLow = AuroraSurfaceContainerLow,
    surfaceContainer = AuroraSurfaceContainer,
    surfaceContainerHigh = AuroraSurfaceContainerHigh,
    surfaceContainerHighest = AuroraSurfaceContainerHighest,
    outline = AuroraOutline,
    outlineVariant = AuroraOutlineVariant,
    background = AuroraSurface,
    onBackground = AuroraOnSurface,
    inverseSurface = AuroraInverseSurface,
    inverseOnSurface = AuroraInverseOnSurface,
    inversePrimary = AuroraDarkPrimary,
    surfaceTint = AuroraPrimary,
    scrim = Color.Black,
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
    tertiary = AuroraDarkTertiary,
    onTertiary = AuroraDarkOnTertiary,
    tertiaryContainer = AuroraDarkTertiaryContainer,
    onTertiaryContainer = AuroraDarkOnTertiaryContainer,
    error = AuroraDarkError,
    onError = AuroraDarkOnError,
    errorContainer = AuroraDarkErrorContainer,
    onErrorContainer = AuroraDarkOnErrorContainer,
    surface = AuroraDarkSurface,
    onSurface = AuroraDarkOnSurface,
    surfaceVariant = AuroraDarkSurfaceVariant,
    onSurfaceVariant = AuroraDarkOnSurfaceVariant,
    surfaceDim = AuroraDarkSurfaceDim,
    surfaceBright = AuroraDarkSurfaceBright,
    surfaceContainerLowest = AuroraDarkSurfaceContainerLowest,
    surfaceContainerLow = AuroraDarkSurfaceContainerLow,
    surfaceContainer = AuroraDarkSurfaceContainer,
    surfaceContainerHigh = AuroraDarkSurfaceContainerHigh,
    surfaceContainerHighest = AuroraDarkSurfaceContainerHighest,
    outline = AuroraDarkOutline,
    outlineVariant = AuroraDarkOutlineVariant,
    background = AuroraDarkSurface,
    onBackground = AuroraDarkOnSurface,
    inverseSurface = AuroraDarkInverseSurface,
    inverseOnSurface = AuroraDarkInverseOnSurface,
    inversePrimary = AuroraPrimary,
    surfaceTint = AuroraDarkPrimary,
    scrim = Color.Black,
)

private val AuroraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalAuroraDarkTheme = staticCompositionLocalOf { false }

@Composable
fun AuroraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    if (!view.isInEditMode) {
        SideEffect {
            // Match the status bar to the selected Aurora or wallpaper-derived surface.
            context.findActivity()?.window?.statusBarColor = colors.surface.toArgb()
        }
    }

    CompositionLocalProvider(LocalAuroraDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colors,
            typography = AuroraTypography,
            shapes = AuroraShapes,
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
                content()
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
