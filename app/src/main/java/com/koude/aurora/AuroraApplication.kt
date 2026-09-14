package com.koude.aurora

import android.app.Application
import com.koude.aurora.data.AppLogger

class AuroraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        AppLogger.i("APP", "Aurora started; version=$version package=$packageName")
    }
}
