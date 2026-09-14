package com.aurora.client.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class ProxyNode(val name: String, val delayMs: Int, val selected: Boolean = false)

object AppState {
    private val _connection = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _vpnDiagnostic = MutableStateFlow("尚未请求 VPN 权限")
    val vpnDiagnostic: StateFlow<String> = _vpnDiagnostic.asStateFlow()

    val nodes = listOf(
        ProxyNode("Auto Select", 42, true),
        ProxyNode("Tokyo 01", 51),
        ProxyNode("Singapore 01", 86),
        ProxyNode("Los Angeles 01", 138)
    )

    fun setConnection(value: ConnectionState, message: String? = null) {
        _connection.value = value
        _message.value = message
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    fun setVpnDiagnostic(value: String) {
        _vpnDiagnostic.value = value
    }

    fun clearMessage() {
        _message.value = null
    }
}
