package com.github.kr328.clash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.getSystemService
import com.github.kr328.clash.common.compat.startForegroundServiceCompat
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.util.fileName
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.R as DesignR
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.log.LogcatFilter
import com.github.kr328.clash.log.LogcatReader
import com.github.kr328.clash.util.logsDir
import com.koude.aurora.designsystem.theme.AuroraTheme
import com.koude.aurora.ui.logs.LogcatScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LogcatActivity : BaseActivity() {
    private var conn: ServiceConnection? = null
    private val messagesState = mutableStateOf<List<LogMessage>>(emptyList())
    private val streamingState = mutableStateOf(false)
    private val exportProgress = mutableIntStateOf(-1)
    private val exportProgressMax = mutableIntStateOf(0)

    override suspend fun main() {
        val name = intent?.fileName
        if (name != null) {
            val file = LogFile.parseFromFileName(name) ?: return showInvalid()
            return mainLocalFile(file)
        }
        mainStreaming()
    }

    private suspend fun mainLocalFile(file: LogFile) {
        messagesState.value = try {
            withContext(Dispatchers.IO) { LogcatReader(this@LogcatActivity, file).use { it.readAll() } }
        } catch (e: Exception) {
            Log.e("Fail to read log file ${file.fileName}: ${e.message}")
            showInvalid()
            finish()
            return
        }
        streamingState.value = false
        setContent {
            AuroraTheme(darkTheme = isDarkTheme) {
                LogcatScreen(
                    messages = messagesState.value,
                    streaming = false,
                    exportProgress = exportProgress.intValue.takeIf { it >= 0 },
                    exportProgressMax = exportProgressMax.intValue,
                    onClose = ::finish,
                    onDelete = {
                        launch {
                            withContext(Dispatchers.IO) { logsDir.resolve(file.fileName).delete() }
                            finish()
                        }
                    },
                    onExport = { launch { exportLog(file, messagesState.value) } },
                    onCopy = ::copyMessage,
                )
            }
        }
        while (isActive) events.receive()
    }

    private suspend fun mainStreaming() {
        streamingState.value = true
        setContent {
            AuroraTheme(darkTheme = isDarkTheme) {
                LogcatScreen(
                    messages = messagesState.value,
                    streaming = true,
                    onClose = {
                        launch {
                            stopService(LogcatService::class.intent)
                            startActivity(LogsActivity::class.intent)
                            finish()
                        }
                    },
                    onDelete = {},
                    onExport = {},
                    onCopy = ::copyMessage,
                )
            }
        }
        startForegroundServiceCompat(LogcatService::class.intent)
        val logcat = bindLogcatService()
        val ticker = ticker(500)
        var initial = true
        while (isActive) {
            select<Unit> {
                events.onReceive { }
                if (activityStarted) ticker.onReceive {
                    val snapshot = logcat.snapshot(initial) ?: return@onReceive
                    messagesState.value = snapshot.messages
                    initial = false
                }
            }
        }
    }

    private suspend fun bindLogcatService(): LogcatService = suspendCoroutine { continuation ->
        bindService(LogcatService::class.intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val instance = service!!.queryLocalInterface("") as LogcatService
                continuation.resume(instance)
                conn = this
            }
            override fun onServiceDisconnected(name: ComponentName?) { conn = null }
        }, Context.BIND_AUTO_CREATE)
    }

    private fun copyMessage(message: LogMessage) {
        getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText("log_message", message.message))
        Toast.makeText(this, DesignR.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun exportLog(file: LogFile, messages: List<LogMessage>) {
        launch {
            val uri: Uri? = startActivityForResult(ActivityResultContracts.CreateDocument("text/plain"), file.fileName)
            if (uri == null) return@launch
            exportProgressMax.intValue = messages.size
            exportProgress.intValue = 0
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.let { output ->
                        LogcatFilter(OutputStreamWriter(output), this@LogcatActivity).use { filter ->
                            filter.writeHeader(file.date)
                            messages.forEachIndexed { index, message ->
                                filter.writeMessage(message)
                                if (index % 50 == 0 || index == messages.lastIndex) exportProgress.intValue = index + 1
                            }
                        }
                    } ?: error("Cannot open the selected output")
                }
                Toast.makeText(this@LogcatActivity, DesignR.string.file_exported, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@LogcatActivity, e.message ?: "导出失败", Toast.LENGTH_LONG).show()
            } finally {
                exportProgress.intValue = -1
            }
        }
    }

    override fun onDestroy() {
        conn?.apply(this::unbindService)
        super.onDestroy()
    }

    private fun showInvalid() {
        Toast.makeText(this, DesignR.string.invalid_log_file, Toast.LENGTH_LONG).show()
    }
}
