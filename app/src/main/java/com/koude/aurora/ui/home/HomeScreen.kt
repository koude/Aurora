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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.model.RoutePreview
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
    val testingLatencySites: Set<WebsiteLatencySite> = emptySet(),
)

enum class WebsiteLatencySite {
    Apple,
    GitHub,
    YouTube,
    Google,
}

data class RouteTestUiState(
    val isOpen: Boolean = false,
    val target: String = "github.com",
    val isTesting: Boolean = false,
    val errorMessage: String? = null,
    val preview: RoutePreview? = null,
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleConnection: () -> Unit,
    onModeSelected: (TunnelState.Mode) -> Unit,
    onTestLatency: () -> Unit,
    onTestSiteLatency: (WebsiteLatencySite) -> Unit,
    routeTestState: RouteTestUiState,
    onOpenConnections: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenRouteTest: () -> Unit,
    onDismissRouteTest: () -> Unit,
    onRouteTargetChange: (String) -> Unit,
    onSubmitRouteTest: () -> Unit,
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
                        AuroraDestination.Connections -> onOpenConnections()
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

            LatencyCard(
                state = state,
                onTestLatency = onTestLatency,
                onTestSiteLatency = onTestSiteLatency,
            )

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

    if (routeTestState.isOpen) {
        RouteTestSheet(
            state = routeTestState,
            onDismiss = onDismissRouteTest,
            onTargetChange = onRouteTargetChange,
            onSubmit = onSubmitRouteTest,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteTestSheet(
    state: RouteTestUiState,
    onDismiss: () -> Unit,
    onTargetChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("路由测试", style = MaterialTheme.typography.headlineSmall)
            Text(
                "预览当前配置的匹配结果，不会连接目标网站。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = state.target,
                    onValueChange = onTargetChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("网站或域名") },
                    singleLine = true,
                    enabled = !state.isTesting,
                    isError = state.errorMessage != null,
                    supportingText = state.errorMessage?.let { message ->
                        { Text(message, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                )
                FilledTonalButton(
                    onClick = onSubmit,
                    enabled = !state.isTesting,
                    modifier = Modifier.padding(top = 8.dp).height(56.dp),
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("测试")
                    }
                }
            }

            if (state.isTesting) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("正在匹配当前规则…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            state.preview?.takeIf { it.error == null }?.let { preview ->
                RoutePreviewCard(preview)
            }
        }
    }
}

@Composable
private fun RoutePreviewCard(preview: RoutePreview) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                RoutePreviewRow("目标", preview.resolvedIp?.let { "${preview.target} · $it" } ?: preview.target)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                RoutePreviewRow("命中规则", preview.rule)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                RoutePreviewRow("策略组", preview.policy)
            }
        }
        Surface(
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text(
                    "实际出口",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    preview.outbound.ifBlank { "—" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun RoutePreviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.width(68.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value.ifBlank { "—" },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
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
private fun LatencyCard(
    state: HomeUiState,
    onTestLatency: () -> Unit,
    onTestSiteLatency: (WebsiteLatencySite) -> Unit,
) {
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
                LatencyCell(
                    "Apple", state.appleLatency, state.testingLatencySites.contains(WebsiteLatencySite.Apple),
                    Modifier.weight(1f), onClick = { onTestSiteLatency(WebsiteLatencySite.Apple) },
                )
                LatencyCell(
                    "GitHub", state.githubLatency, state.testingLatencySites.contains(WebsiteLatencySite.GitHub),
                    Modifier.weight(1f), onClick = { onTestSiteLatency(WebsiteLatencySite.GitHub) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LatencyCell(
                    "YouTube", state.youtubeLatency, state.testingLatencySites.contains(WebsiteLatencySite.YouTube),
                    Modifier.weight(1f), onClick = { onTestSiteLatency(WebsiteLatencySite.YouTube) },
                )
                LatencyCell(
                    "Google", state.googleLatency, state.testingLatencySites.contains(WebsiteLatencySite.Google),
                    Modifier.weight(1f), onClick = { onTestSiteLatency(WebsiteLatencySite.Google) },
                )
            }
        }
    }
}

@Composable
private fun LatencyCell(
    label: String,
    value: String,
    testing: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
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
            if (testing) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
            onTestSiteLatency = {},
            routeTestState = RouteTestUiState(),
            onOpenConnections = {},
            onOpenLogs = {},
            onOpenRouteTest = {},
            onDismissRouteTest = {},
            onRouteTargetChange = {},
            onSubmitRouteTest = {},
            onOpenDns = {},
            onOpenProfiles = {},
            onOpenProxy = {},
            onOpenSettings = {},
        )
    }
}
