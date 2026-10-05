package com.koude.aurora.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.github.kr328.clash.design.model.DarkMode
import com.koude.aurora.ui.components.AuroraDetailTopBar
import com.koude.aurora.ui.components.AuroraSectionTitle

data class AppSettingsUiState(
    val autoRestart: Boolean = false,
    val darkMode: DarkMode = DarkMode.Auto,
    val hideAppIcon: Boolean = false,
    val hideFromRecents: Boolean = false,
    val showTraffic: Boolean = true,
    val running: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    state: AppSettingsUiState,
    onAutoRestartChanged: (Boolean) -> Unit,
    onDarkModeChanged: (DarkMode) -> Unit,
    onHideAppIconChanged: (Boolean) -> Unit,
    onHideFromRecentsChanged: (Boolean) -> Unit,
    onShowTrafficChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var darkModeDialog by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            AuroraDetailTopBar(title = stringResource(DesignR.string.app), onBack = onBack)
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SettingsSection(stringResource(DesignR.string.behavior)) {
                    AppSwitchRow(DesignR.string.auto_restart, DesignR.string.allow_clash_auto_restart, state.autoRestart, onAutoRestartChanged)
                }
            }
            item {
                SettingsSection(stringResource(DesignR.string.interface_)) {
                    ListItem(
                        modifier = Modifier.fillMaxWidth().clickable { darkModeDialog = true },
                        leadingContent = { Icon(painterResource(DesignR.drawable.ic_baseline_brightness_4), contentDescription = null) },
                        headlineContent = { Text(stringResource(DesignR.string.dark_mode)) },
                        supportingContent = { Text(darkModeLabel(state.darkMode)) },
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp))
                    AppSwitchRow(DesignR.string.hide_app_icon_title, DesignR.string.hide_app_icon_desc, state.hideAppIcon, onHideAppIconChanged)
                    HorizontalDivider(Modifier.padding(start = 56.dp))
                    AppSwitchRow(DesignR.string.hide_from_recents_title, DesignR.string.hide_from_recents_desc, state.hideFromRecents, onHideFromRecentsChanged)
                }
            }
            item {
                SettingsSection(stringResource(DesignR.string.service)) {
                    AppSwitchRow(DesignR.string.show_traffic, DesignR.string.show_traffic_summary, state.showTraffic, onShowTrafficChanged, enabled = !state.running)
                }
            }
        }
    }

    if (darkModeDialog) AlertDialog(
        onDismissRequest = { darkModeDialog = false },
        title = { Text(stringResource(DesignR.string.dark_mode)) },
        text = {
            Column {
                DarkMode.values().forEach { mode ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.darkMode == mode, onClick = { onDarkModeChanged(mode); darkModeDialog = false })
                        Text(darkModeLabel(mode), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { darkModeDialog = false }) { Text(stringResource(DesignR.string.cancel)) } },
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        AuroraSectionTitle(title, Modifier.padding(start = 4.dp, bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(content = content)
        }
    }
}

@Composable
private fun AppSwitchRow(title: Int, summary: Int, checked: Boolean, onChanged: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(summary)) },
        trailingContent = { Switch(checked, onCheckedChange = onChanged, enabled = enabled) },
    )
}

@Composable
private fun darkModeLabel(mode: DarkMode): String = stringResource(when (mode) {
    DarkMode.Auto -> DesignR.string.follow_system_android_10
    DarkMode.ForceLight -> DesignR.string.always_light
    DarkMode.ForceDark -> DesignR.string.always_dark
})
