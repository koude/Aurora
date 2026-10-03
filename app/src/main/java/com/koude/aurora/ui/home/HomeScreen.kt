package com.koude.aurora.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.TunnelState
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.components.AuroraBottomNavigation
import com.koude.aurora.ui.components.AuroraDestination

data class HomeUiState(
    val running: Boolean = false,
    val mode: TunnelState.Mode = TunnelState.Mode.Rule,
    val uploadSpeed: String = "-- B/s",
    val downloadSpeed: String = "-- B/s",
    val profileName: String? = null,
    val latencyTesting: Boolean = false,
    val appleLatency: String = "-- ms",
    val githubLatency: String = "-- ms",
    val youtubeLatency: String = "-- ms",
    val googleLatency: String = "-- ms",
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleConnection: () -> Unit,
    onModeSelected: (TunnelState.Mode) -> Unit,
    onTestLatency: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenRouteTest: () -> Unit,
    onOpenDns: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenProxy: () -> Unit,
    onOpenSettings: () -> Unit,
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
                selected = AuroraDestination.Home,
                proxyEnabled = state.running,
                onNavigate = { destination ->
                    when (destination) {
                        AuroraDestination.Home -> Unit
                        AuroraDestination.Proxy -> onOpenProxy()
                        AuroraDestination.Profiles -> onOpenProfiles()
                        AuroraDestination.Settings -> onOpenSettings()
                    }
                },
            )
        }} else {{ }},
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    top = padding.calculateTopPadding() + 18.dp,
                    end = 20.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = "Aurora",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )

            ConnectionControls(
                state = state,
                onModeSelected = onModeSelected,
                onToggleConnection = onToggleConnection,
            )

            LatencyCard(state = state, onTestLatency = onTestLatency)

            QuickTools(
                onOpenConnections = onOpenConnections,
                onOpenLogs = onOpenLogs,
                onOpenRouteTest = onOpenRouteTest,
                onOpenDns = onOpenDns,
            )

            ProfileCard(
                profileName = state.profileName,
                onOpenProfiles = onOpenProfiles,
            )
        }
    }
}

@Composable
private fun ConnectionControls(
    state: HomeUiState,
    onModeSelected: (TunnelState.Mode) -> Unit,
    onToggleConnection: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TrafficValue("下载", state.downloadSpeed)
            TrafficValue("上传", state.uploadSpeed)
        }

        ModeMenu(
            mode = state.mode,
            enabled = state.running,
            onModeSelected = onModeSelected,
        )

        Surface(
            modifier = Modifier
                .size(64.dp)
                .clickable(onClick = onToggleConnection),
            shape = CircleShape,
            color = if (state.running) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (state.running) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 3.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                PowerGlyph(Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun TrafficValue(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ModeMenu(
    mode: TunnelState.Mode,
    enabled: Boolean,
    onModeSelected: (TunnelState.Mode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (mode) {
        TunnelState.Mode.Global -> "全局"
        TunnelState.Mode.Direct -> "直连"
        else -> "规则"
    }

    Box {
        FilledTonalButton(
            onClick = { expanded = true },
            enabled = enabled,
            contentPadding = PaddingValues(start = 18.dp, end = 12.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(label)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                TunnelState.Mode.Rule to "规则",
                TunnelState.Mode.Global to "全局",
                TunnelState.Mode.Direct to "直连",
            ).forEach { (itemMode, itemLabel) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel,
                            fontWeight = if (mode == itemMode) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        expanded = false
                        onModeSelected(itemMode)
                    },
                )
            }
        }
    }
}

@Composable
private fun LatencyCard(state: HomeUiState, onTestLatency: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "网站延迟",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
                TextButton(onClick = onTestLatency, enabled = !state.latencyTesting) {
                    if (state.latencyTesting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Text(if (state.latencyTesting) "检测中" else "全部检测")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LatencyCell("Apple", state.appleLatency, Modifier.weight(1f))
                LatencyCell("GitHub", state.githubLatency, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LatencyCell("YouTube", state.youtubeLatency, Modifier.weight(1f))
                LatencyCell("Google", state.googleLatency, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LatencyCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun QuickTools(
    onOpenConnections: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenRouteTest: () -> Unit,
    onOpenDns: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "快捷工具",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuickTool(Icons.AutoMirrored.Filled.List, "连接", onOpenConnections)
            QuickTool(Icons.Default.MoreVert, "日志", onOpenLogs)
            QuickTool(Icons.Default.Refresh, "路由测试", onOpenRouteTest)
            QuickTool(Icons.Default.Settings, "DNS", onOpenDns)
        }
    }
}

@Composable
private fun QuickTool(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(27.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ProfileCard(profileName: String?, onOpenProfiles: () -> Unit) {
    Card(
        onClick = onOpenProfiles,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profileName ?: "未选择配置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "当前配置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onOpenProfiles) {
                Icon(Icons.Default.Menu, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("配置")
            }
        }
    }
}

@Composable
private fun PowerGlyph(modifier: Modifier = Modifier) {
    val color = androidx.compose.material3.LocalContentColor.current
    androidx.compose.foundation.Canvas(modifier) {
        val stroke = 2.6.dp.toPx()
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * .08f),
            end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * .48f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawArc(
            color = color,
            startAngle = -48f,
            sweepAngle = 276f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * .14f, size.height * .20f),
            size = androidx.compose.ui.geometry.Size(size.width * .72f, size.height * .72f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeScreenPreview() {
    AuroraTheme {
        HomeScreen(
            state = HomeUiState(
                running = true,
                uploadSpeed = "128 KiB/s",
                downloadSpeed = "2.4 MiB/s",
                profileName = "日常使用",
                githubLatency = "86 ms",
            ),
            onToggleConnection = {},
            onModeSelected = {},
            onTestLatency = {},
            onOpenConnections = {},
            onOpenLogs = {},
            onOpenRouteTest = {},
            onOpenDns = {},
            onOpenProfiles = {},
            onOpenProxy = {},
            onOpenSettings = {},
        )
    }
}
