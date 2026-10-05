package com.koude.aurora.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.service.model.AccessControlMode
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
    val accessControlMode: AccessControlMode = AccessControlMode.AcceptAll,
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
    onAccessControlModeChanged: (AccessControlMode) -> Unit,
    onOpenAccessControl: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selection by remember { mutableStateOf<NetworkSettingSelection?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuroraDetailTopBar(title = stringResource(DesignR.string.network), onBack = onBack)
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
                NetworkSection(stringResource(DesignR.string.vpn_service_options)) {
                    NetworkSwitchRow(
                        stringResource(DesignR.string.route_system_traffic),
                        state.routeSystemTraffic,
                        enabled = !state.running,
                        onChanged = onRouteSystemTrafficChanged,
                        icon = { Icon(painterResource(DesignR.drawable.ic_baseline_vpn_lock), contentDescription = null) },
                    )
                    NetworkDivider()
                    NetworkSwitchRow(
                        stringResource(DesignR.string.bypass_private_network),
                        state.bypassPrivateNetwork,
                        enabled = !state.running && state.routeSystemTraffic,
                        onChanged = onBypassPrivateNetworkChanged,
                        icon = { Icon(painterResource(DesignR.drawable.ic_baseline_vpn_lock), contentDescription = null) },
                    )
                    NetworkDivider()
                    NetworkSwitchRow(
                        stringResource(DesignR.string.dns_hijacking),
                        state.dnsHijacking,
                        enabled = !state.running && state.routeSystemTraffic,
                        onChanged = onDnsHijackingChanged,
                        icon = { Icon(painterResource(DesignR.drawable.ic_baseline_dns), contentDescription = null) },
                    )
                    NetworkDivider()
                    NetworkSwitchRow(
                        stringResource(DesignR.string.allow_bypass),
                        state.allowBypass,
                        enabled = !state.running && state.routeSystemTraffic,
                        onChanged = onAllowBypassChanged,
                        icon = { Icon(painterResource(DesignR.drawable.ic_baseline_vpn_lock), contentDescription = null) },
                    )
                    NetworkDivider()
                    NetworkSwitchRow(
                        stringResource(DesignR.string.allow_ipv6),
                        state.allowIpv6,
                        enabled = !state.running && state.routeSystemTraffic,
                        onChanged = onAllowIpv6Changed,
                        icon = { Icon(painterResource(DesignR.drawable.ic_baseline_vpn_lock), contentDescription = null) },
                    )
                    if (state.showSystemProxy) {
                        NetworkDivider()
                        NetworkSwitchRow(
                            stringResource(DesignR.string.system_proxy),
                            state.systemProxy,
                            enabled = !state.running && state.routeSystemTraffic,
                            onChanged = onSystemProxyChanged,
                            icon = { Icon(painterResource(DesignR.drawable.ic_baseline_vpn_lock), contentDescription = null) },
                        )
                    }
                    NetworkDivider()
                    NetworkChoiceRow(
                        title = stringResource(DesignR.string.tun_stack_mode),
                        value = stackLabel(state.tunStack),
                        enabled = !state.running && state.routeSystemTraffic,
                        icon = { Icon(painterResource(DesignR.drawable.ic_baseline_vpn_lock), contentDescription = null) },
                        onClick = { selection = NetworkSettingSelection.TunStack },
                    )
                }
            }

            item {
                NetworkSection(stringResource(DesignR.string.access_control_packages)) {
                    NetworkChoiceRow(
                        title = stringResource(DesignR.string.access_control_mode),
                        value = accessControlLabel(state.accessControlMode),
                        enabled = !state.running && state.routeSystemTraffic,
                        icon = { Icon(painterResource(DesignR.drawable.ic_baseline_apps), contentDescription = null) },
                        onClick = { selection = NetworkSettingSelection.AccessMode },
                    )
                    NetworkDivider()
                    NetworkChoiceRow(
                        title = stringResource(DesignR.string.access_control_packages),
                        value = null,
                        enabled = !state.running && state.routeSystemTraffic,
                        icon = { Icon(painterResource(DesignR.drawable.ic_baseline_apps), contentDescription = null) },
                        onClick = onOpenAccessControl,
                    )
                }
            }
        }
    }

    when (selection) {
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
        NetworkSettingSelection.AccessMode -> ChoiceDialog(
            title = stringResource(DesignR.string.access_control_mode),
            selected = state.accessControlMode.name,
            options = listOf(
                AccessControlMode.AcceptAll.name to stringResource(DesignR.string.allow_all_apps),
                AccessControlMode.AcceptSelected.name to stringResource(DesignR.string.allow_selected_apps),
                AccessControlMode.DenySelected.name to stringResource(DesignR.string.deny_selected_apps),
            ),
            onSelected = { value ->
                onAccessControlModeChanged(AccessControlMode.valueOf(value))
                selection = null
            },
            onDismiss = { selection = null },
        )
        null -> Unit
    }
}

private enum class NetworkSettingSelection { TunStack, AccessMode }

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
    icon: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChanged(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkIcon(icon)
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
        )
        Switch(checked = checked, onCheckedChange = onChanged, enabled = enabled)
    }
}

@Composable
private fun NetworkChoiceRow(
    title: String,
    value: String?,
    enabled: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkIcon(icon)
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
        )
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NetworkIcon(icon: @Composable () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.size(36.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center, content = { icon() })
    }
}

@Composable
private fun NetworkDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 62.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
                            .clickable { onSelected(value) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        RadioButton(selected = selected == value, onClick = { onSelected(value) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun stackLabel(value: String): String = when (value) {
    "gvisor" -> stringResource(DesignR.string.tun_stack_gvisor)
    "mixed" -> stringResource(DesignR.string.tun_stack_mixed)
    "mips" -> stringResource(DesignR.string.tun_stack_mips)
    else -> stringResource(DesignR.string.tun_stack_system)
}

@Composable
private fun accessControlLabel(value: AccessControlMode): String = when (value) {
    AccessControlMode.AcceptAll -> stringResource(DesignR.string.allow_all_apps)
    AccessControlMode.AcceptSelected -> stringResource(DesignR.string.allow_selected_apps)
    AccessControlMode.DenySelected -> stringResource(DesignR.string.deny_selected_apps)
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
            onAccessControlModeChanged = {},
            onOpenAccessControl = {},
            onBack = {},
        )
    }
}
