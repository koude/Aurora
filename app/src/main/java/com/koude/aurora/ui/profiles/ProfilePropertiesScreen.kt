package com.koude.aurora.ui.profiles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.service.model.Profile
import java.util.concurrent.TimeUnit

private enum class PropertyField { Name, Url, AgeSecretKey, Interval }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePropertiesScreen(
    profile: Profile,
    hasUnsavedChanges: Boolean,
    saving: Boolean,
    progress: String?,
    onProfileChange: (Profile) -> Unit,
    onSave: () -> Unit,
    onBrowseFiles: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<PropertyField?>(null) }
    var draft by remember { mutableStateOf("") }
    var showDiscard by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun requestBack() {
        if (saving) return
        if (hasUnsavedChanges) showDiscard = true else onBack()
    }
    BackHandler(enabled = !saving) { requestBack() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("配置详情", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = ::requestBack, enabled = !saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (saving) CircularProgressIndicator(Modifier.padding(horizontal = 16.dp), strokeWidth = 2.dp)
                    else TextButton(onClick = onSave) { Text("保存") }
                },
            )
        },
    ) { inset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inset),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (progress != null) item {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.padding(end = 16.dp), strokeWidth = 2.dp)
                    Text(progress, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { PropertyRow(painterResource(DesignR.drawable.ic_outline_label), "名称", profile.name) { draft = profile.name; editing = PropertyField.Name } }
            item { HorizontalDivider() }
            item {
                PropertyRow(painterResource(DesignR.drawable.ic_outline_inbox), "订阅地址", profile.source.ifBlank { "未设置" }, enabled = profile.type == Profile.Type.Url) {
                    draft = profile.source; editing = PropertyField.Url
                }
            }
            item { HorizontalDivider() }
            item {
                PropertyRow(painterResource(DesignR.drawable.ic_baseline_key), "AGE 密钥", if (profile.ageSecretKey.isNullOrBlank()) "未设置" else "已设置", enabled = profile.type != Profile.Type.External) {
                    draft = profile.ageSecretKey.orEmpty(); editing = PropertyField.AgeSecretKey
                }
            }
            item { HorizontalDivider() }
            item {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(profile.interval)
                PropertyRow(painterResource(DesignR.drawable.ic_outline_update), "自动更新", if (minutes == 0L) "已关闭" else "$minutes 分钟", enabled = profile.type != Profile.Type.File) {
                    draft = if (minutes == 0L) "" else minutes.toString(); editing = PropertyField.Interval
                }
            }
            item { HorizontalDivider() }
            item { PropertyRow(painterResource(DesignR.drawable.ic_outline_folder), "配置文件与提供者", "浏览相关文件", onClick = onBrowseFiles) }
        }
    }

    editing?.let { field ->
        val title = when (field) {
            PropertyField.Name -> "名称"
            PropertyField.Url -> "订阅地址"
            PropertyField.AgeSecretKey -> "AGE 密钥"
            PropertyField.Interval -> "自动更新间隔（分钟）"
        }
        AlertDialog(
            onDismissRequest = { editing = null; validationError = null },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it; validationError = null },
                        singleLine = field != PropertyField.AgeSecretKey,
                        isError = validationError != null,
                        supportingText = { Text(validationError ?: when (field) {
                            PropertyField.Name -> "请输入配置名称"
                            PropertyField.Url -> "支持 HTTP/HTTPS 订阅地址"
                            PropertyField.AgeSecretKey -> "可留空；用于解密受保护的配置"
                            PropertyField.Interval -> "至少 15 分钟；留空表示关闭"
                        }) },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = draft.trim()
                    var error: String? = null
                    when (field) {
                        PropertyField.Name -> if (value.isBlank()) error = "名称不能为空" else onProfileChange(profile.copy(name = value))
                        PropertyField.Url -> if (value.isNotBlank() && !value.startsWith("http://", true) && !value.startsWith("https://", true)) error = "请输入有效的 HTTP/HTTPS 地址" else onProfileChange(profile.copy(source = value))
                        PropertyField.AgeSecretKey -> onProfileChange(profile.copy(ageSecretKey = value.ifBlank { null }))
                        PropertyField.Interval -> {
                            val minutes = value.toLongOrNull()
                            if (value.isNotBlank() && (minutes == null || minutes < 15)) error = "间隔至少为 15 分钟" else onProfileChange(profile.copy(interval = TimeUnit.MINUTES.toMillis(minutes ?: 0)))
                        }
                    }
                    validationError = error
                    if (error == null) editing = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { editing = null; validationError = null }) { Text("取消") } },
        )
    }

    if (showDiscard) AlertDialog(
        onDismissRequest = { showDiscard = false },
        title = { Text("放弃更改？") },
        text = { Text("尚未保存的配置修改将会丢失。") },
        confirmButton = { TextButton(onClick = { showDiscard = false; onBack() }) { Text("放弃") } },
        dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("继续编辑") } },
    )
}

@Composable
private fun PropertyRow(
    icon: Painter,
    title: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
