package com.koude.aurora.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR
import com.koude.aurora.ui.components.AuroraDetailTopBar

data class HelpLink(val titleRes: Int, val summaryRes: Int, val url: String)

@Composable
fun HelpScreen(
    documents: List<HelpLink>,
    sources: List<HelpLink>,
    onOpenLink: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AuroraDetailTopBar(title = stringResource(DesignR.string.help), onBack = onBack)
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HelpSection(stringResource(DesignR.string.document), documents, onOpenLink) }
            item { HelpSection(stringResource(DesignR.string.sources), sources, onOpenLink) }
        }
    }
}

@Composable
private fun HelpSection(title: String, links: List<HelpLink>, onOpenLink: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column {
                links.forEachIndexed { index, link ->
                    ListItem(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenLink(link.url) },
                        headlineContent = { Text(stringResource(link.titleRes)) },
                        supportingContent = { Text(stringResource(link.summaryRes)) },
                        trailingContent = { Text("↗", color = MaterialTheme.colorScheme.primary) },
                        leadingContent = null,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    )
                    if (index != links.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}
