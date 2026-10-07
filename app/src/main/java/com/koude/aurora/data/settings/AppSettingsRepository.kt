package com.koude.aurora.data.settings

import android.content.Context
import android.content.pm.PackageManager
import com.github.kr328.clash.RestartReceiver
import com.github.kr328.clash.common.util.componentName
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.store.UiStore.Companion.mainActivityAlias
import com.github.kr328.clash.service.store.ServiceStore

data class AppSettingsSnapshot(
    val autoRestart: Boolean,
    val darkMode: DarkMode,
    val hideAppIcon: Boolean,
    val hideFromRecents: Boolean,
    val showTraffic: Boolean,
)

interface AppSettingsRepository {
    fun read(): AppSettingsSnapshot
    fun setAutoRestart(value: Boolean)
    fun setDarkMode(value: DarkMode)
    fun setHideAppIcon(value: Boolean)
    fun setHideFromRecents(value: Boolean)
    fun setShowTraffic(value: Boolean)
}

class StoredAppSettingsRepository(context: Context) : AppSettingsRepository {
    private val appContext = context.applicationContext
    private val uiStore = UiStore(appContext)
    private val serviceStore = ServiceStore(appContext)

    override fun read() = AppSettingsSnapshot(
        autoRestart = appContext.packageManager.getComponentEnabledSetting(RestartReceiver::class.componentName) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        darkMode = uiStore.darkMode,
        hideAppIcon = uiStore.hideAppIcon,
        hideFromRecents = uiStore.hideFromRecents,
        showTraffic = serviceStore.dynamicNotification,
    )

    override fun setAutoRestart(value: Boolean) {
        val state = if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        appContext.packageManager.setComponentEnabledSetting(
            RestartReceiver::class.componentName, state, PackageManager.DONT_KILL_APP,
        )
    }

    override fun setDarkMode(value: DarkMode) { uiStore.darkMode = value }

    override fun setHideAppIcon(value: Boolean) {
        val state = if (value) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        appContext.packageManager.setComponentEnabledSetting(
            appContext.mainActivityAlias, state, PackageManager.DONT_KILL_APP,
        )
        uiStore.hideAppIcon = value
    }

    override fun setHideFromRecents(value: Boolean) { uiStore.hideFromRecents = value }

    override fun setShowTraffic(value: Boolean) { serviceStore.dynamicNotification = value }
}
