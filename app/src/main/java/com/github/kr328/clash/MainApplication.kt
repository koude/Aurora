package com.github.kr328.clash

import android.app.Application
import android.content.Context
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.compat.currentProcessName
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.util.sendServiceRecreated
import com.github.kr328.clash.util.clashDir
import java.io.File
import java.io.FileOutputStream

@Suppress("unused")
class MainApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()

        val processName = currentProcessName
        extractGeoFiles()

        Log.d("Process $processName started")

        if (processName == packageName) {
            Remote.launch()
        } else {
            sendServiceRecreated()
        }
    }

    private fun extractGeoFiles() {
        clashDir.mkdirs()
        // APK assets are an offline fallback. Preserve files that the user
        // imported or updated in the app across future APK upgrades.
        listOf("geoip.metadb", "geosite.dat", "ASN.mmdb", "BundleMRS.7z").forEach { name ->
            val target = File(clashDir, name)
            if (target.isFile && target.length() > 0L) return@forEach
            val staged = File.createTempFile("geo-bundled-", ".tmp", clashDir)
            try {
                FileOutputStream(staged).use { output -> assets.open(name).use { it.copyTo(output) } }
                if (!target.isFile || target.length() == 0L) {
                    check(staged.renameTo(target)) { "Unable to install bundled $name" }
                }
            } finally {
                staged.delete()
            }
        }
    }

    fun finalize() {
        Global.destroy()
    }
}
