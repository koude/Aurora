package com.github.kr328.clash

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setFileName
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.util.logsDir
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.logs.LogsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogsActivity : BaseActivity() {
    private val filesState = mutableStateOf<List<LogFile>>(emptyList())

    override suspend fun main() {
        filesState.value = withContext(Dispatchers.IO) { loadFiles() }
        setContent {
            AuroraTheme(darkTheme = isDarkTheme) {
                LogsScreen(
                    files = filesState.value,
                    onBack = ::finish,
                    onStartLogcat = {
                        startActivity(LogcatActivity::class.intent)
                        finish()
                    },
                    onOpenFile = { startActivity(LogcatActivity::class.intent.setFileName(it.fileName)) },
                    onDeleteAll = {
                        launch {
                            withContext(Dispatchers.IO) { logsDir.deleteRecursively() }
                            filesState.value = withContext(Dispatchers.IO) { loadFiles() }
                        }
                    },
                )
            }
        }
        while (isActive) events.receive()
    }

    private fun loadFiles(): List<LogFile> = cacheDir.resolve("logs").listFiles()
        ?.mapNotNull { LogFile.parseFromFileName(it.name) }
        ?.sortedByDescending { it.date.time }
        .orEmpty()
}
