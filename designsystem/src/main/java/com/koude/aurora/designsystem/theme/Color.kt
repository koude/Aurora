package com.koude.aurora.designsystem.theme

import androidx.compose.ui.graphics.Color

// Default Aurora scheme. Wallpaper-derived colors are an explicit opt-in on Android 12+.
// Primary: Aurora teal; secondary: restrained blue; tertiary: warm amber.
internal val AuroraPrimary = Color(0xFF006F73)
internal val AuroraOnPrimary = Color(0xFFFFFFFF)
internal val AuroraPrimaryContainer = Color(0xFFA0F1F0)
internal val AuroraOnPrimaryContainer = Color(0xFF002021)
internal val AuroraSecondary = Color(0xFF4B6078)
internal val AuroraOnSecondary = Color(0xFFFFFFFF)
internal val AuroraSecondaryContainer = Color(0xFFD3E4FF)
internal val AuroraOnSecondaryContainer = Color(0xFF0D1E31)
internal val AuroraTertiary = Color(0xFF7A5800)
internal val AuroraOnTertiary = Color(0xFFFFFFFF)
internal val AuroraTertiaryContainer = Color(0xFFFFE08B)
internal val AuroraOnTertiaryContainer = Color(0xFF291C00)
internal val AuroraError = Color(0xFFBA1A1A)
internal val AuroraOnError = Color(0xFFFFFFFF)
internal val AuroraErrorContainer = Color(0xFFFFDAD6)
internal val AuroraOnErrorContainer = Color(0xFF410002)
internal val AuroraSurface = Color(0xFFF7FAF9)
internal val AuroraOnSurface = Color(0xFF191D1C)
internal val AuroraSurfaceVariant = Color(0xFFDAE5E2)
internal val AuroraOnSurfaceVariant = Color(0xFF404A48)
internal val AuroraSurfaceDim = Color(0xFFD7DCDA)
internal val AuroraSurfaceBright = Color(0xFFF7FAF9)
internal val AuroraSurfaceContainerLowest = Color(0xFFFFFFFF)
internal val AuroraSurfaceContainerLow = Color(0xFFF1F5F3)
internal val AuroraSurfaceContainer = Color(0xFFEBF0EE)
internal val AuroraSurfaceContainerHigh = Color(0xFFE5EAE8)
internal val AuroraSurfaceContainerHighest = Color(0xFFDFE5E2)
internal val AuroraOutline = Color(0xFF6F7977)
internal val AuroraOutlineVariant = Color(0xFFBEC9C6)
internal val AuroraInverseSurface = Color(0xFF2E3533)
internal val AuroraInverseOnSurface = Color(0xFFEDF4F1)

internal val AuroraDarkPrimary = Color(0xFF86D3D5)
internal val AuroraDarkOnPrimary = Color(0xFF003739)
internal val AuroraDarkPrimaryContainer = Color(0xFF005054)
internal val AuroraDarkOnPrimaryContainer = Color(0xFFA0F1F0)
internal val AuroraDarkSecondary = Color(0xFFB4C8E0)
internal val AuroraDarkOnSecondary = Color(0xFF203147)
internal val AuroraDarkSecondaryContainer = Color(0xFF36485F)
internal val AuroraDarkOnSecondaryContainer = Color(0xFFD3E4FF)
internal val AuroraDarkTertiary = Color(0xFFF4C94A)
internal val AuroraDarkOnTertiary = Color(0xFF403000)
internal val AuroraDarkTertiaryContainer = Color(0xFF5C4300)
internal val AuroraDarkOnTertiaryContainer = Color(0xFFFFE08B)
internal val AuroraDarkError = Color(0xFFFFB4AB)
internal val AuroraDarkOnError = Color(0xFF690005)
internal val AuroraDarkErrorContainer = Color(0xFF93000A)
internal val AuroraDarkOnErrorContainer = Color(0xFFFFDAD6)
internal val AuroraDarkSurface = Color(0xFF101515)
internal val AuroraDarkOnSurface = Color(0xFFDFE5E2)
internal val AuroraDarkSurfaceVariant = Color(0xFF404A48)
internal val AuroraDarkOnSurfaceVariant = Color(0xFFBEC9C6)
internal val AuroraDarkSurfaceDim = Color(0xFF101515)
internal val AuroraDarkSurfaceBright = Color(0xFF363F3D)
internal val AuroraDarkSurfaceContainerLowest = Color(0xFF0B1010)
internal val AuroraDarkSurfaceContainerLow = Color(0xFF191F1E)
internal val AuroraDarkSurfaceContainer = Color(0xFF1D2524)
internal val AuroraDarkSurfaceContainerHigh = Color(0xFF28302E)
internal val AuroraDarkSurfaceContainerHighest = Color(0xFF323B39)
internal val AuroraDarkOutline = Color(0xFF899390)
internal val AuroraDarkOutlineVariant = Color(0xFF404A48)
internal val AuroraDarkInverseSurface = Color(0xFFDFE5E2)
internal val AuroraDarkInverseOnSurface = Color(0xFF2E3533)

// These are semantic outlet colors, not Material color roles. They must stay
// recognizable even when the user opts into wallpaper-derived Material colors.
data class AuroraModeColors(val container: Color, val onContainer: Color, val accent: Color)

object AuroraModePalette {
    val ruleLight = AuroraModeColors(Color(0xFFD8F4E8), Color(0xFF123E35), Color(0xFF006B55))
    val globalLight = AuroraModeColors(Color(0xFFD6E7FF), Color(0xFF123D69), Color(0xFF225CA6))
    val directLight = AuroraModeColors(Color(0xFFFFF0B8), Color(0xFF554000), Color(0xFF886400))

    val ruleDark = AuroraModeColors(Color(0xFF164B3F), Color(0xFFC1F0DF), Color(0xFF82DDB9))
    val globalDark = AuroraModeColors(Color(0xFF173E71), Color(0xFFD8E7FF), Color(0xFFA9C8FA))
    val directDark = AuroraModeColors(Color(0xFF5B4700), Color(0xFFFFE89E), Color(0xFFE9C55C))
}

// Connection colors describe the current state, not the action on tap.
object AuroraConnectionStatusColors {
    val connected = Color(0xFF2E7D32)
    val onConnected = Color.White
}
