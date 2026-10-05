package com.koude.aurora.ui.logs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.design.util.format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    files: List<LogFile>,
    onBack: () -> Unit,
    onStartLogcat: () -> Unit,
    onOpenFile: (LogFile) -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(DesignR.string.logs), fontWeight = FontWeight.Medium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(painterResource(DesignR.drawable.ic_baseline_arrow_back), contentDescription = "返回") } },
                actions = { IconButton(onClick = { confirmDelete = true }, enabled = files.isNotEmpty()) { Icon(painterResource(DesignR.drawable.ic_baseline_clear_all), contentDescription = stringResource(DesignR.string.delete_all_logs)) } },
            )
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onStartLogcat),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    ListItem(
                        leadingContent = { Icon(painterResource(DesignR.drawable.ic_baseline_adb), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                        headlineContent = { Text(stringResource(DesignR.string.clash_logcat), color = MaterialTheme.colorScheme.onPrimaryContainer) },
                        supportingContent = { Text(stringResource(DesignR.string.tap_to_start), color = MaterialTheme.colorScheme.onPrimaryContainer) },
                    )
                }
            }
            item { Text(stringResource(DesignR.string.history), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
            if (files.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("暂无历史日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(files, key = { it.fileName }) { file ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenFile(file) },
                        headlineContent = { Text(file.fileName) },
                        supportingContent = { Text(file.date.format(androidx.compose.ui.platform.LocalContext.current)) },
                    )
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(DesignR.string.delete_all_logs)) },
        text = { Text(stringResource(DesignR.string.delete_all_logs_warn)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteAll() }) { Text(stringResource(DesignR.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(DesignR.string.cancel)) } },
    )
}
