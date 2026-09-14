package com.koude.aurora.core

import android.content.Context
import com.koude.aurora.data.AppLogger
import io.github.oviron.libmihomo.Clash
import io.github.oviron.libmihomo.TunInterface
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

interface CoreBridge {
    val isAvailable: Boolean
    fun start(configPath: String, tunFd: Int, tunInterface: TunInterface): Result<Unit>
    fun stop()
}

class MihomoCore(private val context: Context) : CoreBridge {
    @Volatile
    private var loaded = false

    override val isAvailable: Boolean
        get() = runCatching {
            ensureLoaded()
            Clash.isLoaded()
        }.onFailure {
            AppLogger.e("CORE", "Core availability check failed: ${it.javaClass.simpleName}: ${it.message}", it)
        }.getOrDefault(false)

    private fun ensureLoaded() {
        if (loaded && Clash.isLoaded()) return
        synchronized(this) {
            if (!loaded || !Clash.isLoaded()) {
                val nativeDir = context.applicationInfo.nativeLibraryDir
                val nativeFiles = runCatching { File(nativeDir).listFiles()?.joinToString { "${it.name}(${it.length()})" } ?: "<empty>" }.getOrDefault("<unreadable>")
                AppLogger.i("CORE", "Loading mihomo bridge from nativeLibraryDir=$nativeDir")
                AppLogger.i("CORE", "Native libraries: $nativeFiles")
                try {
                    Clash.load(nativeDir)
                } catch (t: Throwable) {
                    AppLogger.e("CORE", "Clash.load failed: ${t.javaClass.simpleName}: ${t.message}", t)
                    throw t
                }

                val isLoaded = runCatching { Clash.isLoaded() }.getOrElse {
                    AppLogger.e("CORE", "Clash.isLoaded failed after load", it)
                    throw it
                }
                check(isLoaded) { "Aurora core load failed" }

                val actualAbi = runCatching { Clash.bridgeABI() }.getOrElse {
                    AppLogger.e("CORE", "bridgeABI query failed", it)
                    throw it
                }
                val expectedAbi = Clash.EXPECTED_BRIDGE_ABI
                AppLogger.i("CORE", "Core loaded; bridgeABI=$actualAbi expected=$expectedAbi")
                check(actualAbi == expectedAbi) { "Core ABI mismatch: $actualAbi != $expectedAbi" }
                loaded = true
            }
        }
    }

    override fun start(configPath: String, tunFd: Int, tunInterface: TunInterface): Result<Unit> = runCatching {
        AppLogger.i("CORE", "Starting core; config=$configPath tunFd=$tunFd")
        ensureLoaded()

        val setupLatch = CountDownLatch(1)
        var setupError: String? = null

        val home = context.filesDir.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")
        val profile = configPath.replace("\\", "\\\\").replace("\"", "\\\"")

        AppLogger.i("CORE", "Calling quickSetup")
        Clash.quickSetup(
            initParams = """{"homeDir":"$home"}""",
            setupParams = """{"profile":"$profile"}"""
        ) { result ->
            if (!result.isNullOrEmpty()) setupError = result
            setupLatch.countDown()
        }

        check(setupLatch.await(15, TimeUnit.SECONDS)) { "Core setup timed out" }
        check(setupError.isNullOrEmpty()) { "Core setup failed: $setupError" }
        AppLogger.i("CORE", "quickSetup completed")

        AppLogger.i("CORE", "Calling startTUN")
        Clash.startTUN(
            fd = tunFd,
            cb = tunInterface,
            device = "aurora",
            stack = "system",
            address = "172.19.0.1/30",
            dns = "1.1.1.1,1.0.0.1",
            mtu = 1400
        )
        AppLogger.i("CORE", "startTUN returned successfully")
    }.onFailure {
        AppLogger.e("CORE", "Core start failed: ${it.javaClass.simpleName}: ${it.message}", it)
    }

    override fun stop() {
        AppLogger.i("CORE", "Stopping core")
        if (runCatching { Clash.isLoaded() }.getOrDefault(false)) {
            runCatching { Clash.stopTun() }
                .onSuccess { AppLogger.i("CORE", "stopTun completed") }
                .onFailure { AppLogger.w("CORE", "stopTun failed: ${it.message}", it) }
        }
    }
}
