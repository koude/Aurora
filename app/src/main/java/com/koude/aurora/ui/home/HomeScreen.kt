package com.koude.aurora.ui.home

import com.github.kr328.clash.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.model.RoutePreview
import com.koude.aurora.model.WebsiteLatencySite
import com.koude.aurora.model.ProfileSummary
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.components.AuroraBottomNavigation
import com.koude.aurora.ui.components.AuroraDestination
import com.koude.aurora.ui.components.AuroraPageHeader
import com.koude.aurora.ui.components.AuroraPageSpacing
import com.koude.aurora.ui.components.AuroraSectionTitle
import com.koude.aurora.ui.components.AuroraCardStyle
import java.util.UUID

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
    profiles: List<ProfileSummary> = emptyList(),
    onSelectProfile: (UUID) -> Unit = {},
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
                    start = AuroraPageSpacing.Horizontal,
                    top = padding.calculateTopPadding() + AuroraPageSpacing.Top,
                    end = AuroraPageSpacing.Horizontal,
                    bottom = padding.calculateBottomPadding() + AuroraPageSpacing.Bottom,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AuroraPageHeader(title = "Aurora")

            ConnectionControls(
                state = state,
                profiles = profiles,
                onOpenProfiles = onOpenProfiles,
                onSelectProfile = onSelectProfile,
                onModeSelected = onModeSelected,
                onToggleConnection = onToggleConnection,
            )

            TrafficLatencyCard(
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
    profiles: List<ProfileSummary>,
    onOpenProfiles: () -> Unit,
    onSelectProfile: (UUID) -> Unit,
    onModeSelected: (TunnelState.Mode) -> Unit,
    onToggleConnection: () -> Unit,
) {
    var profileExpanded by remember { mutableStateOf(false) }
    val modeWidth by animateDpAsState(
        targetValue = if (profileExpanded) 52.dp else 102.dp,
        label = "Mode control width",
    )
    val powerWidth by animateDpAsState(
        targetValue = if (profileExpanded) 58.dp else 78.dp,
        label = "Power control width",
    )
    Card(
        shape = AuroraCardStyle.groupShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileMenu(
                profileName = state.profileName,
                profiles = profiles,
                onSelectProfile = onSelectProfile,
                onOpenProfiles = onOpenProfiles,
                expanded = profileExpanded,
                onExpandedChange = { profileExpanded = it },
                modifier = Modifier.weight(1f),
            )
            ModeMenu(
                mode = state.mode,
                enabled = state.running,
                onModeSelected = onModeSelected,
                compact = profileExpanded,
                modifier = Modifier.width(modeWidth),
            )
            Button(
                onClick = onToggleConnection,
                modifier = Modifier.width(powerWidth).height(52.dp).semantics {
                    contentDescription = if (state.running) "停止" else "启动"
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.running) Color(0xFFC62828) else Color(0xFF2E7D32),
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                PowerGlyph(Modifier.size(24.dp))
                AnimatedVisibility(
                    visible = !profileExpanded,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.running) "停止" else "启动")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMenu(
    profileName: String?,
    profiles: List<ProfileSummary>,
    onSelectProfile: (UUID) -> Unit,
    onOpenProfiles: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectableProfiles = profiles.filter(ProfileSummary::imported)
    Box(modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onExpandedChange(true) },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = profileName ?: profiles.firstOrNull(ProfileSummary::active)?.name ?: "未选择配置",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "切换配置",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            selectableProfiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = if (profile.active) {
                        { Icon(Icons.Default.Check, contentDescription = "当前配置") }
                    } else null,
                    onClick = {
                        onExpandedChange(false)
                        if (!profile.active) onSelectProfile(profile.id)
                    },
                )
            }
            if (selectableProfiles.isNotEmpty()) HorizontalDivider()
            DropdownMenuItem(
                text = { Text("管理配置") },
                onClick = { onExpandedChange(false); onOpenProfiles() },
            )
        }
    }
}

@Composable
private fun TrafficValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
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
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else .55f)
                .clickable(enabled = enabled) { expanded = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(modeIcon(mode)),
                    contentDescription = "切换当前出口",
                    modifier = Modifier.size(28.dp),
                    tint = modeIconTint(mode),
                )
                AnimatedVisibility(
                    visible = !compact,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    Text(
                        modeLabel(mode),
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(TunnelState.Mode.Rule, TunnelState.Mode.Global, TunnelState.Mode.Direct).forEach { itemMode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = modeLabel(itemMode),
                            fontWeight = if (mode == itemMode) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(modeIcon(itemMode)),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = modeIconTint(itemMode),
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

private fun modeLabel(mode: TunnelState.Mode): String = when (mode) {
    TunnelState.Mode.Rule -> "规则"
    TunnelState.Mode.Global -> "全局"
    TunnelState.Mode.Direct -> "直连"
    TunnelState.Mode.Script -> "脚本"
}

private fun modeIcon(mode: TunnelState.Mode): Int = when (mode) {
    TunnelState.Mode.Rule, TunnelState.Mode.Script -> R.drawable.ic_mode_rule
    TunnelState.Mode.Global -> R.drawable.ic_mode_global
    TunnelState.Mode.Direct -> R.drawable.ic_mode_direct
}

@Composable
private fun modeIconTint(mode: TunnelState.Mode): Color = when (mode) {
    TunnelState.Mode.Rule, TunnelState.Mode.Script -> MaterialTheme.colorScheme.onSurface
    TunnelState.Mode.Global -> Color(0xFF1976D2)
    TunnelState.Mode.Direct -> Color(0xFFD99B00)
}

@Composable
private fun TrafficLatencyCard(
    state: HomeUiState,
    onTestLatency: () -> Unit,
    onTestSiteLatency: (WebsiteLatencySite) -> Unit,
) {
    Card(
        shape = AuroraCardStyle.groupShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TrafficValue("下载速度", state.downloadSpeed, Modifier.weight(1f))
                TrafficValue("上传速度", state.uploadSpeed, Modifier.weight(1f))
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 3.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "网站延迟",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(
                    onClick = onTestLatency,
                    enabled = !state.latencyTesting,
                    modifier = Modifier.size(40.dp).semantics {
                        contentDescription = if (state.latencyTesting) "检测中" else "检测全部网站"
                    },
                ) {
                    if (state.latencyTesting) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LatencyCell(
                    "Apple", R.drawable.ic_site_apple, state.appleLatency, state.testingLatencySites.contains(WebsiteLatencySite.Apple),
                    Modifier.weight(1f), onClick = { onTestSiteLatency(WebsiteLatencySite.Apple) },
                )
                LatencyCell(
                    "GitHub", R.drawable.ic_site_github, state.githubLatency, state.testingLatencySites.contains(WebsiteLatencySite.GitHub),
                    Modifier.weight(1f), onClick = { onTestSiteLatency(WebsiteLatencySite.GitHub) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LatencyCell(
                    "YouTube", R.drawable.ic_site_youtube, state.youtubeLatency, state.testingLatencySites.contains(WebsiteLatencySite.YouTube),
                    Modifier.weight(1f), onClick = { onTestSiteLatency(WebsiteLatencySite.YouTube) },
                )
                LatencyCell(
                    "Google", R.drawable.ic_site_google, state.googleLatency, state.testingLatencySites.contains(WebsiteLatencySite.Google),
                    Modifier.weight(1f), onClick = { onTestSiteLatency(WebsiteLatencySite.Google) },
                )
            }
        }
    }
}

@Composable
private fun LatencyCell(
    label: String,
    iconRes: Int,
    value: String,
    testing: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        shape = AuroraCardStyle.itemShape(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.padding(3.dp),
                        tint = Color.Unspecified,
                    )
                }
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (testing) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
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
    Card(
        shape = AuroraCardStyle.groupShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AuroraCardStyle.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuroraSectionTitle("快捷工具", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
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
