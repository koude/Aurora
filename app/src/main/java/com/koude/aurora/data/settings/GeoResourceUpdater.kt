package com.koude.aurora.data.settings

import android.content.Context
import com.github.kr328.clash.util.clashDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

data class GeoUpdateResult(val updated: List<String>, val failed: List<String>)

/** Updates data files only; configuration rules and subscriptions are untouched. */
class GeoResourceUpdater(private val context: Context) {
    private data class Resource(
        val name: String,
        val releaseName: String = name,
        val maxBytes: Long = 64L * 1024 * 1024,
    ) {
        val urls: List<String> get() = listOf(
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/$releaseName",
            "https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/$releaseName",
            "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/$releaseName",
        )
    }

    private val resources = listOf(
        Resource("geoip.metadb"),
        Resource("geosite.dat"),
        Resource("ASN.mmdb", "GeoLite2-ASN.mmdb"),
        Resource("BundleMRS.7z", maxBytes = 128L * 1024 * 1024),
    )

    suspend fun update(onProgress: (name: String, index: Int, total: Int) -> Unit): GeoUpdateResult =
        withContext(Dispatchers.IO) {
            val directory = context.clashDir.apply { mkdirs() }
            val updated = mutableListOf<String>()
            val failed = mutableListOf<String>()
            resources.forEachIndexed { index, resource ->
                coroutineContext.ensureActive()
                onProgress(resource.name, index + 1, resources.size)
                val staged = File.createTempFile("geo-update-", ".tmp", directory)
                try {
                    var lastError: Exception? = null
                    var valid = false
                    for (url in resource.urls) {
                        coroutineContext.ensureActive()
                        try {
                            download(url, staged, resource.maxBytes)
                            validate(resource.name, staged)
                            valid = true
                            break
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            lastError = error
                            staged.delete()
                        }
                    }
                    if (!valid) throw IOException("${resource.name}: ${lastError?.message ?: "下载失败"}")
                    check(staged.renameTo(directory.resolve(resource.name))) {
                        "${resource.name}: 无法保存更新文件"
                    }
                    updated += resource.name
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failed += resource.name
                } finally {
                    staged.delete()
                }
            }
            GeoUpdateResult(updated, failed)
        }

    private fun download(url: String, destination: File, maxBytes: Long) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("User-Agent", "Aurora-Geo-Updater")
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "HTTP ${connection.responseCode}"
            }
            check(connection.url.protocol == "https") { "下载地址必须使用 HTTPS" }
            val expected = connection.contentLengthLong
            check(expected <= maxBytes) { "文件超过大小限制" }
            connection.inputStream.use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= maxBytes) { "文件超过大小限制" }
                        output.write(buffer, 0, count)
                    }
                    check(total > 128 * 1024) { "文件过小" }
                    if (expected >= 0) check(total == expected) { "下载不完整" }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun validate(name: String, file: File) {
        val prefix = file.inputStream().use { input -> ByteArray(8).also { input.read(it) } }
        when (name) {
            "geoip.metadb", "ASN.mmdb" -> {
                val marker = byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte()) + "MaxMind.com".toByteArray()
                val tail = ByteArray(minOf(file.length(), 512L).toInt())
                RandomAccessFile(file, "r").use { input ->
                    input.seek(file.length() - tail.size)
                    input.readFully(tail)
                }
                check(tail.asList().windowed(marker.size).any { it == marker.asList() }) { "不是有效的数据库" }
            }
            "geosite.dat" -> check(prefix[0] == 0x0a.toByte()) { "不是有效的 GeoSite 文件" }
            "BundleMRS.7z" -> check(prefix.copyOfRange(0, 6).contentEquals(
                byteArrayOf(0x37, 0x7a, 0xbc.toByte(), 0xaf.toByte(), 0x27, 0x1c)
            )) { "不是有效的 7z 文件" }
        }
    }
}
