package com.koude.aurora.ui.connections

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.ConnectionInfo
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.components.AuroraPageHeader
import com.koude.aurora.ui.components.AuroraPageSpacing
import com.koude.aurora.ui.components.AuroraCardStyle
import java.text.DateFormat
import java.util.Date

data class ConnectionsUiState(
    val serviceRunning: Boolean = false,
    val loading: Boolean = false,
    val connections: List<ConnectionInfo> = emptyList(),
    val errorMessage: String? = null,
)

@Composable
fun ConnectionsScreen(
    state: ConnectionsUiState,
    onRefresh: () -> Unit,
    onCloseConnection: (String) -> Unit,
    onCloseVisible: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var pendingCloseIds by remember { mutableStateOf<List<String>?>(null) }
    var selectedConnection by remember { mutableStateOf<ConnectionInfo?>(null) }
    val filtered = remember(state.connections, query) {
        filterConnections(state.connections, query)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            AuroraPageHeader(
                title = "连接",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AuroraPageSpacing.Horizontal,
                        top = padding.calculateTopPadding() + AuroraPageSpacing.Top,
                        end = 12.dp,
                        bottom = 8.dp,
                    ),
                actions = {
                    IconButton(onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) query = ""
                    }) {
                        Icon(
                            if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (searchVisible) "关闭搜索" else "搜索连接",
                        )
                    }
                    IconButton(onClick = onRefresh, enabled = !state.loading) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    IconButton(
                        onClick = { pendingCloseIds = filtered.map(ConnectionInfo::id) },
                        enabled = state.serviceRunning && filtered.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭所示连接")
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
                    placeholder = { Text("搜索应用、域名或规则") },
                    shape = MaterialTheme.shapes.large,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AuroraPageSpacing.Horizontal,
                    top = 8.dp,
                    end = AuroraPageSpacing.Horizontal,
                    bottom = padding.calculateBottomPadding() + AuroraPageSpacing.Bottom,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.errorMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (!state.serviceRunning) {
                    item { ConnectionsEmptyState("代理未运行") }
                } else if (state.loading && state.connections.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (filtered.isEmpty()) {
                    item { ConnectionsEmptyState(if (query.isBlank()) "暂无活动连接" else "没有匹配的连接") }
                } else {
                    items(filtered, key = ConnectionInfo::id) { connection ->
                        ConnectionCard(
                            connection = connection,
                            onClick = { selectedConnection = connection },
                            onClose = { onCloseConnection(connection.id) },
                        )
                    }
                }
            }
        }
    }

    pendingCloseIds?.let { ids ->
        AlertDialog(
            onDismissRequest = { pendingCloseIds = null },
            title = { Text("关闭所示连接？") },
            text = { Text("将中断列表中显示的 ${ids.size} 条连接。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingCloseIds = null
                    onCloseVisible(ids)
                }) { Text("关闭") }
            },
            dismissButton = { TextButton(onClick = { pendingCloseIds = null }) { Text("取消") } },
        )
    }
    selectedConnection?.let { connection ->
        ConnectionDetailsSheet(connection, onDismiss = { selectedConnection = null })
    }
}

internal fun filterConnections(connections: List<ConnectionInfo>, query: String): List<ConnectionInfo> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isEmpty()) return connections
    return connections.filter { connection ->
        listOf(
            connection.host,
            connection.destination,
            connection.process,
            connection.network,
            connection.rule,
            connection.rulePayload,
            connection.chains.joinToString(" "),
        ).any { normalizedQuery in it.lowercase() }
    }
}

internal fun connectionActualOutlet(connection: ConnectionInfo): String =
    connection.chains.firstOrNull()?.takeIf(String::isNotBlank) ?: "未知去向"

internal fun connectionRoutePath(connection: ConnectionInfo): String =
    connection.chains.asReversed().joinToString(" → ").ifBlank { "未知去向" }

@Composable
private fun ConnectionCard(connection: ConnectionInfo, onClick: () -> Unit, onClose: () -> Unit) {
    Card(
        onClick = onClick,
        shape = AuroraCardStyle.groupShape(),
        colors = CardDefaults.cardColors(containerColor = AuroraCardStyle.groupColor()),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = connection.host.ifBlank { connection.destination.ifBlank { "未知目标" } },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "关闭连接")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = connection.rule.ifBlank { "未匹配规则" },
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("→", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = connectionActualOutlet(connection),
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (connection.process.isNotBlank()) {
                    Text(
                        text = connection.process,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Box(Modifier.weight(1f))
                }
                Text(
                    text = "↑ ${formatBytes(connection.uploaded)}   ↓ ${formatBytes(connection.downloaded)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionDetailsSheet(connection: ConnectionInfo, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = connection.host.ifBlank { connection.destination.ifBlank { "连接详情" } },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            ConnectionDetailField("目标地址", connection.destination)
            ConnectionDetailField("应用 / 进程", connection.process)
            ConnectionDetailField(
                "命中规则",
                listOf(connection.rule, connection.rulePayload).filter(String::isNotBlank).joinToString(" · "),
            )
            ConnectionDetailField("代理路径", connectionRoutePath(connection))
            ConnectionDetailField("网络类型", connection.network.uppercase())
            if (connection.startedAt > 0) {
                ConnectionDetailField(
                    "建立时间",
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(connection.startedAt)),
                )
            }
            ConnectionDetailField(
                "流量",
                "↑ ${formatBytes(connection.uploaded)}   ↓ ${formatBytes(connection.downloaded)}",
            )
        }
    }
}

@Composable
private fun ConnectionDetailField(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ConnectionsEmptyState(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AuroraCardStyle.groupShape(),
        colors = CardDefaults.cardColors(containerColor = AuroraCardStyle.groupColor()),
    ) {
        Box(Modifier.padding(28.dp), contentAlignment = Alignment.Center) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KiB", "MiB", "GiB")
    var value = bytes.toDouble()
    for (unit in units) {
        value /= 1024.0
        if (value < 1024.0 || unit == units.last()) return "%.1f %s".format(value, unit)
    }
    return "$bytes B"
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ConnectionsScreenPreview() {
    AuroraTheme {
        ConnectionsScreen(
            state = ConnectionsUiState(
                serviceRunning = true,
                connections = listOf(
                    ConnectionInfo(
                        id = "preview",
                        host = "github.com",
                        process = "com.android.chrome",
                        network = "tcp",
                        destination = "140.82.112.4:443",
                        rule = "DOMAIN-SUFFIX",
                        rulePayload = "github.com",
                        chains = listOf("Proxy", "Tokyo 01"),
                        uploaded = 5_120,
                        downloaded = 73_728,
                        startedAt = System.currentTimeMillis(),
                    ),
                ),
            ),
            onRefresh = {},
            onCloseConnection = {},
            onCloseVisible = {},
        )
    }
}
