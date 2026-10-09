package com.github.kr328.clash

import com.github.kr328.clash.common.compat.versionCodeCompat
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.log.SystemLogcat
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.misc.AppCrashedScreen
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class AppCrashedActivity : BaseActivity() {
    private val logsState = mutableStateOf("")

    override suspend fun main() {
        val packageInfo = withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0)
        }

        Log.i("App version: versionName = ${packageInfo.versionName} versionCode = ${packageInfo.versionCodeCompat}")

        val logs = withContext(Dispatchers.IO) {
            SystemLogcat.dumpCrash()
        }

        logsState.value = logs
        setContent { AuroraTheme(darkTheme = isDarkTheme) { AppCrashedScreen(logsState.value) } }

        while (isActive) {
            events.receive()
        }
    }
}
