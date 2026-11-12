package com.github.kr328.clash.util

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.github.kr328.clash.common.compat.startForegroundServiceCompat
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.service.ClashService
import com.github.kr328.clash.service.RootService
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.service.TunService
import com.github.kr328.clash.common.ProxyMethod
import com.github.kr328.clash.service.util.sendBroadcastSelf

fun Context.startClashService(): Intent? {
    val method = UiStore(this).proxyMethod
    ServiceStore(this).proxyMethod = method
    when (method) {
        ProxyMethod.Vpn -> {
            val vpnRequest = VpnService.prepare(this)
            if (vpnRequest != null)
                return vpnRequest

            startForegroundServiceCompat(TunService::class.intent)
        }
        ProxyMethod.Proxy -> {
            startForegroundServiceCompat(ClashService::class.intent)
        }
        ProxyMethod.Root -> {
            startForegroundServiceCompat(RootService::class.intent)
        }
    }

    return null
}

fun Context.stopClashService() {
    sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP))
}
