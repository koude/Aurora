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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.Proxy
import com.koude.aurora.designsystem.theme.AuroraTheme

data class ProxyGroupUiState(
    val name: String,
    val selectedProxy: String = "",
    val selectable: Boolean = false,
    val proxies: List<Proxy> = emptyList(),
    val testing: Boolean = false,
)

data class ProxyUiState(
    val loading: Boolean = false,
    val serviceRunning: Boolean = false,
    val groups: List<ProxyGroupUiState> = emptyList(),
    val selectedGroupIndex: Int = 0,
    val errorMessage: String? = null,
)

@Composable
fun ProxyScreen(
    state: ProxyUiState,
    onSelectGroup: (Int) -> Unit,
    onSelectProxy: (groupIndex: Int, proxyName: String) -> Unit,
    onTestGroup: (Int) -> Unit,
    onRefresh: () -> Unit,
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
    contentPadding: PaddingValues,
) {
    val selectedIndex = state.selectedGroupIndex.coerceIn(state.groups.indices)
    val group = state.groups[selectedIndex]

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
            Column {
                Text(
                    text = "代理",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = group.selectedProxy.ifBlank { "尚未选择节点" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRefresh, enabled = !state.loading) {
                if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 20.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {},
        ) {
            state.groups.forEachIndexed { index, item ->
                Tab(
                    selected = index == selectedIndex,
                    onClick = { onSelectGroup(index) },
                    text = { Text(item.name, maxLines = 1) },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                top = 14.dp,
                end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${group.proxies.size} 个节点",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        modifier = Modifier.clickable(enabled = !group.testing) {
                            onTestGroup(selectedIndex)
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (group.testing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(if (group.testing) "测试中" else "延迟测试", style = MaterialTheme.typography.labelLarge)
                        }
                    }
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

            items(group.proxies, key = Proxy::name) { proxy ->
                ProxyCard(
                    proxy = proxy,
                    selected = proxy.name == group.selectedProxy,
                    enabled = group.selectable,
                    onClick = { onSelectProxy(selectedIndex, proxy.name) },
                )
            }
        }
    }
}

@Composable
private fun ProxyCard(
    proxy: Proxy,
    selected: Boolean,
    enabled: Boolean,
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
                Text(
                    text = if (proxy.isGroup) proxy.name else proxy.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = if (proxy.isGroup) proxy.type else proxy.subtitle
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
            if (proxy.delay in 0..Short.MAX_VALUE) {
                val delayColor = when (proxy.delay) {
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
                        text = "${proxy.delay} ms",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
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
        )
    }
}
