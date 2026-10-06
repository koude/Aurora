package com.koude.aurora.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.components.AuroraDetailTopBar
import com.koude.aurora.ui.components.AuroraSectionTitle

data class NetworkSettingsUiState(
    val running: Boolean = false,
    val routeSystemTraffic: Boolean = true,
    val bypassPrivateNetwork: Boolean = true,
    val dnsHijacking: Boolean = true,
    val allowBypass: Boolean = true,
    val allowIpv6: Boolean = false,
    val systemProxy: Boolean = true,
    val tunStack: String = "system",
    val showSystemProxy: Boolean = true,
)

@Composable
fun NetworkSettingsScreen(
    state: NetworkSettingsUiState,
    onRouteSystemTrafficChanged: (Boolean) -> Unit,
    onBypassPrivateNetworkChanged: (Boolean) -> Unit,
    onDnsHijackingChanged: (Boolean) -> Unit,
    onAllowBypassChanged: (Boolean) -> Unit,
    onAllowIpv6Changed: (Boolean) -> Unit,
    onSystemProxyChanged: (Boolean) -> Unit,
    onTunStackChanged: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selection by remember { mutableStateOf<NetworkSettingSelection?>(null) }
    var confirmManualMode by remember { mutableStateOf(false) }
    val vpnSettingsEnabled = !state.running && state.routeSystemTraffic

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuroraDetailTopBar(title = stringResource(DesignR.string.aurora_network_vpn_settings), onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 12.dp,
                end = 20.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NetworkSection(stringResource(DesignR.string.aurora_network_connection)) {
                    NetworkChoiceRow(
                        title = stringResource(DesignR.string.aurora_proxy_method),
                        value = stringResource(if (state.routeSystemTraffic)
                            DesignR.string.aurora_proxy_method_auto else DesignR.string.aurora_proxy_method_manual),
                        enabled = !state.running,
                        onClick = { selection = NetworkSettingSelection.ProxyMethod },
                    )
                }
            }
            if (state.running || !state.routeSystemTraffic) {
                item {
                    Text(
                        stringResource(if (state.running)
                            DesignR.string.aurora_network_stop_to_edit else DesignR.string.aurora_network_vpn_required),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                NetworkSection(stringResource(DesignR.string.aurora_network_vpn_traffic)) {
                    NetworkSwitchRow(
                        stringResource(DesignR.string.bypass_private_network),
                        state.bypassPrivateNetwork,
                        enabled = vpnSettingsEnabled,
                        onChanged = onBypassPrivateNetworkChanged,
                    )
                    NetworkDivider()
                    NetworkSwitchRow(
                        stringResource(DesignR.string.aurora_network_dns_capture),
                        state.dnsHijacking,
                        enabled = vpnSettingsEnabled,
                        onChanged = onDnsHijackingChanged,
                    )
                    NetworkDivider()
                    NetworkSwitchRow(
                        stringResource(DesignR.string.allow_ipv6),
                        state.allowIpv6,
                        enabled = vpnSettingsEnabled,
                        onChanged = onAllowIpv6Changed,
                    )
                }
            }
            item {
                NetworkSection(stringResource(DesignR.string.aurora_network_advanced)) {
                    if (state.showSystemProxy) {
                        NetworkSwitchRow(
                            stringResource(DesignR.string.aurora_network_system_http_proxy),
                            state.systemProxy,
                            enabled = vpnSettingsEnabled,
                            onChanged = onSystemProxyChanged,
                        )
                        NetworkDivider()
                    }
                    NetworkSwitchRow(
                        stringResource(DesignR.string.aurora_network_allow_app_bypass),
                        state.allowBypass,
                        enabled = vpnSettingsEnabled,
                        onChanged = onAllowBypassChanged,
                    )
                    NetworkDivider()
                    NetworkChoiceRow(
                        title = stringResource(DesignR.string.tun_stack_mode),
                        value = stackLabel(state.tunStack),
                        enabled = vpnSettingsEnabled,
                        onClick = { selection = NetworkSettingSelection.TunStack },
                    )
                }
            }
        }
    }

    when (selection) {
        NetworkSettingSelection.ProxyMethod -> ProxyMethodDialog(
            useVpn = state.routeSystemTraffic,
            onSelectAuto = {
                selection = null
                if (!state.routeSystemTraffic) onRouteSystemTrafficChanged(true)
            },
            onSelectManual = {
                selection = null
                if (state.routeSystemTraffic) confirmManualMode = true
            },
            onDismiss = { selection = null },
        )
        NetworkSettingSelection.TunStack -> ChoiceDialog(
            title = stringResource(DesignR.string.tun_stack_mode),
            selected = state.tunStack,
            options = listOf(
                "system" to stringResource(DesignR.string.tun_stack_system),
                "gvisor" to stringResource(DesignR.string.tun_stack_gvisor),
                "mixed" to stringResource(DesignR.string.tun_stack_mixed),
                "mips" to stringResource(DesignR.string.tun_stack_mips),
            ),
            onSelected = { onTunStackChanged(it); selection = null },
            onDismiss = { selection = null },
        )
        null -> Unit
    }
    if (confirmManualMode) {
        AlertDialog(
            onDismissRequest = { confirmManualMode = false },
            title = { Text(stringResource(DesignR.string.aurora_proxy_method_manual_warning_title)) },
            text = { Text(stringResource(DesignR.string.aurora_proxy_method_manual_warning)) },
            confirmButton = {
                TextButton(onClick = { confirmManualMode = false; onRouteSystemTrafficChanged(false) }) {
                    Text(stringResource(DesignR.string.aurora_proxy_method_manual_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmManualMode = false }) {
                    Text(stringResource(DesignR.string.cancel))
                }
            },
        )
    }
}

private enum class NetworkSettingSelection { ProxyMethod, TunStack }

@Composable
private fun NetworkSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AuroraSectionTitle(title, Modifier.padding(start = 8.dp))
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(content = { content() })
        }
    }
}

@Composable
private fun NetworkSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChanged(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun NetworkChoiceRow(
    title: String,
    value: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
        )
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .38f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NetworkDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ProxyMethodDialog(
    useVpn: Boolean,
    onSelectAuto: () -> Unit,
    onSelectManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(DesignR.string.aurora_proxy_method)) },
        text = {
            Column {
                listOf(
                    Triple(true, DesignR.string.aurora_proxy_method_auto, DesignR.string.aurora_proxy_method_auto_summary),
                    Triple(false, DesignR.string.aurora_proxy_method_manual, DesignR.string.aurora_proxy_method_manual_summary),
                ).forEach { (vpn, label, summary) ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .selectable(selected = useVpn == vpn, role = Role.RadioButton) {
                                if (vpn) onSelectAuto() else onSelectManual()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = useVpn == vpn, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.close)) } },
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected == value, role = Role.RadioButton) { onSelected(value) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        RadioButton(selected = selected == value, onClick = null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.close)) } },
    )
}

@Composable
private fun stackLabel(value: String): String = when (value) {
    "gvisor" -> stringResource(DesignR.string.tun_stack_gvisor)
    "mixed" -> stringResource(DesignR.string.tun_stack_mixed)
    "mips" -> stringResource(DesignR.string.tun_stack_mips)
    else -> stringResource(DesignR.string.tun_stack_system)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NetworkSettingsScreenPreview() {
    AuroraTheme {
        NetworkSettingsScreen(
            state = NetworkSettingsUiState(),
            onRouteSystemTrafficChanged = {},
            onBypassPrivateNetworkChanged = {},
            onDnsHijackingChanged = {},
            onAllowBypassChanged = {},
            onAllowIpv6Changed = {},
            onSystemProxyChanged = {},
            onTunStackChanged = {},
            onBack = {},
        )
    }
}
