package com.koude.aurora.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.DialerReceiver
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.design.model.DarkMode
import com.koude.aurora.ui.components.AuroraCardStyle
import com.koude.aurora.ui.components.AuroraDetailTopBar

data class AppSettingsUiState(
    val autoRestart: Boolean = false,
    val darkMode: DarkMode = DarkMode.Auto,
    val hideAppIcon: Boolean = false,
    val hideFromRecents: Boolean = false,
    val showTraffic: Boolean = true,
    val running: Boolean = false,
)

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
    var hideIconDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuroraDetailTopBar(title = stringResource(DesignR.string.aurora_settings_general), onBack = onBack)
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp,
            ),
        ) {
            item {
                Card(
                    shape = AuroraCardStyle.groupShape(),
                    colors = CardDefaults.cardColors(containerColor = AuroraCardStyle.groupColor()),
                ) {
                    Column {
                        GeneralSwitchRow(
                            title = stringResource(DesignR.string.auto_restart),
                            checked = state.autoRestart,
                            onChanged = onAutoRestartChanged,
                        )
                        GeneralDivider()
                        GeneralChoiceRow(
                            title = stringResource(DesignR.string.dark_mode),
                            value = darkModeLabel(state.darkMode),
                            onClick = { darkModeDialog = true },
                        )
                        GeneralDivider()
                        GeneralSwitchRow(
                            title = stringResource(DesignR.string.hide_app_icon_title),
                            checked = state.hideAppIcon,
                            onChanged = { hide ->
                                if (hide) hideIconDialog = true else onHideAppIconChanged(false)
                            },
                        )
                        GeneralDivider()
                        GeneralSwitchRow(
                            title = stringResource(DesignR.string.hide_from_recents_title),
                            checked = state.hideFromRecents,
                            onChanged = onHideFromRecentsChanged,
                        )
                        GeneralDivider()
                        GeneralSwitchRow(
                            title = stringResource(DesignR.string.aurora_general_notification_traffic),
                            checked = state.showTraffic,
                            enabled = !state.running,
                            onChanged = onShowTrafficChanged,
                        )
                    }
                }
            }
        }
    }

    if (darkModeDialog) {
        AlertDialog(
            onDismissRequest = { darkModeDialog = false },
            title = { Text(stringResource(DesignR.string.dark_mode)) },
            text = {
                Column {
                    DarkMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = state.darkMode == mode,
                                role = Role.RadioButton,
                                onClick = { onDarkModeChanged(mode); darkModeDialog = false },
                            ).padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.darkMode == mode, onClick = null)
                            Text(darkModeLabel(mode), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { darkModeDialog = false }) {
                    Text(stringResource(DesignR.string.cancel))
                }
            },
        )
    }

    if (hideIconDialog) {
        AlertDialog(
            onDismissRequest = { hideIconDialog = false },
            title = { Text(stringResource(DesignR.string.hide_app_icon_title)) },
            text = {
                Column {
                    Text(stringResource(DesignR.string.aurora_hide_icon_warning))
                    Text(
                        DialerReceiver.DIAL_CODE,
                        modifier = Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(stringResource(DesignR.string.aurora_hide_icon_dialer_note))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val copied = runCatching {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Aurora dial code", DialerReceiver.DIAL_CODE)
                        )
                    }.isSuccess
                    if (copied) {
                        hideIconDialog = false
                        onHideAppIconChanged(true)
                    } else {
                        Toast.makeText(context, DesignR.string.aurora_hide_icon_copy_failed, Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text(stringResource(DesignR.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { hideIconDialog = false }) {
                    Text(stringResource(DesignR.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun GeneralSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChanged(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun GeneralChoiceRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.padding(start = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GeneralDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun darkModeLabel(mode: DarkMode): String = stringResource(when (mode) {
    DarkMode.Auto -> DesignR.string.follow_system_android_10
    DarkMode.ForceLight -> DesignR.string.always_light
    DarkMode.ForceDark -> DesignR.string.always_dark
})
