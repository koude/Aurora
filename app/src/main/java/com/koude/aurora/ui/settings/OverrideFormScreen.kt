package com.koude.aurora.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ConfigFieldEditor { Text, Lines, Choices }

data class ConfigChoice(val value: String?, val label: String)

data class ConfigField(
    val section: String,
    val title: String,
    val value: String,
    val editor: ConfigFieldEditor,
    val initialText: String = "",
    val choices: List<ConfigChoice> = emptyList(),
    val selectedValue: String? = null,
    val enabled: Boolean = true,
    val onSet: (String?) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverrideFormScreen(
    title: String,
    fields: List<ConfigField>,
    onBack: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<ConfigField?>(null) }
    var resetMenu by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Medium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                actions = {
                    IconButton(onClick = { resetMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                    DropdownMenu(expanded = resetMenu, onDismissRequest = { resetMenu = false }) {
                        DropdownMenuItem(text = { Text("重置全部覆写") }, onClick = { resetMenu = false; confirmReset = true })
                    }
                },
            )
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            fields.groupBy { it.section }.forEach { (section, sectionFields) ->
                item(key = "section:$section") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(section, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column {
                                sectionFields.forEachIndexed { index, field ->
                                    ListItem(
                                        modifier = Modifier.fillMaxWidth().clickable(enabled = field.enabled) { selected = field },
                                        headlineContent = { Text(field.title) },
                                        supportingContent = { Text(field.value, maxLines = 2) },
                                    )
                                    if (index != sectionFields.lastIndex) HorizontalDivider(Modifier.padding(start = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { field ->
        if (field.enabled) when (field.editor) {
            ConfigFieldEditor.Choices -> AlertDialog(
                onDismissRequest = { selected = null },
                title = { Text(field.title) },
                text = {
                    Column {
                        field.choices.forEach { choice ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { field.onSet(choice.value); selected = null }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = field.selectedValue == choice.value, onClick = { field.onSet(choice.value); selected = null })
                                Text(choice.label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { selected = null }) { Text("关闭") } },
            )
            ConfigFieldEditor.Text, ConfigFieldEditor.Lines -> ConfigTextEditor(field, onDismiss = { selected = null })
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("重置全部覆写？") },
        text = { Text("所有保存的覆写设置都会被清除。") },
        confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text("重置") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("取消") } },
    )
}

@Composable
private fun ConfigTextEditor(field: ConfigField, onDismiss: () -> Unit) {
    var value by remember(field) { mutableStateOf(field.initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(field.title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = if (field.editor == ConfigFieldEditor.Lines) 4 else 1,
                maxLines = if (field.editor == ConfigFieldEditor.Lines) 8 else 1,
                supportingText = { Text(if (field.editor == ConfigFieldEditor.Lines) "每行一项；使用“恢复不修改”可清除此覆写" else "留空值会按该设置项的默认行为处理") },
            )
        },
        confirmButton = { TextButton(onClick = { field.onSet(value); onDismiss() }) { Text("保存") } },
        dismissButton = {
            Row {
                TextButton(onClick = { field.onSet(null); onDismiss() }) { Text("恢复不修改") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
