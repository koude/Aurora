package com.aurora.client.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurora.client.data.AppState
import com.aurora.client.data.ConnectionState

private enum class Page { HOME, NODES, SETTINGS }

@Composable
fun AuroraApp(onConnect: () -> Unit, onDisconnect: () -> Unit) {
    var page by remember { mutableStateOf(Page.HOME) }
    val state by AppState.connection.collectAsState()
    val message by AppState.message.collectAsState()
    val context = LocalContext.current
    var imported by remember { mutableStateOf(context.filesDir.resolve("config.yaml").exists()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) imported = copyConfig(context, uri)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = page == Page.HOME,
                    onClick = { page = Page.HOME },
                    icon = { Icon(Icons.Outlined.Home, null) },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = page == Page.NODES,
                    onClick = { page = Page.NODES },
                    icon = { Icon(Icons.Outlined.Public, null) },
                    label = { Text("节点") }
                )
                NavigationBarItem(
                    selected = page == Page.SETTINGS,
                    onClick = { page = Page.SETTINGS },
                    icon = { Icon(Icons.Outlined.Settings, null) },
                    label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (page) {
                Page.HOME -> HomeScreen(
                    state = state,
                    message = message,
                    imported = imported,
                    onImport = { picker.launch(arrayOf("application/x-yaml", "text/yaml", "text/plain", "*/*")) },
                    onToggle = {
                        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) onDisconnect()
                        else onConnect()
                    }
                )
                Page.NODES -> NodesScreen()
                Page.SETTINGS -> SettingsScreen(imported)
            }
        }
    }
}

private fun copyConfig(context: Context, uri: Uri): Boolean = runCatching {
    context.contentResolver.openInputStream(uri)!!.use { input ->
        context.filesDir.resolve("config.yaml").outputStream().use { output -> input.copyTo(output) }
    }
    true
}.getOrDefault(false)

@Composable
private fun HomeScreen(
    state: ConnectionState,
    message: String?,
    imported: Boolean,
    onImport: () -> Unit,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Landscape, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Aurora", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("安静 · 简洁 · 只为连接", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(44.dp))

        val active = state == ConnectionState.CONNECTED
        val connecting = state == ConnectionState.CONNECTING
        val statusText = when (state) {
            ConnectionState.DISCONNECTED -> "未连接"
            ConnectionState.CONNECTING -> "正在连接"
            ConnectionState.CONNECTED -> "已连接"
            ConnectionState.ERROR -> "连接失败"
        }

        Box(
            Modifier.size(190.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
                .clickable(enabled = !connecting) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (active) Icons.Outlined.PowerSettingsNew else Icons.Outlined.PowerSettingsNew,
                    null,
                    modifier = Modifier.size(56.dp),
                    tint = if (active) MaterialTheme.colorScheme.primary else Color.DarkGray
                )
                Spacer(Modifier.height(10.dp))
                Text(statusText, fontWeight = FontWeight.Medium)
                Text(if (active) "点击断开" else "点击连接", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(28.dp))

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (imported) "配置已导入" else "还没有配置", fontWeight = FontWeight.Medium)
                        Text(
                            if (imported) "config.yaml · 本地保存" else "导入 Clash/Mihomo YAML 配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    TextButton(onClick = onImport) { Text(if (imported) "更换" else "导入") }
                }
            }
        }

        if (message != null) {
            Spacer(Modifier.height(14.dp))
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("模式", "Rule")
            Stat("节点", "Auto Select")
            Stat("延迟", "42 ms")
        }
    }
}

@Composable
private fun Stat(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun NodesScreen() {
    var selected by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("节点", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("选择出口节点", color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        AppState.nodes.forEachIndexed { index, node ->
            ElevatedCard(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selected = index },
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(node.name, fontWeight = FontWeight.Medium)
                        Text("${node.delayMs} ms", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    RadioButton(selected = selected == index, onClick = { selected = index })
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(imported: Boolean) {
    var autoStart by remember { mutableStateOf(false) }
    var darkFollow by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        SettingSwitch("系统启动后恢复连接", "需要后续加入开机广播", autoStart) { autoStart = it }
        SettingSwitch("跟随系统外观", "当前 v0.1 使用浅色主题", darkFollow) { darkFollow = it }
        SettingRow("本地配置", if (imported) "config.yaml" else "未导入")
        SettingRow("核心", "mihomo adapter · 待接入")
        SettingRow("遥测", "无")
        SettingRow("版本", "0.2.0")
        Spacer(Modifier.height(24.dp))
        Text(
            "Aurora 不包含广告、统计 SDK 或远程日志。v0.1 已完成 Android VPN 控制层与 UI，代理核心通过独立适配层接入。",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
    HorizontalDivider()
}

@Composable
private fun SettingRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Text(value, color = Color.Gray)
    }
    HorizontalDivider()
}
