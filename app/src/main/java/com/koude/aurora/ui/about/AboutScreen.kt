package com.koude.aurora.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR
import com.koude.aurora.ui.components.AuroraCardStyle
import com.koude.aurora.ui.components.AuroraDetailTopBar

object AboutLinks {
    const val PROJECT = "https://github.com/koude/Aurora"
    const val RELEASES = "$PROJECT/releases"
    const val LICENSE = "$PROJECT/blob/main/LICENSE"
    const val NOTICE = "$PROJECT/blob/main/NOTICE"
    const val CMFA = "https://github.com/MetaCubeX/ClashMetaForAndroid"
    const val MIHOMO = "https://github.com/MetaCubeX/mihomo"
}

@Composable
fun AboutScreen(
    versionName: String,
    versionCode: Int,
    onOpenLink: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuroraDetailTopBar(title = stringResource(DesignR.string.about), onBack = onBack) },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(DesignR.string.aurora_brand_name),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(DesignR.string.aurora_about_version, versionName, versionCode),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(DesignR.string.aurora_about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                AboutGroup {
                    AboutLinkRow(stringResource(DesignR.string.aurora_about_project), AboutLinks.PROJECT, onOpenLink)
                    AboutDivider()
                    AboutLinkRow(stringResource(DesignR.string.aurora_about_releases), AboutLinks.RELEASES, onOpenLink)
                }
            }
            item {
                Text(
                    text = stringResource(DesignR.string.aurora_about_open_source),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                AboutGroup {
                    AboutLinkRow("Clash Meta for Android", AboutLinks.CMFA, onOpenLink)
                    AboutDivider()
                    AboutLinkRow("mihomo", AboutLinks.MIHOMO, onOpenLink)
                    AboutDivider()
                    AboutLinkRow(stringResource(DesignR.string.aurora_about_license), AboutLinks.LICENSE, onOpenLink)
                    AboutDivider()
                    AboutLinkRow(stringResource(DesignR.string.aurora_about_notice), AboutLinks.NOTICE, onOpenLink)
                }
            }
        }
    }
}

@Composable
private fun AboutGroup(content: @Composable () -> Unit) {
    Card(
        shape = AuroraCardStyle.groupShape(),
        colors = CardDefaults.cardColors(containerColor = AuroraCardStyle.groupColor()),
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun AboutLinkRow(title: String, url: String, onOpenLink: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenLink(url) }
            .padding(horizontal = 16.dp, vertical = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text("↗", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
