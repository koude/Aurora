package com.koude.aurora.core

import android.content.Context
import android.os.Build
import android.os.Build
import com.koude.aurora.data.AppLogger
import io.github.oviron.libmihomo.Clash
import io.github.oviron.libmihomo.TunInterface
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

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
                val nativeDir = resolveNativeLibraryDir()
                val nativeFiles = runCatching {
                    File(nativeDir).listFiles()?.joinToString { "${it.name}(${it.length()})" } ?: "<empty>"
                }.getOrDefault("<unreadable>")

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

    /**
     * libmihomo-android expects a real filesystem directory containing both native libraries.
     * Most Android installs expose those through applicationInfo.nativeLibraryDir. If a ROM or
     * packaging mode keeps them inside the APK instead, extract the current ABI pair into Aurora's
     * private files directory and load from there.
     */
    private fun resolveNativeLibraryDir(): String {
        val systemDir = File(context.applicationInfo.nativeLibraryDir)
        if (hasRequiredLibraries(systemDir)) {
            AppLogger.i("CORE", "Using system native library directory")
            return systemDir.absolutePath
        }

        AppLogger.w(
            "CORE",
            "System native library directory is missing mihomo libraries; attempting APK fallback extraction"
        )

        val apk = File(context.applicationInfo.sourceDir)
        check(apk.isFile) { "APK source not found: ${apk.absolutePath}" }

        ZipFile(apk).use { zip ->
            val abi = Build.SUPPORTED_ABIS.firstOrNull { candidate ->
                REQUIRED_LIBRARIES.all { name -> zip.getEntry("lib/$candidate/$name") != null }
            } ?: error("APK does not contain mihomo native libraries for supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")

            val outDir = File(context.filesDir, "native/$abi")
            check(outDir.exists() || outDir.mkdirs()) { "Unable to create native extraction directory: $outDir" }

            REQUIRED_LIBRARIES.forEach { name ->
                val entry = zip.getEntry("lib/$abi/$name")
                    ?: error("Missing $name for ABI $abi in APK")
                val target = File(outDir, name)

                if (!target.isFile || target.length() != entry.size) {
                    val temp = File(outDir, "$name.tmp")
                    zip.getInputStream(entry).use { input ->
                        temp.outputStream().buffered().use { output -> input.copyTo(output) }
                    }
                    if (target.exists() && !target.delete()) {
                        error("Unable to replace stale native library: ${target.absolutePath}")
                    }
                    check(temp.renameTo(target)) { "Unable to install native library: ${target.absolutePath}" }
                    target.setReadable(true, true)
                    target.setExecutable(true, true)
                }
            }

            check(hasRequiredLibraries(outDir)) { "APK native library extraction incomplete" }
            AppLogger.i("CORE", "Extracted mihomo libraries from APK for ABI=$abi to ${outDir.absolutePath}")
            return outDir.absolutePath
        }
    }

    private fun hasRequiredLibraries(dir: File): Boolean =
        dir.isDirectory && REQUIRED_LIBRARIES.all { File(dir, it).isFile && File(dir, it).length() > 0L }

    override fun start(configPath: String, tunFd: Int, tunInterface: TunInterface): Result<Unit> = runCatching {
        AppLogger.i("CORE", "Starting core; config=$configPath tunFd=$tunFd")
        ensureLoaded()

        val setupLatch = CountDownLatch(1)
        var setupError: String? = null

        val home = context.filesDir.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")
        val configFile = File(context.filesDir, "config.yaml")
        check(configFile.isFile) { "Core config missing: ${configFile.absolutePath}" }
        AppLogger.i("CORE", "Core home=${context.filesDir.absolutePath}; config=${configFile.absolutePath} size=${configFile.length()}")

        // libmihomo/FlClash core expects `home-dir` (not `homeDir`) and always
        // loads <home-dir>/config.yaml. SetupParams does not contain a profile path.
        val initJson = """{"home-dir":"$home","version":${Build.VERSION.SDK_INT}}"""
        val setupJson = """{"selected-map":{},"test-url":"https://www.gstatic.com/generate_204"}"""

        AppLogger.i("CORE", "Calling quickSetup with home-dir and SDK=${Build.VERSION.SDK_INT}")
        Clash.quickSetup(
            initParams = initJson,
            setupParams = setupJson
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

    private companion object {
        val REQUIRED_LIBRARIES = listOf("libclash.so", "libmihomo-jni.so")
    }
}
