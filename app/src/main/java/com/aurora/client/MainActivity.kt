package com.aurora.client

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
                AppState.setVpnDiagnostic("prepare=null；系统已授权 Aurora")
                startVpnSafely()
                return@runCatching
            }

            val component = permissionIntent.component?.flattenToShortString() ?: "<无 component>"
            val action = permissionIntent.action ?: "<无 action>"
            val resolved = packageManager.resolveActivity(permissionIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val resolvedName = resolved?.activityInfo?.let { "${it.packageName}/${it.name}" } ?: "<无法解析>"
            AppState.setVpnDiagnostic(
                "prepare=Intent；action=$action；component=$component；resolved=$resolvedName"
            )

            if (resolved == null) {
                AppState.setConnection(
                    ConnectionState.ERROR,
                    "系统返回了 VPN 授权 Intent，但没有系统 Activity 可以处理。请打开系统 VPN 设置检查 Aurora。"
                )
                openVpnSettingsFallback()
                return@runCatching
            }

            AppState.setConnection(ConnectionState.CONNECTING, "正在打开系统 VPN 授权页面")
            @Suppress("DEPRECATION")
            startActivityForResult(permissionIntent, REQUEST_VPN_PERMISSION)
        }.onFailure {
            AppState.setVpnDiagnostic("启动授权页异常：${it.javaClass.simpleName}: ${it.message}")
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
                val resultName = if (resultCode == Activity.RESULT_OK) "RESULT_OK" else "resultCode=$resultCode"
                if (pendingIntent == null) {
                    AppState.setVpnDiagnostic("授权页返回 $resultName；再次 prepare=null；授权成功")
                    startVpnSafely()
                } else {
                    val component = pendingIntent.component?.flattenToShortString() ?: "<无 component>"
                    AppState.setVpnDiagnostic(
                        "授权页返回 $resultName；再次 prepare 仍为 Intent；component=$component"
                    )
                    AppState.setConnection(
                        ConnectionState.ERROR,
                        "VPN 权限仍未授权。诊断信息已写入设置页，请截图发我。"
                    )
                }
            }
            .onFailure {
                AppState.setVpnDiagnostic("确认授权状态异常：${it.javaClass.simpleName}: ${it.message}")
                AppState.setConnection(ConnectionState.ERROR, it.message ?: "无法确认 VPN 权限状态")
            }
    }

    private fun openVpnSettingsFallback() {
        runCatching {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        }
    }

    private fun startVpnSafely() {
        AppState.showMessage("VPN 权限已授权，正在启动服务")
        runCatching { AuroraVpnService.start(this) }
            .onFailure { AppState.setConnection(ConnectionState.ERROR, it.message ?: "VPN 服务启动失败") }
    }
}
