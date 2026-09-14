package com.koude.aurora

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.koude.aurora.data.AppState
import com.koude.aurora.data.ConnectionState
import com.koude.aurora.service.AuroraVpnService
import com.koude.aurora.ui.AuroraApp
import com.koude.aurora.ui.theme.AuroraTheme

class MainActivity : ComponentActivity() {
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) AppState.showMessage("通知权限未授权；连接仍可尝试，但系统可能限制前台服务提示")
    }

    // Match Clash Meta's VPN consent flow: launch the Intent returned by VpnService.prepare(),
    // then retry service start only after Android reports RESULT_OK.
    private val vpnConsent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        AppState.setVpnDiagnostic("VPN 授权页返回 resultCode=${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            startAuroraService()
        } else {
            AppState.setConnection(ConnectionState.ERROR, "VPN 权限未授权：用户未允许或系统未完成授权")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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
            val request = startAuroraService()
            if (request != null) {
                val component = request.component?.flattenToShortString() ?: "<无 component>"
                val action = request.action ?: "<无 action>"
                AppState.setVpnDiagnostic("prepare=Intent；action=$action；component=$component；正在打开系统授权页")
                AppState.setConnection(ConnectionState.CONNECTING, "正在请求系统 VPN 权限")
                vpnConsent.launch(request)
            }
        }.onFailure {
            AppState.setVpnDiagnostic("VPN 授权启动异常：${it.javaClass.simpleName}: ${it.message}")
            AppState.setConnection(ConnectionState.ERROR, it.message ?: "无法打开系统 VPN 授权页面")
        }
    }

    /**
     * Same control pattern used by Clash Meta: if Android still requires consent,
     * return the consent Intent. Otherwise start the VPN service immediately.
     */
    private fun startAuroraService(): Intent? {
        val request = VpnService.prepare(this)
        if (request != null) return request

        AppState.setVpnDiagnostic("prepare=null；VPN 权限已授权，启动 AuroraVpnService")
        AuroraVpnService.start(this)
        return null
    }
}
