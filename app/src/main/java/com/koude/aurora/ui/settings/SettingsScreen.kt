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

@Composable
fun SettingsScreen(
    proxyEnabled: Boolean,
    onOpenHome: () -> Unit,
    onOpenProxy: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenMetaFeature: () -> Unit,
    onOpenOverride: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            AuroraBottomNavigation(
                selected = AuroraDestination.Settings,
                proxyEnabled = proxyEnabled,
                onNavigate = { destination ->
                    when (destination) {
                        AuroraDestination.Home -> onOpenHome()
                        AuroraDestination.Proxy -> onOpenProxy()
                        AuroraDestination.Profiles -> onOpenProfiles()
                        AuroraDestination.Settings -> Unit
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = padding.calculateTopPadding() + 18.dp,
                end = 20.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
            item { Spacer(Modifier.height(12.dp)) }

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
    Text(
        text = text,
        modifier = modifier.padding(start = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
