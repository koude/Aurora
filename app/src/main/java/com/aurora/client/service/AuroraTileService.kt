package com.aurora.client.service

import android.content.ComponentName
import android.content.Context
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.aurora.client.data.AppState
import com.aurora.client.data.ConnectionState

class AuroraTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        if (AppState.connection.value == ConnectionState.CONNECTED ||
            AppState.connection.value == ConnectionState.CONNECTING
        ) {
            AuroraVpnService.stop(this)
        } else {
            val permission = VpnService.prepare(this)
            if (permission == null) {
                AuroraVpnService.start(this)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(permission.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        refresh()
    }

    private fun refresh() {
        qsTile?.apply {
            state = if (AppState.connection.value == ConnectionState.CONNECTED) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Aurora"
            updateTile()
        }
    }

    companion object {
        fun requestRefresh(context: Context) {
            if (Build.VERSION.SDK_INT >= 24) {
                requestListeningState(context, ComponentName(context, AuroraTileService::class.java))
            }
        }
    }
}
