package com.koude.aurora.ui.proxy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxySort
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.components.AuroraPageHeader
import com.koude.aurora.ui.components.AuroraPageSpacing
import com.koude.aurora.ui.components.AuroraCardStyle

data class ProxyGroupUiState(
    val name: String,
    val type: String = "Selector",
    val selectedProxy: String = "",
    val selectable: Boolean = false,
    val delayTested: Boolean = false,
    val proxies: List<Proxy> = emptyList(),
    val nestedRoutes: Map<String, ProxyRouteUiState> = emptyMap(),
    val testing: Boolean = false,
    val activeDelay: Int = 65535,
    val activeDelayTested: Boolean = false,
    val selectingProxy: String? = null,
)

data class ProxyRouteUiState(
    val names: List<String>,
    val delay: Int,
)

data class ProxyUiState(
    val loading: Boolean = false,
    val serviceRunning: Boolean = false,
    val groups: List<ProxyGroupUiState> = emptyList(),
    val selectedGroupIndex: Int = 0,
    val expandedGroups: Map<String, Boolean> = emptyMap(),
    val errorMessage: String? = null,
    val sort: ProxySort = ProxySort.Default,
    val hideUnselectableGroups: Boolean = false,
    val activeEndpointsTesting: Boolean = false,
)

@Composable
fun ProxyScreen(
    state: ProxyUiState,
    onSelectGroup: (Int) -> Unit,
    onGroupExpandedChange: (String, Boolean) -> Unit,
    onSelectProxy: (groupIndex: Int, proxyName: String) -> Unit,
    onRefresh: (expandedGroupIndex: Int?) -> Unit,
    onSortChanged: (ProxySort) -> Unit,
    onHideUnselectableChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { padding ->
        when {
            !state.serviceRunning -> ProxyMessage(
                title = "代理服务未连接",
                message = "从首页开启 Aurora 后即可选择节点",
                modifier = Modifier.padding(padding),
            )
            state.loading && state.groups.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            state.groups.isEmpty() -> ProxyMessage(
                title = "没有可用的代理组",
                message = state.errorMessage ?: "请检查当前配置",
                modifier = Modifier.padding(padding),
            )
            else -> ProxyContent(
                state = state,
                onSelectGroup = onSelectGroup,
                onGroupExpandedChange = onGroupExpandedChange,
                onSelectProxy = onSelectProxy,
                onRefresh = onRefresh,
                onSortChanged = onSortChanged,
                onHideUnselectableChanged = onHideUnselectableChanged,
                contentPadding = PaddingValues(
                    start = AuroraPageSpacing.Horizontal,
                    top = padding.calculateTopPadding() + AuroraPageSpacing.Top,
                    end = AuroraPageSpacing.Horizontal,
                    bottom = padding.calculateBottomPadding() + AuroraPageSpacing.Bottom,
                ),
            )
        }
    }
}

@Composable
private fun ProxyContent(
    state: ProxyUiState,
    onSelectGroup: (Int) -> Unit,
    onGroupExpandedChange: (String, Boolean) -> Unit,
    onSelectProxy: (Int, String) -> Unit,
    onRefresh: (expandedGroupIndex: Int?) -> Unit,
    onSortChanged: (ProxySort) -> Unit,
    onHideUnselectableChanged: (Boolean) -> Unit,
    contentPadding: PaddingValues,
) {
    var settingsVisible by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim().lowercase()
    val filteredGroups = state.groups.filter { group ->
        normalizedQuery.isEmpty() || group.name.lowercase().contains(normalizedQuery) ||
            group.proxies.any { proxy ->
                val route = group.nestedRoutes[proxy.name]?.names.orEmpty().joinToString(" ")
                listOf(proxy.name, proxy.title, proxy.subtitle, proxy.type, route)
                    .any { normalizedQuery in it.lowercase() }
            }
    }

    Column(Modifier.fillMaxSize()) {
        AuroraPageHeader(
            title = "代理",
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AuroraPageSpacing.Horizontal,
                    top = contentPadding.calculateTopPadding(),
                    end = 12.dp,
                    bottom = 8.dp,
                ),
            actions = {
                IconButton(
                    onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) query = ""
                    },
                ) {
                    Icon(
                        if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (searchVisible) "关闭搜索" else "搜索策略组或节点",
                    )
                }
                val expandedGroupIndex = state.groups.indexOfFirst { state.expandedGroups[it.name] == true }
                    .takeIf { it >= 0 }
                val expandedGroupTesting = expandedGroupIndex?.let { state.groups[it].testing } == true
                IconButton(
                    onClick = { onRefresh(expandedGroupIndex) },
                    enabled = !state.loading && !state.activeEndpointsTesting && !expandedGroupTesting,
                ) {
                    if (state.loading || state.activeEndpointsTesting || expandedGroupTesting) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = "测速")
                }
                IconButton(onClick = { settingsVisible = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "代理设置")
                }
            },
        )

        if (searchVisible) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = AuroraPageSpacing.Horizontal, vertical = 4.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "清除搜索") } }
                } else null,
                placeholder = { Text("搜索策略组或节点") },
                shape = MaterialTheme.shapes.large,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                top = 8.dp,
                end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (filteredGroups.isEmpty()) {
                item {
                    Text(
                        text = "没有匹配的策略组或节点",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(filteredGroups, key = ProxyGroupUiState::name) { item ->
                    val index = state.groups.indexOfFirst { it.name == item.name }
                    val expanded = if (normalizedQuery.isNotEmpty()) true
                    else state.expandedGroups[item.name] ?: false
                    ProxyGroupCard(
                        group = item,
                        expanded = expanded,
                        searchQuery = normalizedQuery,
                        onExpand = {
                            val next = !expanded
                            onGroupExpandedChange(item.name, next)
                            if (next) onSelectGroup(index)
                        },
                        onSelectProxy = { name -> onSelectProxy(index, name) },
                    )
                }
            }
        }
    }

    if (settingsVisible) {
        ProxySettingsSheet(
            state = state,
            onDismiss = { settingsVisible = false },
            onSortChanged = onSortChanged,
            onHideUnselectableChanged = onHideUnselectableChanged,
        )
    }
}

@Composable
private fun ProxyGroupCard(
    group: ProxyGroupUiState,
    expanded: Boolean,
    searchQuery: String,
    onExpand: () -> Unit,
    onSelectProxy: (String) -> Unit,
) {
    val visibleProxies = if (searchQuery.isBlank() || group.name.contains(searchQuery, ignoreCase = true)) {
        group.proxies
    } else {
        group.proxies.filter { proxy ->
            val route = group.nestedRoutes[proxy.name]?.names.orEmpty().joinToString(" ")
            listOf(proxy.name, proxy.title, proxy.subtitle, proxy.type, route)
                .any { searchQuery in it.lowercase() }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AuroraCardStyle.groupShape())
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onExpand)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val current = group.nestedRoutes[group.selectedProxy]?.names?.lastOrNull()
                ?: group.selectedProxy
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BoxWithConstraints(Modifier.weight(1f)) {
                    val availableWidth = maxWidth
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = group.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (availableWidth >= 270.dp) {
                            Text(
                                displayGroupType(group.type),
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
                Box(Modifier.width(64.dp), contentAlignment = Alignment.CenterEnd) {
                    if (group.activeDelayTested) ProxyDelayText(group.activeDelay)
                }
            }
            if (current.isNotBlank()) {
                Text(
                    text = current,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (expanded) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .75f),
            )
            Column {
                visibleProxies.forEachIndexed { index, proxy ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .32f),
                        )
                    }
                    ProxyCard(
                        proxy = proxy,
                        selected = proxy.name == group.selectedProxy,
                        selecting = proxy.name == group.selectingProxy,
                        route = group.nestedRoutes[proxy.name],
                        showDelay = group.delayTested,
                        enabled = group.selectable && group.selectingProxy == null,
                        onClick = { onSelectProxy(proxy.name) },
                    )
                }
                if (visibleProxies.isEmpty()) {
                    Text(
                        "没有匹配的节点",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxySettingsSheet(
    state: ProxyUiState,
    onDismiss: () -> Unit,
    onSortChanged: (ProxySort) -> Unit,
    onHideUnselectableChanged: (Boolean) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(
                text = "代理设置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "排序",
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            ProxySort.entries.forEach { sort ->
                val label = when (sort) {
                    ProxySort.Default -> "默认排序"
                    ProxySort.Title -> "按名称"
                    ProxySort.Delay -> "按延迟"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSortChanged(sort)
                            onDismiss()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    RadioButton(
                        selected = state.sort == sort,
                        onClick = {
                            onSortChanged(sort)
                            onDismiss()
                        },
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHideUnselectableChanged(!state.hideUnselectableGroups) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("隐藏不可选代理组", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Checkbox(
                    checked = state.hideUnselectableGroups,
                    onCheckedChange = null,
                )
            }
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onDismiss,
            ) {
                Text("完成")
            }
        }
    }
}

@Composable
private fun ProxyCard(
    proxy: Proxy,
    selected: Boolean,
    selecting: Boolean,
    enabled: Boolean,
    route: ProxyRouteUiState?,
    showDelay: Boolean,
    onClick: () -> Unit,
) {
    val itemColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow
    val itemContentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemColor)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val finalEndpoint = if (proxy.isGroup) route?.names?.lastOrNull() else null
        Text(
            text = if (finalEndpoint.isNullOrBlank() || finalEndpoint == proxy.name) proxy.title
                else "${proxy.name} → $finalEndpoint",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = itemContentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val delay = if (proxy.isGroup) route?.delay ?: proxy.delay else proxy.delay
        Box(Modifier.width(64.dp), contentAlignment = Alignment.CenterEnd) {
            if (selecting) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else if (showDelay && (delay == 65535 || delay in 1..65534)) {
                ProxyDelayText(delay)
            }
        }
    }
}

@Composable
private fun ProxyDelayText(delay: Int) {
    val color = when {
        delay == 65535 -> MaterialTheme.colorScheme.error.copy(alpha = .78f)
        delay in 1..399 -> MaterialTheme.colorScheme.primary.copy(alpha = .82f)
        delay in 400..799 -> MaterialTheme.colorScheme.tertiary.copy(alpha = .82f)
        delay in 800..65534 -> MaterialTheme.colorScheme.error.copy(alpha = .82f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = if (delay == 65535) "超时" else "$delay ms",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = color,
        maxLines = 1,
        softWrap = false,
    )
}

private fun displayGroupType(type: String): String = when (type.lowercase()) {
    "urltest", "url-test" -> "自动测速"
    "loadbalance", "load-balance" -> "负载均衡"
    "selector" -> "手动选择"
    "fallback" -> "故障切换"
    "relay" -> "链式代理"
    else -> type
}

@Composable
private fun ProxyMessage(title: String, message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProxyScreenPreview() {
    AuroraTheme {
        ProxyScreen(
            state = ProxyUiState(
                serviceRunning = true,
                groups = listOf(
                    ProxyGroupUiState(
                        name = "节点选择",
                        selectedProxy = "Hong Kong 01",
                        selectable = true,
                        proxies = listOf(
                            Proxy("hk", "Hong Kong 01", "香港", "Shadowsocks", 68, false),
                            Proxy("sg", "Singapore 01", "新加坡", "VLESS", 132, false),
                        ),
                    ),
                ),
            ),
            onSelectGroup = {},
            onGroupExpandedChange = { _, _ -> },
            onSelectProxy = { _, _ -> },
            onRefresh = {},
            onSortChanged = {},
            onHideUnselectableChanged = {},
        )
    }
}
