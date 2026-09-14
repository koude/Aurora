package com.koude.aurora.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koude.aurora.data.AppLogger
import com.koude.aurora.data.AppState
import com.koude.aurora.data.ConfigImporter
import com.koude.aurora.data.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Page { HOME, NODES, SETTINGS, LOGS }

@Composable
fun AuroraApp(onConnect: () -> Unit, onDisconnect: () -> Unit) {
    var page by remember { mutableStateOf(Page.HOME) }
    val state by AppState.connection.collectAsState()
    val message by AppState.message.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var imported by remember { mutableStateOf(ConfigImporter.hasConfig(context)) }
    var sourceLabel by remember { mutableStateOf(ConfigImporter.sourceLabel(context)) }
    var showImportOptions by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var importingUrl by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) { ConfigImporter.importFile(context, uri) }
                result.onSuccess {
                    imported = true
                    sourceLabel = ConfigImporter.sourceLabel(context)
                    AppState.showMessage("配置导入成功")
                }.onFailure {
                    AppState.showMessage(it.message ?: "配置导入失败")
                }
            }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            AppState.clearMessage()
        }
    }

    if (showImportOptions) {
        AlertDialog(
            onDismissRequest = { showImportOptions = false },
            title = { Text("导入配置") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("从文件导入") },
                        supportingContent = { Text("选择本地 YAML 配置") },
                        leadingContent = { Icon(Icons.Outlined.FolderOpen, null) },
                        modifier = Modifier.clickable {
                            showImportOptions = false
                            picker.launch(arrayOf("application/x-yaml", "text/yaml", "text/plain", "*/*"))
                        }
                    )
                    ListItem(
                        headlineContent = { Text("从链接导入") },
                        supportingContent = { Text("粘贴 http/https 配置或订阅链接") },
                        leadingContent = { Icon(Icons.Outlined.Link, null) },
                        modifier = Modifier.clickable {
                            showImportOptions = false
                            showUrlDialog = true
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showImportOptions = false }) { Text("取消") } }
        )
    }

    if (showUrlDialog) {
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!importingUrl) showUrlDialog = false },
            title = { Text("从链接导入") },
            text = {
                Column {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        singleLine = true,
                        label = { Text("配置链接") },
                        placeholder = { Text("https://…") },
                        enabled = !importingUrl,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (importingUrl) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = url.isNotBlank() && !importingUrl,
                    onClick = {
                        importingUrl = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { ConfigImporter.importUrl(context, url) }
                            importingUrl = false
                            result.onSuccess {
                                imported = true
                                sourceLabel = ConfigImporter.sourceLabel(context)
                                showUrlDialog = false
                                AppState.showMessage("配置下载并导入成功")
                            }.onFailure {
                                AppState.showMessage(it.message ?: "链接导入失败")
                            }
                        }
                    }
                ) { Text("导入") }
            },
            dismissButton = {
                TextButton(enabled = !importingUrl, onClick = { showUrlDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (page != Page.LOGS) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(page == Page.HOME, { page = Page.HOME }, { Icon(Icons.Outlined.Home, null) }, label = { Text("首页") })
                    NavigationBarItem(page == Page.NODES, { page = Page.NODES }, { Icon(Icons.Outlined.Public, null) }, label = { Text("节点") })
                    NavigationBarItem(page == Page.SETTINGS, { page = Page.SETTINGS }, { Icon(Icons.Outlined.Settings, null) }, label = { Text("设置") })
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (page) {
                Page.HOME -> HomeScreen(
                    state = state,
                    imported = imported,
                    sourceLabel = sourceLabel,
                    onImport = { showImportOptions = true },
                    onToggle = {
                        when {
                            state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING -> onDisconnect()
                            !imported -> AppState.showMessage("请先导入配置")
                            else -> onConnect()
                        }
                    }
                )
                Page.NODES -> NodesScreen()
                Page.SETTINGS -> SettingsScreen(imported, sourceLabel, onOpenLogs = { page = Page.LOGS })
                Page.LOGS -> LogsScreen(onBack = { page = Page.SETTINGS })
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: ConnectionState,
    imported: Boolean,
    sourceLabel: String,
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
                .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .clickable(enabled = !connecting) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (connecting) {
                    CircularProgressIndicator(Modifier.size(48.dp), strokeWidth = 4.dp)
                } else {
                    Icon(Icons.Outlined.PowerSettingsNew, null, modifier = Modifier.size(56.dp), tint = if (active) MaterialTheme.colorScheme.primary else Color.DarkGray)
                }
                Spacer(Modifier.height(10.dp))
                Text(statusText, fontWeight = FontWeight.Medium)
                Text(if (active) "点击断开" else if (connecting) "正在启动核心" else "点击连接", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                        Text(if (imported) "$sourceLabel · config.yaml" else "支持本地 YAML 或配置链接", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    TextButton(onClick = onImport) { Text(if (imported) "更换" else "导入") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("模式", "Rule")
            Stat("节点", "Auto Select")
            Stat("延迟", "--")
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
        Text("当前页面仍为演示节点，实时节点读取将在后续版本接入", color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        AppState.nodes.forEachIndexed { index, node ->
            ElevatedCard(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { selected = index }, shape = RoundedCornerShape(18.dp)) {
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
private fun SettingsScreen(imported: Boolean, sourceLabel: String, onOpenLogs: () -> Unit) {
    val vpnDiagnostic by AppState.vpnDiagnostic.collectAsState()
    var autoStart by remember { mutableStateOf(false) }
    var darkFollow by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        SettingSwitch("系统启动后恢复连接", "尚未启用开机自连", autoStart) { autoStart = it }
        SettingSwitch("跟随系统外观", "使用系统深浅色设置", darkFollow) { darkFollow = it }
        SettingRow("配置", if (imported) sourceLabel else "未导入")
        SettingRow("核心", "libmihomo-android 0.3.1")
        SettingRow("日志", "本机保存 · 最大约 2 MB", onClick = onOpenLogs)
        SettingRow("遥测", "无")
        SettingRow("版本", "0.3.0")
        Spacer(Modifier.height(18.dp))
        Text("VPN 授权诊断", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(vpnDiagnostic, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        Text("Aurora 不包含广告、统计 SDK 或远程日志。诊断日志仅保存在本机，可由你手动复制或导出。", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
private fun LogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lines by AppLogger.lines.collectAsState()
    val scroll = rememberScrollState()

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(AppLogger.allText().toByteArray())
                } ?: error("无法打开导出文件")
            }.onSuccess {
                AppState.showMessage("日志已导出")
                AppLogger.i("UI", "Log exported by user")
            }.onFailure {
                AppState.showMessage(it.message ?: "日志导出失败")
                AppLogger.e("UI", "Log export failed: ${it.message}", it)
            }
        }
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) scroll.animateScrollTo(scroll.maxValue)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "返回") }
            Text("日志", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("${lines.size} 行", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Aurora log", AppLogger.allText()))
                    AppState.showMessage("日志已复制")
                    AppLogger.i("UI", "Log copied by user")
                }
            ) { Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("复制") }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { exporter.launch("Aurora-v0.3.0-log.txt") }
            ) { Icon(Icons.Outlined.UploadFile, null); Spacer(Modifier.width(6.dp)); Text("导出") }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { AppLogger.clear(); AppState.showMessage("日志已清空") }
            ) { Icon(Icons.Outlined.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("清空") }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = if (lines.isEmpty()) "暂无日志" else lines.joinToString("\n"),
                modifier = Modifier.padding(12.dp).fillMaxSize().verticalScroll(scroll),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
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
private fun SettingRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Text(value, color = Color.Gray)
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.ChevronRight, null, tint = Color.Gray)
        }
    }
    HorizontalDivider()
}
