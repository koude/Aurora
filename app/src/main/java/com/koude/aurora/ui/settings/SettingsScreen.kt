package com.koude.aurora.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.components.AuroraBottomNavigation
import com.koude.aurora.ui.components.AuroraDestination
import com.koude.aurora.ui.components.AuroraPageHeader
import com.koude.aurora.ui.components.AuroraPageSpacing
import com.koude.aurora.ui.components.AuroraSectionTitle
import com.koude.aurora.ui.components.AuroraCardStyle

@Composable
fun SettingsScreen(
    proxyEnabled: Boolean,
    onOpenHome: () -> Unit,
    onOpenProxy: () -> Unit,
    onOpenConnections: () -> Unit = {},
    onOpenProfiles: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenMetaFeature: () -> Unit,
    onOpenOverride: () -> Unit,
    showBottomNavigation: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = if (showBottomNavigation) ScaffoldDefaults.contentWindowInsets
        else WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        bottomBar = if (showBottomNavigation) {{
            AuroraBottomNavigation(
                selected = AuroraDestination.Settings,
                proxyEnabled = proxyEnabled,
                onNavigate = { destination ->
                    when (destination) {
                        AuroraDestination.Home -> onOpenHome()
                        AuroraDestination.Proxy -> onOpenProxy()
                        AuroraDestination.Connections -> onOpenConnections()
                        AuroraDestination.Settings -> Unit
                    }
                },
            )
        }} else {{ }},
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AuroraPageSpacing.Horizontal,
                top = padding.calculateTopPadding() + AuroraPageSpacing.Top,
                end = AuroraPageSpacing.Horizontal,
                bottom = padding.calculateBottomPadding() + AuroraPageSpacing.Bottom,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AuroraPageHeader(title = "设置")
            }
            item { Spacer(Modifier.height(12.dp)) }

            item { SettingsSectionTitle("管理") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Default.Menu, "配置与订阅", onOpenProfiles)
                }
            }

            item { SettingsSectionTitle("网络") }
            item {
                SettingsGroup {
                    SettingsRow(Icons.AutoMirrored.Filled.List, "VPN 与路由", onOpenNetwork)
                    SettingsDivider()
                    SettingsRow(Icons.Default.Menu, "DNS", onOpenNetwork)
                    SettingsDivider()
                    SettingsRow(Icons.Default.Settings, "应用访问控制", onOpenApp)
                }
            }

            item { SettingsSectionTitle("应用", Modifier.padding(top = 10.dp)) }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Default.Settings, "应用设置", onOpenApp)
                }
            }

            item { SettingsSectionTitle("高级", Modifier.padding(top = 10.dp)) }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Default.Refresh, "Mihomo 功能", onOpenMetaFeature)
                    SettingsDivider()
                    SettingsRow(Icons.Default.Edit, "配置覆写", onOpenOverride)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    AuroraSectionTitle(text, modifier.padding(start = 8.dp, bottom = 2.dp))
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        shape = AuroraCardStyle.groupShape(),
        colors = CardDefaults.cardColors(
            containerColor = AuroraCardStyle.groupColor(),
        ),
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        ChevronIcon()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ChevronIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
        val stroke = 1.8.dp.toPx()
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .25f), androidx.compose.ui.geometry.Offset(size.width * .66f, size.height * .5f), stroke)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .66f, size.height * .5f), androidx.compose.ui.geometry.Offset(size.width * .38f, size.height * .75f), stroke)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    AuroraTheme {
        SettingsScreen(
            proxyEnabled = true,
            onOpenHome = {},
            onOpenProxy = {},
            onOpenProfiles = {},
            onOpenNetwork = {},
            onOpenApp = {},
            onOpenMetaFeature = {},
            onOpenOverride = {},
        )
    }
}
