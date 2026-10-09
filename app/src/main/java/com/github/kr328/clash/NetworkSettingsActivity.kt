package com.github.kr328.clash

import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.github.kr328.clash.common.util.intent
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.settings.NetworkSettingsScreen
import com.koude.aurora.ui.settings.NetworkSettingsViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class NetworkSettingsActivity : BaseActivity() {
    private val viewModel: NetworkSettingsViewModel by viewModels {
        NetworkSettingsViewModel.factory(applicationContext)
    }

    override suspend fun main() {
        viewModel.refresh(clashRunning)

        setContent {
            AuroraTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor) {
                val screenState by viewModel.uiState.collectAsStateWithLifecycle()
                NetworkSettingsScreen(
                    state = screenState,
                    onRouteSystemTrafficChanged = viewModel::setRouteSystemTraffic,
                    onBypassPrivateNetworkChanged = viewModel::setBypassPrivateNetwork,
                    onDnsHijackingChanged = viewModel::setDnsHijacking,
                    onAllowBypassChanged = viewModel::setAllowBypass,
                    onAllowIpv6Changed = viewModel::setAllowIpv6,
                    onSystemProxyChanged = viewModel::setSystemProxy,
                    onTunStackChanged = viewModel::setTunStack,
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
                            viewModel.refresh(clashRunning)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

}
