package com.aurora.client

import android.Manifest
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.aurora.client.data.AppState
import com.aurora.client.data.ConnectionState
import com.aurora.client.service.AuroraVpnService
import com.aurora.client.ui.AuroraApp
import com.aurora.client.ui.theme.AuroraTheme

class MainActivity : ComponentActivity() {
    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) startVpnSafely() else AppState.setConnection(ConnectionState.ERROR, "VPN 权限未授权")
    }

    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) AppState.showMessage("通知权限未授权；连接仍可尝试，但系统可能限制前台服务提示")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            AuroraTheme {
                AuroraApp(
                    onConnect = {
                        runCatching {
                            val intent = VpnService.prepare(this)
                            if (intent != null) vpnPermission.launch(intent) else startVpnSafely()
                        }.onFailure {
                            AppState.setConnection(ConnectionState.ERROR, it.message ?: "无法请求 VPN 权限")
                        }
                    },
                    onDisconnect = {
                        runCatching { AuroraVpnService.stop(this) }
                            .onFailure { AppState.showMessage(it.message ?: "断开失败") }
                    }
                )
            }
        }
    }

    private fun startVpnSafely() {
        AppState.showMessage("正在启动 VPN 服务")
        runCatching { AuroraVpnService.start(this) }
            .onFailure { AppState.setConnection(ConnectionState.ERROR, it.message ?: "VPN 服务启动失败") }
    }
}
