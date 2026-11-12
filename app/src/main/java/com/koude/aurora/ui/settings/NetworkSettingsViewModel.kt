package com.koude.aurora.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.common.ProxyMethod
import com.github.kr328.clash.service.root.RootCore
import com.github.kr328.clash.util.stopClashService
import com.koude.aurora.data.settings.NetworkSettingsRepository
import com.koude.aurora.data.settings.StoredNetworkSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkSettingsViewModel(
    private val repository: NetworkSettingsRepository,
    private val context: Context,
) : ViewModel() {
    private val rootCore = RootCore(context)
    private val mutableUiState = MutableStateFlow(NetworkSettingsUiState())
    val uiState: StateFlow<NetworkSettingsUiState> = mutableUiState.asStateFlow()

    fun refresh(running: Boolean) {
        val settings = repository.read()
        mutableUiState.value = NetworkSettingsUiState(
            running = running,
            proxyMethod = settings.proxyMethod,
            bypassPrivateNetwork = settings.bypassPrivateNetwork,
            dnsHijacking = settings.dnsHijacking,
            allowBypass = settings.allowBypass,
            allowIpv6 = settings.allowIpv6,
            systemProxy = settings.systemProxy,
            tunStack = settings.tunStack,
            showSystemProxy = settings.showSystemProxy,
            rootInstalled = rootCore.isInstalled(),
            rootArtifactsPresent = rootCore.hasInstallationRecord(),
            rootWorking = mutableUiState.value.rootWorking,
            rootMessage = mutableUiState.value.rootMessage,
        )
    }

    fun installRoot() = performRootAction {
        rootCore.install()
    }

    fun uninstallRoot() = performRootAction {
        if (mutableUiState.value.running) context.stopClashService()
        rootCore.uninstall()
        if (repository.read().proxyMethod == ProxyMethod.Root) repository.setProxyMethod(ProxyMethod.Vpn)
    }

    fun dismissRootMessage() {
        mutableUiState.update { it.copy(rootMessage = null) }
    }

    private fun performRootAction(action: suspend () -> Unit) {
        if (mutableUiState.value.rootWorking) return
        mutableUiState.update { it.copy(rootWorking = true, rootMessage = null) }
        viewModelScope.launch {
            val error = runCatching { withContext(Dispatchers.IO + NonCancellable) { action() } }.exceptionOrNull()
            refresh(false)
            mutableUiState.update {
                it.copy(rootWorking = false, rootMessage = error?.message ?: if (rootCore.isInstalled()) "透明代理已安装" else "透明代理已卸载")
            }
        }
    }

    fun setProxyMethod(value: ProxyMethod) {
        repository.setProxyMethod(value)
        mutableUiState.update { it.copy(proxyMethod = value) }
    }

    fun setBypassPrivateNetwork(value: Boolean) {
        repository.setBypassPrivateNetwork(value)
        mutableUiState.update { it.copy(bypassPrivateNetwork = value) }
    }

    fun setDnsHijacking(value: Boolean) {
        repository.setDnsHijacking(value)
        mutableUiState.update { it.copy(dnsHijacking = value) }
    }

    fun setAllowBypass(value: Boolean) {
        repository.setAllowBypass(value)
        mutableUiState.update { it.copy(allowBypass = value) }
    }

    fun setAllowIpv6(value: Boolean) {
        repository.setAllowIpv6(value)
        mutableUiState.update { it.copy(allowIpv6 = value) }
    }

    fun setSystemProxy(value: Boolean) {
        repository.setSystemProxy(value)
        mutableUiState.update { it.copy(systemProxy = value) }
    }

    fun setTunStack(value: String) {
        repository.setTunStack(value)
        mutableUiState.update { it.copy(tunStack = value) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                NetworkSettingsViewModel(StoredNetworkSettingsRepository(context.applicationContext), context.applicationContext) as T
        }
    }
}
