package com.koude.aurora.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.koude.aurora.data.settings.NetworkSettingsRepository
import com.koude.aurora.data.settings.StoredNetworkSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NetworkSettingsViewModel(private val repository: NetworkSettingsRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NetworkSettingsUiState())
    val uiState: StateFlow<NetworkSettingsUiState> = mutableUiState.asStateFlow()

    fun refresh(running: Boolean) {
        val settings = repository.read()
        mutableUiState.value = NetworkSettingsUiState(
            running = running,
            routeSystemTraffic = settings.routeSystemTraffic,
            bypassPrivateNetwork = settings.bypassPrivateNetwork,
            dnsHijacking = settings.dnsHijacking,
            allowBypass = settings.allowBypass,
            allowIpv6 = settings.allowIpv6,
            systemProxy = settings.systemProxy,
            tunStack = settings.tunStack,
            showSystemProxy = settings.showSystemProxy,
        )
    }

    fun setRouteSystemTraffic(value: Boolean) {
        repository.setRouteSystemTraffic(value)
        mutableUiState.update { it.copy(routeSystemTraffic = value) }
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
                NetworkSettingsViewModel(StoredNetworkSettingsRepository(context.applicationContext)) as T
        }
    }
}
