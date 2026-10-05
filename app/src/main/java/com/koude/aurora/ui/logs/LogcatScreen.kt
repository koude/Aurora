package com.koude.aurora.ui.logs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.design.util.format

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogcatScreen(
    messages: List<LogMessage>,
    streaming: Boolean,
    exportProgress: Int? = null,
    exportProgressMax: Int = 0,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onCopy: (LogMessage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (streaming) DesignR.string.clash_logcat else DesignR.string.logcat), fontWeight = FontWeight.Medium) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                actions = {
                    if (streaming) {
                        IconButton(onClick = onClose) { Icon(painterResource(DesignR.drawable.ic_baseline_stop), contentDescription = stringResource(DesignR.string.close)) }
                    } else {
                        IconButton(onClick = onDelete) { Icon(painterResource(DesignR.drawable.ic_baseline_delete), contentDescription = stringResource(DesignR.string.delete)) }
                        IconButton(onClick = onExport, enabled = exportProgress == null) { Icon(painterResource(DesignR.drawable.ic_baseline_publish), contentDescription = stringResource(DesignR.string.export)) }
                    }
                },
            )
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            if (exportProgress != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                    Text("正在导出日志 ${exportProgress}/${exportProgressMax}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = streaming,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(messages, key = { index, message -> "${message.time.time}-${message.level}-$index" }) { _, message ->
                    Column(
                        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { onCopy(message) }).padding(vertical = 4.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(message.level.name, style = MaterialTheme.typography.labelMedium, color = levelColor(message.level))
                            Text(message.time.format(androidx.compose.ui.platform.LocalContext.current, includeDate = false, includeTime = true), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(message.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun levelColor(level: LogMessage.Level) = when (level) {
    LogMessage.Level.Error -> MaterialTheme.colorScheme.error
    LogMessage.Level.Warning -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}
