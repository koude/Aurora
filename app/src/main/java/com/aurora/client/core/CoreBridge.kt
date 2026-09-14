package com.aurora.client.core

import android.content.Context
import io.github.oviron.libmihomo.Clash
import io.github.oviron.libmihomo.TunInterface
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

interface CoreBridge {
    val isAvailable: Boolean
    fun start(configPath: String, tunFd: Int, tunInterface: TunInterface): Result<Unit>
    fun stop()
}

/**
 * Thin adapter around libmihomo-android.
 * Aurora deliberately keeps the upstream core isolated behind this interface so the UI/service
 * layer does not depend on mihomo implementation details.
 */
class MihomoCore(private val context: Context) : CoreBridge {
    @Volatile
    private var loaded = false

    override val isAvailable: Boolean
        get() = runCatching {
            ensureLoaded()
            Clash.isLoaded()
        }.getOrDefault(false)

    private fun ensureLoaded() {
        if (loaded && Clash.isLoaded()) return
        synchronized(this) {
            if (!loaded || !Clash.isLoaded()) {
                Clash.load(context.applicationInfo.nativeLibraryDir)
                require(Clash.isLoaded()) { "Aurora core load failed" }
                require(Clash.bridgeABI() == Clash.EXPECTED_BRIDGE_ABI) {
                    "Core ABI mismatch: ${Clash.bridgeABI()} != ${Clash.EXPECTED_BRIDGE_ABI}"
                }
                loaded = true
            }
        }
    }

    override fun start(configPath: String, tunFd: Int, tunInterface: TunInterface): Result<Unit> = runCatching {
        ensureLoaded()

        val setupLatch = CountDownLatch(1)
        var setupError: String? = null

        val home = context.filesDir.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")
        val profile = configPath.replace("\\", "\\\\").replace("\"", "\\\"")

        Clash.quickSetup(
            initParams = """{"homeDir":"$home"}""",
            setupParams = """{"profile":"$profile"}"""
        ) { result ->
            if (!result.isNullOrEmpty()) setupError = result
            setupLatch.countDown()
        }

        check(setupLatch.await(15, TimeUnit.SECONDS)) { "Core setup timed out" }
        check(setupError.isNullOrEmpty()) { "Core setup failed: $setupError" }

        Clash.startTUN(
            fd = tunFd,
            cb = tunInterface,
            device = "aurora",
            stack = "system",
            address = "172.19.0.1/30",
            dns = "1.1.1.1,1.0.0.1",
            mtu = 1400
        )
    }

    override fun stop() {
        if (runCatching { Clash.isLoaded() }.getOrDefault(false)) {
            runCatching { Clash.stopTun() }
        }
    }
}
