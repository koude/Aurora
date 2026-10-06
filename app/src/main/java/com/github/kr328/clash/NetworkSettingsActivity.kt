package com.github.kr328.clash

import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.service.store.ServiceStore
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.settings.NetworkSettingsScreen
import com.koude.aurora.ui.settings.NetworkSettingsUiState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class NetworkSettingsActivity : BaseActivity() {
    override suspend fun main() {
        val serviceStore = ServiceStore(this)
        val screenState = mutableStateOf(readState(serviceStore))

        setContent {
            AuroraTheme {
                NetworkSettingsScreen(
                    state = screenState.value,
                    onRouteSystemTrafficChanged = {
                        uiStore.enableVpn = it
                        screenState.value = screenState.value.copy(routeSystemTraffic = it)
                    },
                    onBypassPrivateNetworkChanged = {
                        serviceStore.bypassPrivateNetwork = it
                        screenState.value = screenState.value.copy(bypassPrivateNetwork = it)
                    },
                    onDnsHijackingChanged = {
                        serviceStore.dnsHijacking = it
                        screenState.value = screenState.value.copy(dnsHijacking = it)
                    },
                    onAllowBypassChanged = {
                        serviceStore.allowBypass = it
                        screenState.value = screenState.value.copy(allowBypass = it)
                    },
                    onAllowIpv6Changed = {
                        serviceStore.allowIpv6 = it
                        screenState.value = screenState.value.copy(allowIpv6 = it)
                    },
                    onSystemProxyChanged = {
                        serviceStore.systemProxy = it
                        screenState.value = screenState.value.copy(systemProxy = it)
                    },
                    onTunStackChanged = {
                        serviceStore.tunStackMode = it
                        screenState.value = screenState.value.copy(tunStack = it)
                    },
                    onOpenPerAppProxy = { startActivity(AccessControlActivity::class.intent) },
                    onBack = ::finish,
                )
            }
        }

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ClashStart, Event.ClashStop, Event.ServiceRecreated -> {
                            screenState.value = readState(serviceStore)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun readState(serviceStore: ServiceStore) = NetworkSettingsUiState(
        running = clashRunning,
        routeSystemTraffic = uiStore.enableVpn,
        bypassPrivateNetwork = serviceStore.bypassPrivateNetwork,
        dnsHijacking = serviceStore.dnsHijacking,
        allowBypass = serviceStore.allowBypass,
        allowIpv6 = serviceStore.allowIpv6,
        systemProxy = serviceStore.systemProxy,
        tunStack = serviceStore.tunStackMode,
        showSystemProxy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
    )
}
