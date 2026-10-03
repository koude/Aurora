package com.koude.aurora.ui.proxy

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxySort
import com.koude.aurora.designsystem.theme.AuroraTheme

data class ProxyGroupUiState(
    val name: String,
    val type: String = "Selector",
    val selectedProxy: String = "",
    val selectable: Boolean = false,
    val delayTested: Boolean = false,
    val proxies: List<Proxy> = emptyList(),
    val nestedRoutes: Map<String, ProxyRouteUiState> = emptyMap(),
    val testing: Boolean = false,
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
    val errorMessage: String? = null,
    val sort: ProxySort = ProxySort.Default,
    val hideUnselectableGroups: Boolean = false,
)

@Composable
fun ProxyScreen(
    state: ProxyUiState,
    onSelectGroup: (Int) -> Unit,
    onSelectProxy: (groupIndex: Int, proxyName: String) -> Unit,
    onTestGroup: (Int) -> Unit,
    onRefresh: () -> Unit,
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
                onSelectProxy = onSelectProxy,
                onTestGroup = onTestGroup,
                onRefresh = onRefresh,
                onSortChanged = onSortChanged,
                onHideUnselectableChanged = onHideUnselectableChanged,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = padding.calculateTopPadding() + 18.dp,
                    end = 20.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
            )
        }
    }
}

@Composable
private fun ProxyContent(
    state: ProxyUiState,
    onSelectGroup: (Int) -> Unit,
    onSelectProxy: (Int, String) -> Unit,
    onTestGroup: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSortChanged: (ProxySort) -> Unit,
    onHideUnselectableChanged: (Boolean) -> Unit,
    contentPadding: PaddingValues,
) {
    var settingsVisible by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    top = contentPadding.calculateTopPadding(),
                    end = 12.dp,
                    bottom = 8.dp,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "代理",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row {
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
                IconButton(onClick = { settingsVisible = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "代理设置")
                }
                IconButton(onClick = onRefresh, enabled = !state.loading) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        }

        if (searchVisible) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (normalizedQuery.isBlank()) "${state.groups.size} 个策略组"
                        else "${filteredGroups.size} / ${state.groups.size} 个策略组",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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
                    else expandedGroups.getOrPut(item.name) { index == state.selectedGroupIndex }
                    ProxyGroupCard(
                        group = item,
                        expanded = expanded,
                        searchQuery = normalizedQuery,
                        onExpand = {
                            val next = !expanded
                            expandedGroups[item.name] = next
                            if (next) onSelectGroup(index)
                        },
                        onSelectProxy = { name -> onSelectProxy(index, name) },
                        onTestGroup = { onTestGroup(index) },
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
    onTestGroup: () -> Unit,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    val selectedRoute = group.nestedRoutes[group.selectedProxy]?.names
                    val current = selectedRoute?.joinToString(" → ") ?: group.selectedProxy
                    if (current.isNotBlank()) {
                        Text(
                            text = "当前选择：$current",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        displayGroupType(group.type),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起${group.name}" else "展开${group.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${visibleProxies.size} 个选项",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onTestGroup, enabled = !group.testing) {
                        if (group.testing) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (group.testing) "测速中" else "测速")
                    }
                }
                Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp)) {
                    visibleProxies.forEach { proxy ->
                        ProxyCard(
                            proxy = proxy,
                            selected = proxy.name == group.selectedProxy,
                            route = group.nestedRoutes[proxy.name],
                            showDelay = group.delayTested,
                            enabled = group.selectable,
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
    enabled: Boolean,
    route: ProxyRouteUiState?,
    showDelay: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 17.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(width = 4.dp, height = 34.dp)
                        .padding(vertical = 1.dp),
                ) {
                    Surface(Modifier.fillMaxSize(), shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primary) {}
                }
            }
            Column(Modifier.weight(1f)) {
                val routeNames = if (proxy.isGroup) route?.names else null
                Text(
                    text = routeNames?.joinToString(" → ") ?: if (proxy.isGroup) proxy.name else proxy.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = if (proxy.isGroup) "嵌套策略组" else {
                    listOf(proxy.subtitle.takeIf { it.isNotBlank() && it != proxy.type }, shortProtocol(proxy.type))
                        .filterNotNull()
                        .filter(String::isNotBlank)
                        .distinct()
                        .joinToString(" · ")
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val delay = if (proxy.isGroup) route?.delay ?: proxy.delay else proxy.delay
            if (showDelay && delay == 65535) {
                Text("超时", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            } else if (showDelay && delay in 1..65534) {
                val delayColor = when (delay) {
                    in 1..399 -> MaterialTheme.colorScheme.primary
                    in 400..799 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = delayColor.copy(alpha = .12f),
                    contentColor = delayColor,
                ) {
                    Text(
                        text = "$delay ms",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun shortProtocol(type: String): String = when (type.lowercase()) {
    "shadowsocks" -> "SS"
    "shadowsocksr" -> "SSR"
    "hysteria2" -> "Hy2"
    else -> type
}

private fun displayGroupType(type: String): String = when (type.lowercase()) {
    "urltest" -> "URL-Test"
    "loadbalance" -> "Load-Balance"
    "selector" -> "Selector"
    "fallback" -> "Fallback"
    "relay" -> "Relay"
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
            onSelectProxy = { _, _ -> },
            onTestGroup = {},
            onRefresh = {},
            onSortChanged = {},
            onHideUnselectableChanged = {},
        )
    }
}
