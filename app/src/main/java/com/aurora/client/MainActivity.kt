package com.aurora.client

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.aurora.client.service.AuroraVpnService
import com.aurora.client.ui.AuroraApp
import com.aurora.client.ui.theme.AuroraTheme

class MainActivity : ComponentActivity() {
    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) AuroraVpnService.start(this)
    }

    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            AuroraTheme {
                AuroraApp(
                    onConnect = {
                        val intent = VpnService.prepare(this)
                        if (intent != null) vpnPermission.launch(intent) else AuroraVpnService.start(this)
                    },
                    onDisconnect = { AuroraVpnService.stop(this) }
                )
            }
        }
    }
}
