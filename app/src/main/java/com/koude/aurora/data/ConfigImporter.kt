package com.koude.aurora.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

object ConfigImporter {
    private const val CONFIG_NAME = "config.yaml"
    private const val PREFS = "aurora_config"
    private const val KEY_SOURCE = "source"

    fun hasConfig(context: Context): Boolean = configFile(context).let { it.exists() && it.length() > 0 }

    fun sourceLabel(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SOURCE, null)
            ?: if (hasConfig(context)) "本地配置" else "未导入"

    fun importFile(context: Context, uri: Uri): Result<Unit> = runCatching {
        AppLogger.i("CONFIG", "Importing configuration from local document")
        val temp = File(context.filesDir, "$CONFIG_NAME.tmp")
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取所选文件")
        validate(temp)
        replaceConfig(context, temp)
        saveSource(context, "本地文件")
        AppLogger.i("CONFIG", "Local configuration imported; size=${configFile(context).length()}")
    }.onFailure { AppLogger.e("CONFIG", "Local import failed: ${it.javaClass.simpleName}: ${it.message}", it) }

    fun importUrl(context: Context, rawUrl: String): Result<Unit> = runCatching {
        AppLogger.i("CONFIG", "Importing configuration from URL")
        val url = rawUrl.trim()
        require(url.startsWith("https://") || url.startsWith("http://")) { "仅支持 http/https 链接" }

        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "clash.meta")
        connection.setRequestProperty("Accept", "text/yaml,text/plain,application/yaml,*/*")

        try {
            val code = connection.responseCode
            require(code in 200..299) { "下载失败：HTTP $code" }
            val temp = File(context.filesDir, "$CONFIG_NAME.tmp")
            connection.inputStream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            validate(temp)
            replaceConfig(context, temp)
            val host = runCatching { URI(url).host }.getOrNull().orEmpty()
            saveSource(context, if (host.isBlank()) "链接导入" else "链接 · $host")
            AppLogger.i("CONFIG", "URL configuration imported; host=${if (host.isBlank()) "<unknown>" else host} size=${configFile(context).length()}")
        } finally {
            connection.disconnect()
        }
    }.onFailure { AppLogger.e("CONFIG", "URL import failed: ${it.javaClass.simpleName}: ${it.message}", it) }

    private fun validate(file: File) {
        require(file.exists() && file.length() > 0) { "配置内容为空" }
        require(file.length() <= 20L * 1024 * 1024) { "配置文件过大" }
        val head = file.inputStream().bufferedReader().use { reader ->
            buildString {
                repeat(8) {
                    val line = reader.readLine() ?: return@repeat
                    append(line).append('\n')
                }
            }
        }.trimStart()
        require(!head.startsWith("<!DOCTYPE", true) && !head.startsWith("<html", true)) {
            "链接返回的是网页，不是配置文件"
        }
    }

    private fun replaceConfig(context: Context, temp: File) {
        val target = configFile(context)
        if (target.exists() && !target.delete()) error("无法替换旧配置")
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }

    private fun configFile(context: Context) = File(context.filesDir, CONFIG_NAME)

    private fun saveSource(context: Context, source: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SOURCE, source).apply()
    }
}
