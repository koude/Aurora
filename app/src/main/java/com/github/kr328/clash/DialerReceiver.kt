package com.github.kr328.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import com.github.kr328.clash.design.store.UiStore.Companion.mainActivityAlias

class DialerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_SECRET_CODE &&
            intent.action != "android.provider.Telephony.SECRET_CODE"
        ) return
        if (intent.data?.scheme != SECRET_CODE_SCHEME || intent.data?.host != SECRET_CODE_HOST) return
        if (context.packageManager.getComponentEnabledSetting(context.mainActivityAlias) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        ) return

        val launchIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }

    companion object {
        private const val SECRET_CODE_SCHEME = "android_secret_code"
        private const val SECRET_CODE_HOST = "252746382"
        const val DIAL_CODE = "*#*#${SECRET_CODE_HOST}#*#*"
    }
}
