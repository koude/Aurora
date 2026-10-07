package com.koude.aurora.data.settings

import android.content.Context
import android.os.Build
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.service.store.ServiceStore

data class NetworkSettingsSnapshot(
    val routeSystemTraffic: Boolean,
    val bypassPrivateNetwork: Boolean,
    val dnsHijacking: Boolean,
    val allowBypass: Boolean,
    val allowIpv6: Boolean,
    val systemProxy: Boolean,
    val tunStack: String,
    val showSystemProxy: Boolean,
)

interface NetworkSettingsRepository {
    fun read(): NetworkSettingsSnapshot
    fun setRouteSystemTraffic(value: Boolean)
    fun setBypassPrivateNetwork(value: Boolean)
    fun setDnsHijacking(value: Boolean)
    fun setAllowBypass(value: Boolean)
    fun setAllowIpv6(value: Boolean)
    fun setSystemProxy(value: Boolean)
    fun setTunStack(value: String)
}

class StoredNetworkSettingsRepository(context: Context) : NetworkSettingsRepository {
    private val uiStore = UiStore(context.applicationContext)
    private val serviceStore = ServiceStore(context.applicationContext)

    override fun read() = NetworkSettingsSnapshot(
        routeSystemTraffic = uiStore.enableVpn,
        bypassPrivateNetwork = serviceStore.bypassPrivateNetwork,
        dnsHijacking = serviceStore.dnsHijacking,
        allowBypass = serviceStore.allowBypass,
        allowIpv6 = serviceStore.allowIpv6,
        systemProxy = serviceStore.systemProxy,
        tunStack = serviceStore.tunStackMode,
        showSystemProxy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
    )

    override fun setRouteSystemTraffic(value: Boolean) { uiStore.enableVpn = value }
    override fun setBypassPrivateNetwork(value: Boolean) { serviceStore.bypassPrivateNetwork = value }
    override fun setDnsHijacking(value: Boolean) { serviceStore.dnsHijacking = value }
    override fun setAllowBypass(value: Boolean) { serviceStore.allowBypass = value }
    override fun setAllowIpv6(value: Boolean) { serviceStore.allowIpv6 = value }
    override fun setSystemProxy(value: Boolean) { serviceStore.systemProxy = value }
    override fun setTunStack(value: String) { serviceStore.tunStackMode = value }
}
