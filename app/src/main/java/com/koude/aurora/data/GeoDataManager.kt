package com.koude.aurora.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object GeoDataManager {
    private data class Resource(val fileName: String, val marker: Regex, val urls: List<String>)

    private val resources = listOf(
        Resource(
            "GeoSite.dat",
            Regex("(?im)\\bGEOSITE\\s*,|geosite:"),
            listOf(
                "https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geosite.dat",
                "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geosite.dat",
                "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geosite.dat"
            )
        ),
        Resource(
            "GeoIP.dat",
            Regex("(?im)\\bGEOIP\\s*,"),
            listOf(
                "https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geoip.dat",
                "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geoip.dat"
            )
        )
    )

    fun ensureRequired(context: Context, configFile: File) {
        val config = configFile.readText()
        resources.filter { it.marker.containsMatchIn(config) }.forEach { ensure(context, it) }
    }

    private fun ensure(context: Context, resource: Resource) {
        val target = File(context.filesDir, resource.fileName)
        if (target.isFile && target.length() > 1024L) {
            AppLogger.i("GEO", "${resource.fileName} ready; size=${target.length()}")
            return
        }

        AppLogger.i("GEO", "${resource.fileName} missing; bootstrapping before VPN")
        var lastError: Throwable? = null
        for ((index, url) in resource.urls.withIndex()) {
            try {
                download(url, target)
                AppLogger.i("GEO", "${resource.fileName} downloaded; source=${index + 1}/${resource.urls.size} size=${target.length()}")
                return
            } catch (t: Throwable) {
                lastError = t
                AppLogger.w("GEO", "${resource.fileName} source ${index + 1} failed: ${t.javaClass.simpleName}: ${t.message}")
            }
        }
        throw IllegalStateException("Unable to bootstrap ${resource.fileName}: ${lastError?.message}", lastError)
    }

    private fun download(source: String, target: File) {
        val temp = File(target.parentFile, "${target.name}.download")
        if (temp.exists()) temp.delete()
        val connection = (URL(source).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "clash.meta")
        }
        try {
            val code = connection.responseCode
            check(code in 200..299) { "HTTP $code" }
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            check(temp.length() > 1024L) { "downloaded file is too small (${temp.length()} bytes)" }
            if (target.exists()) check(target.delete()) { "cannot replace ${target.name}" }
            check(temp.renameTo(target)) { "cannot install ${target.name}" }
        } finally {
            connection.disconnect()
            if (temp.exists()) temp.delete()
        }
    }
}
