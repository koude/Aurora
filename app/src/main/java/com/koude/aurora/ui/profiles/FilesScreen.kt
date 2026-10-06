package com.koude.aurora.ui.profiles

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.koude.aurora.ui.components.AuroraCardStyle
import com.koude.aurora.ui.components.AuroraDetailTopBar
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.design.model.File
import com.github.kr328.clash.design.util.format
import com.github.kr328.clash.design.util.toBytesString
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    files: List<File>,
    currentInBase: Boolean,
    configurationEditable: Boolean,
    onBack: () -> Unit,
    onOpenDirectory: (File) -> Unit,
    onOpenFile: (File) -> Unit,
    onImport: (File?, Uri, String?) -> Unit,
    onExport: (File, Uri) -> Unit,
    onRename: (File, String) -> Unit,
    onDelete: (File) -> Unit,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<File?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var editNameFor by remember { mutableStateOf<File?>(null) }
    var draftName by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<File?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var confirmDelete by remember { mutableStateOf<File?>(null) }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val target = pendingImport
        if (target == null) {
            draftName = uri.displayName(context) ?: "File"
            // The sentinel dialog state means create a new file in this directory.
            editNameFor = NEW_FILE_SENTINEL
            pendingUri = uri
        } else {
            onImport(target, uri, null)
        }
        pendingImport = null
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val file = selected
        if (uri != null && file != null) onExport(file, uri)
        selected = null
    }

    val closeOrGoBack = { if (selected != null) { selected = null; menuExpanded = false } else onBack() }
    BackHandler(onBack = closeOrGoBack)
    Scaffold(topBar = {
        AuroraDetailTopBar(
            title = stringResource(DesignR.string.files),
            onBack = closeOrGoBack,
            actions = {
                if (!currentInBase && files.isNotEmpty()) IconButton(onClick = { pendingImport = null; importPicker.launch("*/*") }) {
                    Icon(painterResource(DesignR.drawable.ic_baseline_add), contentDescription = stringResource(DesignR.string._new))
                }
            },
        )
    }) { insets ->
        if (files.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(insets).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painterResource(DesignR.drawable.ic_outline_folder),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(DesignR.string.aurora_files_empty),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (!currentInBase) {
                    FilledTonalButton(
                        onClick = { pendingImport = null; importPicker.launch("*/*") },
                        modifier = Modifier.padding(top = 20.dp),
                    ) { Text(stringResource(DesignR.string.aurora_files_import)) }
                }
            }
        } else LazyColumn(
            Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(files, key = { it.id }) { file ->
                Card(
                    shape = AuroraCardStyle.groupShape(),
                    colors = CardDefaults.cardColors(containerColor = AuroraCardStyle.groupColor()),
                ) {
                    Row(Modifier.fillMaxWidth().clickable { if (file.isDirectory) onOpenDirectory(file) else onOpenFile(file) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(if (file.isDirectory) DesignR.drawable.ic_outline_folder else DesignR.drawable.ic_outline_article), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(file.name, style = MaterialTheme.typography.bodyLarge)
                            if (!file.isDirectory) Text("${file.size.toBytesString()} · ${Date(file.lastModified).format(context)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { selected = file; menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                            DropdownMenu(expanded = menuExpanded && selected?.id == file.id, onDismissRequest = { menuExpanded = false; selected = null }) {
                                if (!file.isDirectory && (!currentInBase || configurationEditable)) DropdownMenuItem(text = { Text("导入/替换") }, onClick = { menuExpanded = false; selected = null; pendingImport = file; importPicker.launch("*/*") })
                                if (!file.isDirectory && file.size > 0) DropdownMenuItem(text = { Text("导出") }, onClick = { menuExpanded = false; exportPicker.launch(file.name) })
                                if (!currentInBase) {
                                    DropdownMenuItem(text = { Text("重命名") }, onClick = { menuExpanded = false; selected = null; draftName = file.name; editNameFor = file })
                                    DropdownMenuItem(text = { Text("删除", color = MaterialTheme.colorScheme.error) }, onClick = { menuExpanded = false; selected = null; confirmDelete = file })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val renameTarget = editNameFor
    if (renameTarget != null) AlertDialog(
        onDismissRequest = { editNameFor = null },
        title = { Text(if (renameTarget === NEW_FILE_SENTINEL) "导入文件" else "重命名") },
        text = { OutlinedTextField(value = draftName, onValueChange = { draftName = it }, singleLine = true, label = { Text("文件名") }) },
        confirmButton = { TextButton(onClick = {
            if (draftName.isNotBlank() && !draftName.contains('/') && !draftName.contains('\n')) {
                if (renameTarget === NEW_FILE_SENTINEL) pendingUri?.let { onImport(null, it, draftName) }
                else onRename(renameTarget, draftName)
                editNameFor = null
            }
        }) { Text("确定") } },
        dismissButton = { TextButton(onClick = { editNameFor = null }) { Text("取消") } },
    )
    confirmDelete?.let { file -> AlertDialog(
        onDismissRequest = { confirmDelete = null },
        title = { Text("删除文件？") }, text = { Text(file.name) },
        confirmButton = { TextButton(onClick = { confirmDelete = null; onDelete(file) }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("取消") } },
    ) }
}

private val NEW_FILE_SENTINEL = File("", "", 0, 0, false)
private fun Uri.displayName(context: Context): String? = context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) cursor.getString(0) else null
}
