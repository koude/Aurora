package com.koude.aurora.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class AuroraDestination {
    Home,
    Proxy,
    Connections,
    Settings,
}

@Composable
fun AuroraBottomNavigation(
    selected: AuroraDestination,
    onNavigate: (AuroraDestination) -> Unit,
    proxyEnabled: Boolean = true,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        DestinationItem("首页", Icons.Default.Home, AuroraDestination.Home, selected, true, onNavigate)
        DestinationItem("代理", Icons.AutoMirrored.Filled.List, AuroraDestination.Proxy, selected, proxyEnabled, onNavigate)
        DestinationItem("连接", Icons.Default.Share, AuroraDestination.Connections, selected, true, onNavigate)
        DestinationItem("设置", Icons.Default.Settings, AuroraDestination.Settings, selected, true, onNavigate)
    }
}

@Composable
private fun RowScope.DestinationItem(
    label: String,
    icon: ImageVector,
    destination: AuroraDestination,
    selected: AuroraDestination,
    enabled: Boolean,
    onNavigate: (AuroraDestination) -> Unit,
) {
    NavigationBarItem(
        selected = destination == selected,
        onClick = { onNavigate(destination) },
        enabled = enabled,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
}
