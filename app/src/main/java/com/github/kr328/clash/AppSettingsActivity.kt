package com.github.kr328.clash

import android.content.pm.PackageManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.github.kr328.clash.common.util.componentName
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.Behavior
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore.Companion.mainActivityAlias
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.util.ApplicationObserver
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.settings.AppSettingsScreen
import com.koude.aurora.ui.settings.AppSettingsUiState
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AppSettingsActivity : BaseActivity<Design<*>>(), Behavior {
    override suspend fun main() {
        val serviceStore = ServiceStore(this)
        val screenState = mutableStateOf(readState(serviceStore))

        setContent {
            AuroraTheme {
                AppSettingsScreen(
                    state = screenState.value,
                    onAutoRestartChanged = { autoRestart = it; screenState.value = screenState.value.copy(autoRestart = it) },
                    onDarkModeChanged = {
                        uiStore.darkMode = it
                        screenState.value = screenState.value.copy(darkMode = it)
                        ApplicationObserver.createdActivities.forEach { activity -> activity.recreate() }
                    },
                    onHideAppIconChanged = {
                        uiStore.hideAppIcon = it
                        onHideIconChange(it)
                        screenState.value = screenState.value.copy(hideAppIcon = it)
                    },
                    onHideFromRecentsChanged = {
                        uiStore.hideFromRecents = it
                        screenState.value = screenState.value.copy(hideFromRecents = it)
                        ApplicationObserver.createdActivities.forEach { activity -> activity.recreate() }
                    },
                    onShowTrafficChanged = {
                        serviceStore.dynamicNotification = it
                        screenState.value = screenState.value.copy(showTraffic = it)
                    },
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

    override var autoRestart: Boolean
        get() = packageManager.getComponentEnabledSetting(RestartReceiver::class.componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        set(value) {
            val status = if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            packageManager.setComponentEnabledSetting(RestartReceiver::class.componentName, status, PackageManager.DONT_KILL_APP)
        }

    private fun onHideIconChange(hide: Boolean) {
        val newState = if (hide) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        packageManager.setComponentEnabledSetting(mainActivityAlias, newState, PackageManager.DONT_KILL_APP)
    }

    private fun readState(serviceStore: ServiceStore) = AppSettingsUiState(
        autoRestart = autoRestart,
        darkMode = uiStore.darkMode,
        hideAppIcon = uiStore.hideAppIcon,
        hideFromRecents = uiStore.hideFromRecents,
        showTraffic = serviceStore.dynamicNotification,
        running = clashRunning,
    )
}
