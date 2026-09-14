package com.koude.aurora.data

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AppLogger {
    private const val TAG = "Aurora"
    private const val MAX_BYTES = 2L * 1024 * 1024
    private const val MAX_LINES_IN_MEMORY = 800

    private val lock = Any()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private var logFile: File? = null

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun init(context: Context) {
        synchronized(lock) {
            if (logFile != null) return
            val dir = File(context.filesDir, "logs").apply { mkdirs() }
            logFile = File(dir, "aurora.log")
            _lines.value = readTailLocked()
        }
        i("APP", "Logger initialized; Android=${Build.VERSION.RELEASE} SDK=${Build.VERSION.SDK_INT} ABI=${Build.SUPPORTED_ABIS.joinToString()}")
        i("APP", "nativeLibraryDir=${context.applicationInfo.nativeLibraryDir}")
    }

    fun d(area: String, message: String) = write("D", area, message, null)
    fun i(area: String, message: String) = write("I", area, message, null)
    fun w(area: String, message: String, error: Throwable? = null) = write("W", area, message, error)
    fun e(area: String, message: String, error: Throwable? = null) = write("E", area, message, error)

    fun allText(): String = synchronized(lock) {
        logFile?.takeIf { it.exists() }?.readText().orEmpty()
    }

    fun clear() {
        synchronized(lock) {
            logFile?.writeText("")
            _lines.value = emptyList()
        }
        i("APP", "Log cleared by user")
    }

    private fun write(level: String, area: String, message: String, error: Throwable?) {
        val safe = message.replace('\n', ' ').replace('\r', ' ')
        val head = "${LocalDateTime.now().format(formatter)} $level/$area $safe"
        val body = if (error != null) head + "\n" + Log.getStackTraceString(error) else head
        when (level) {
            "E" -> Log.e(TAG, "$area: $safe", error)
            "W" -> Log.w(TAG, "$area: $safe", error)
            "D" -> Log.d(TAG, "$area: $safe")
            else -> Log.i(TAG, "$area: $safe")
        }

        synchronized(lock) {
            val file = logFile ?: return
            rotateIfNeededLocked(file, body.length.toLong())
            file.appendText(body + "\n")
            val newLines = (_lines.value + body.lines()).takeLast(MAX_LINES_IN_MEMORY)
            _lines.value = newLines
        }
    }

    private fun rotateIfNeededLocked(file: File, incoming: Long) {
        if (file.exists() && file.length() + incoming > MAX_BYTES) {
            val old = File(file.parentFile, "aurora.log.1")
            if (old.exists()) old.delete()
            file.renameTo(old)
            file.createNewFile()
        }
    }

    private fun readTailLocked(): List<String> {
        val file = logFile ?: return emptyList()
        if (!file.exists()) return emptyList()
        return runCatching { file.readLines().takeLast(MAX_LINES_IN_MEMORY) }.getOrDefault(emptyList())
    }
}
