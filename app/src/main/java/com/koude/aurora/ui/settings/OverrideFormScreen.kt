package com.koude.aurora.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR
import com.koude.aurora.ui.components.AuroraDetailTopBar
import com.koude.aurora.ui.components.AuroraSectionTitle

enum class ConfigFieldEditor { Text, Lines, Choices }

data class ConfigChoice(val value: String?, val label: String)

data class ConfigAction(val id: String, val section: String, val title: String, val summary: String)

data class ConfigField(
    val section: String,
    val title: String,
    val value: String,
    val editor: ConfigFieldEditor,
    val initialText: String = "",
    val choices: List<ConfigChoice> = emptyList(),
    val selectedValue: String? = null,
    val enabled: Boolean = true,
    val supportingText: String? = null,
    val inputError: ((String) -> String?)? = null,
    val sensitive: Boolean = false,
    val onSet: (String?) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverrideFormScreen(
    title: String,
    fields: List<ConfigField>,
    actions: List<ConfigAction> = emptyList(),
    onAction: (String) -> Unit = {},
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
            AuroraDetailTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { resetMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(DesignR.string.more))
                    }
                    DropdownMenu(expanded = resetMenu, onDismissRequest = { resetMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(DesignR.string.reset_override_settings)) },
                            onClick = { resetMenu = false; confirmReset = true },
                        )
                    }
                },
            )
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val fieldsBySection = fields.groupBy { it.section }
            val actionsBySection = actions.groupBy { it.section }
            (actions.map { it.section } + fields.map { it.section }).distinct().forEach { section ->
                val sectionFields = fieldsBySection[section].orEmpty()
                val sectionActions = actionsBySection[section].orEmpty()
                item(key = "section:$section") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AuroraSectionTitle(section, Modifier.padding(start = 4.dp))
                        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column {
                                sectionActions.forEach { action ->
                                    ListItem(
                                        modifier = Modifier.fillMaxWidth().clickable { onAction(action.id) },
                                        headlineContent = { Text(action.title) },
                                        supportingContent = { Text(action.summary) },
                                    )
                                    if (sectionFields.isNotEmpty() || action != sectionActions.last()) HorizontalDivider(Modifier.padding(start = 16.dp))
                                }
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
                confirmButton = { TextButton(onClick = { selected = null }) { Text(stringResource(DesignR.string.close)) } },
            )
            ConfigFieldEditor.Text, ConfigFieldEditor.Lines -> ConfigTextEditor(field, onDismiss = { selected = null })
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(stringResource(DesignR.string.aurora_override_reset_title)) },
        text = { Text(stringResource(DesignR.string.aurora_override_reset_scope)) },
        confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text(stringResource(DesignR.string.reset)) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(DesignR.string.cancel)) } },
    )
}

@Composable
private fun ConfigTextEditor(field: ConfigField, onDismiss: () -> Unit) {
    var value by remember(field) { mutableStateOf(field.initialText) }
    var revealSensitiveValue by remember(field) { mutableStateOf(false) }
    val error = field.inputError?.invoke(value)
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
                isError = error != null,
                visualTransformation = if (field.sensitive && !revealSensitiveValue) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                trailingIcon = if (field.sensitive) {
                    {
                        TextButton(onClick = { revealSensitiveValue = !revealSensitiveValue }) {
                            Text(stringResource(
                                if (revealSensitiveValue) DesignR.string.aurora_override_hide_value
                                else DesignR.string.aurora_override_show_value
                            ))
                        }
                    }
                } else null,
                supportingText = {
                    val hint = if (field.editor == ConfigFieldEditor.Lines) {
                        stringResource(DesignR.string.aurora_override_lines_hint)
                    } else {
                        stringResource(DesignR.string.aurora_override_text_hint)
                    }
                    Text(error ?: field.supportingText ?: hint)
                },
            )
        },
        confirmButton = {
            TextButton(enabled = error == null, onClick = { field.onSet(value); onDismiss() }) {
                Text(stringResource(DesignR.string.save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { field.onSet(null); onDismiss() }) {
                    Text(stringResource(DesignR.string.aurora_override_restore_unmodified))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.cancel)) }
            }
        },
    )
}
