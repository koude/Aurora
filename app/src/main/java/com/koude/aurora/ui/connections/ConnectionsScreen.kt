package com.koude.aurora.ui.connections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var confirmCloseAll by remember { mutableStateOf(false) }
    val filtered = remember(state.connections, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) state.connections else state.connections.filter { connection ->
            listOf(
                connection.host,
                connection.process,
                connection.network,
                connection.rule,
                connection.rulePayload,
                connection.chains.joinToString(" "),
            ).any { normalized in it.lowercase() }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = padding.calculateTopPadding() + 18.dp,
                end = 20.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "连接",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = if (state.serviceRunning) "${state.connections.size} 条活动连接" else "未连接",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        IconButton(onClick = onRefresh, enabled = !state.loading) {
                            if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        IconButton(
                            onClick = { confirmCloseAll = true },
                            enabled = state.connections.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "关闭全部连接")
                        }
                    }
                }
            }

            if (state.connections.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("搜索应用、域名或规则") },
                        shape = MaterialTheme.shapes.large,
                    )
                }
            }

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
                    ConnectionCard(connection, onClose = { onCloseConnection(connection.id) })
                }
            }
        }
    }

    if (confirmCloseAll) {
        AlertDialog(
            onDismissRequest = { confirmCloseAll = false },
            title = { Text("关闭全部连接？") },
            text = { Text("当前 ${state.connections.size} 条活动连接将被中断。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCloseAll = false
                    onCloseAll()
                }) { Text("关闭全部") }
            },
            dismissButton = { TextButton(onClick = { confirmCloseAll = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ConnectionCard(connection: ConnectionInfo, onClose: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = connection.host.ifBlank { connection.destination.ifBlank { "未知目标" } },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            connection.process.takeIf(String::isNotBlank),
                            connection.network.uppercase().takeIf(String::isNotBlank),
                            connection.startedAt.takeIf { it > 0 }?.let(::formatStartedAt),
                        ).joinToString(" · ").ifBlank { connection.destination },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "关闭连接")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 6.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = buildString {
                    append(connection.rule.ifBlank { "未匹配规则" })
                    if (connection.rulePayload.isNotBlank()) append(" · ${connection.rulePayload}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val route = connection.chains.joinToString(" → ").ifBlank { "DIRECT" }
            Text(
                text = route,
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("↑ ${formatBytes(connection.uploaded)}", style = MaterialTheme.typography.labelMedium)
                Text("↓ ${formatBytes(connection.downloaded)}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ConnectionsEmptyState(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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

private fun formatStartedAt(timestamp: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))

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
            onCloseAll = {},
        )
    }
}
