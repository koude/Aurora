package com.aurora.client

import android.Manifest
import android.app.Activity
import android.content.Intent
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
    companion object {
        private const val REQUEST_VPN_PERMISSION = 1001
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
                    onConnect = { requestVpnAndConnect() },
                    onDisconnect = {
                        runCatching { AuroraVpnService.stop(this) }
                            .onFailure { AppState.showMessage(it.message ?: "断开失败") }
                    }
                )
            }
        }
    }

    private fun requestVpnAndConnect() {
        runCatching {
            val permissionIntent = VpnService.prepare(this)
            if (permissionIntent == null) {
                startVpnSafely()
            } else {
                AppState.setConnection(ConnectionState.CONNECTING, "正在请求系统 VPN 授权")
                // Use the platform's classic startActivityForResult path here instead of
                // ActivityResultLauncher. Some heavily customized Android ROMs return from
                // the launcher immediately without ever presenting the VPN consent dialog.
                @Suppress("DEPRECATION")
                startActivityForResult(permissionIntent, REQUEST_VPN_PERMISSION)
            }
        }.onFailure {
            AppState.setConnection(ConnectionState.ERROR, it.message ?: "无法打开系统 VPN 授权页面")
        }
    }

    @Deprecated("Deprecated in Android API; kept intentionally for OEM VPN consent compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_VPN_PERMISSION) return

        // Do not trust resultCode alone. The platform permission state is authoritative.
        runCatching { VpnService.prepare(this) }
            .onSuccess { pendingIntent ->
                if (pendingIntent == null) {
                    startVpnSafely()
                } else {
                    val suffix = if (resultCode == Activity.RESULT_OK) "系统返回已允许，但权限状态未生效" else "用户未允许或系统未完成授权"
                    AppState.setConnection(ConnectionState.ERROR, "VPN 权限未授权：$suffix")
                }
            }
            .onFailure {
                AppState.setConnection(ConnectionState.ERROR, it.message ?: "无法确认 VPN 权限状态")
            }
    }

    private fun startVpnSafely() {
        AppState.showMessage("VPN 权限已授权，正在启动服务")
        runCatching { AuroraVpnService.start(this) }
            .onFailure { AppState.setConnection(ConnectionState.ERROR, it.message ?: "VPN 服务启动失败") }
    }
}
