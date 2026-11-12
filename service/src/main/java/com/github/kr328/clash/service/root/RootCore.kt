package com.github.kr328.clash.service.root

import android.content.Context
import android.os.Process
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.Imported
import com.github.kr328.clash.service.util.importedDir
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/** Owns Aurora's separate privileged core. No box module files are touched. */
class RootCore(private val context: Context) {
    private val rootDir = "/data/adb/aurora/${context.packageName}"
    private val rootProfile = "$rootDir/profile"
    private val controlUrl = "http://127.0.0.1:19090"

    var secret: String? = null
        private set

    private val secretFile: File get() = context.filesDir.resolve("aurora-root-secret")
    private val installedMarker: File get() = context.filesDir.resolve("aurora-root-installed")

    fun hasInstallationRecord(): Boolean = installedMarker.isFile

    fun isInstalled(): Boolean = installedMarker.isFile &&
        installedMarker.readText() == context.packageManager
            .getPackageInfo(context.packageName, 0).lastUpdateTime.toString()

    /** Installation is deliberately separate from selecting or starting ROOT mode. */
    fun install() {
        check(runRoot("id -u").trim() == "0") { "ROOT permission was not granted" }
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val base = nativeDir.resolve("libclash.so")
        val patch = nativeDir.resolve("libaurora_root_patch.so")
        check(base.isFile && patch.isFile) { "ROOT patch is missing from this APK" }
        val restored = context.filesDir.resolve("aurora-root-restored")
        try {
            check(Clash.restoreRootCore(base, patch, restored)) { "Unable to restore ROOT core; please reinstall the APK" }
            runRoot(
                "set -e; mkdir -p ${quote(rootDir)}; chmod 700 ${quote(rootDir)}; " +
                    "cp ${quote(restored.absolutePath)} ${quote("$rootDir/mihomo.new")}; " +
                    "chmod 700 ${quote("$rootDir/mihomo.new")}; " +
                    "mv -f ${quote("$rootDir/mihomo.new")} ${quote("$rootDir/mihomo")}",
            )
        } finally {
            restored.delete()
        }
        installedMarker.writeText(context.packageManager
            .getPackageInfo(context.packageName, 0).lastUpdateTime.toString())
    }

    fun uninstall() {
        check(runRoot("id -u").trim() == "0") { "ROOT permission was not granted" }
        stop()
        // These names and table indices are reserved for Aurora's ROOT profile.
        runRoot(
            "set -e; " +
                "if command -v iptables >/dev/null 2>&1; then " +
                "for spec in 'nat OUTPUT aurora-output' 'filter INPUT aurora-input' " +
                "'filter FORWARD aurora-forward' 'nat PREROUTING aurora-prerouting'; do " +
                "set -- \$spec; while iptables -t \$1 -D \$2 -j \$3 2>/dev/null; do :; done; " +
                "iptables -t \$1 -F \$3 2>/dev/null || true; " +
                "iptables -t \$1 -X \$3 2>/dev/null || true; done; fi; " +
                "if command -v ip >/dev/null 2>&1; then " +
                "for family in -4 -6; do " +
                "while ip \$family rule show 2>/dev/null | grep -q 'lookup 20222'; do " +
                "ip \$family rule del table 20222 2>/dev/null || break; done; " +
                "ip \$family route flush table 20222 2>/dev/null || true; done; " +
                "ip link delete Aurora 2>/dev/null || true; fi; " +
                "if command -v iptables >/dev/null 2>&1; then " +
                "if iptables -t nat -S 2>/dev/null | grep -q 'aurora-'; then exit 1; fi; " +
                "if iptables -t filter -S 2>/dev/null | grep -q 'aurora-'; then exit 1; fi; fi; " +
                "if command -v ip >/dev/null 2>&1; then " +
                "if ip -4 rule show 2>/dev/null | grep -q 'lookup 20222'; then exit 1; fi; " +
                "if ip -6 rule show 2>/dev/null | grep -q 'lookup 20222'; then exit 1; fi; " +
                "if [ -n \"\$(ip -4 route show table 20222 2>/dev/null)\" ]; then exit 1; fi; " +
                "if [ -n \"\$(ip -6 route show table 20222 2>/dev/null)\" ]; then exit 1; fi; " +
                "if ip link show Aurora >/dev/null 2>&1; then exit 1; fi; fi; " +
                "rm -rf ${quote(rootDir)}; rmdir /data/adb/aurora 2>/dev/null || true",
        )
        secretFile.delete()
        context.filesDir.resolve("aurora-root-restored").delete()
        context.filesDir.resolve("aurora-root-config.yaml").delete()
        installedMarker.delete()
    }

    fun start(profile: Imported) {
        check(isInstalled()) { "请先到“设置 → 代理模式”安装透明代理" }
        check(runRoot("id -u").trim() == "0") { "ROOT permission was not granted" }
        stop()

        val source = context.importedDir.resolve(profile.uuid.toString())
        check(source.resolve("config.yaml").isFile) { "Selected profile is not available" }
        check(runCatching { runRoot("test -x ${quote("$rootDir/mihomo")}") }.isSuccess) {
            "透明代理安装不完整，请重新安装"
        }

        val key = ByteArray(32).also(SecureRandom()::nextBytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        Clash.setAgeSecretKey(profile.ageSecretKey?.takeIf(String::isNotBlank))
        val config = Clash.exportRootConfig(source, rootProfile, key)
        val staged = context.filesDir.resolve("aurora-root-config.yaml")
        staged.writeText(config)
        val geoCopies = listOf("geoip.metadb", "geosite.dat", "ASN.mmdb", "BundleMRS.7z")
            .map { context.filesDir.resolve("clash/$it") }
            .filter(File::isFile)
            .joinToString("; ") { "cp ${quote(it.absolutePath)} ${quote("$rootDir/${it.name}")}" }
        try {
            runRoot(
                "set -e; mkdir -p ${quote(rootDir)} ${quote(rootProfile)}; " +
                    "chmod 700 ${quote(rootDir)}; rm -rf ${quote(rootProfile)}; mkdir -p ${quote(rootProfile)}; " +
                    "cp -R ${quote(source.absolutePath + "/.")} ${quote(rootProfile)}; " +
                    (if (geoCopies.isNotEmpty()) "$geoCopies; " else "") +
                    "cp ${quote(staged.absolutePath)} ${quote("$rootProfile/config.yaml")}; " +
                    "chmod 600 ${quote("$rootProfile/config.yaml")}; " +
                    "(${quote("$rootDir/mihomo")} -d ${quote(rootDir)} " +
                    "-f ${quote("$rootProfile/config.yaml")} " +
                    (profile.ageSecretKey?.takeIf(String::isNotBlank)?.let { "-age-secret-key ${quote(it)} " } ?: "") +
                    "> ${quote("$rootDir/mihomo.log")} 2>&1 & echo \$! > ${quote("$rootDir/mihomo.pid")}); " +
                    "(core_pid=\$(cat ${quote("$rootDir/mihomo.pid")}); " +
                    "while kill -0 ${Process.myPid()} 2>/dev/null && kill -0 \$core_pid 2>/dev/null; " +
                    "do sleep 2; done; kill -TERM \$core_pid 2>/dev/null || true) " +
                    "</dev/null >/dev/null 2>&1 &",
            )
            secret = key
            repeat(40) {
                if (isHealthy() && runCatching {
                    runRoot("test -d /sys/class/net/Aurora")
                }.isSuccess) {
                    secretFile.writeText(key)
                    return
                }
                Thread.sleep(250)
            }
            error("ROOT core did not become ready; check $rootDir/mihomo.log")
        } catch (e: Exception) {
            runCatching { stop() }.onFailure(e::addSuppressed)
            throw e
        } finally {
            staged.delete()
        }
    }

    fun stop() {
        runRoot(
            "if [ -f ${quote("$rootDir/mihomo.pid")} ]; then " +
                "core_pid=\$(cat ${quote("$rootDir/mihomo.pid")}); " +
                "kill -TERM \$core_pid 2>/dev/null || true; " +
                "attempt=0; while kill -0 \$core_pid 2>/dev/null && [ \$attempt -lt 5 ]; do " +
                "sleep 1; attempt=\$((attempt + 1)); done; " +
                "if kill -0 \$core_pid 2>/dev/null; then exit 1; fi; " +
                "rm -f ${quote("$rootDir/mihomo.pid")}; fi",
        )
        secret = null
        secretFile.delete()
    }

    fun isHealthy(): Boolean = runCatching {
        val key = secret ?: return false
        val conn = URL("$controlUrl/version").openConnection() as HttpURLConnection
        conn.connectTimeout = 500
        conn.readTimeout = 500
        conn.setRequestProperty("Authorization", "Bearer $key")
        try { conn.responseCode == 200 } finally { conn.disconnect() }
    }.getOrDefault(false)

    private fun runRoot(command: String): String {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(true)
            .start()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (true) {
            val result = runCatching { process.exitValue() }.getOrNull()
            if (result != null) {
                val output = process.inputStream.bufferedReader().use { it.readText() }
                check(result == 0) { output.ifBlank { "ROOT command failed ($result)" } }
                return output
            }
            if (System.nanoTime() >= deadline) {
                process.destroy()
                error("ROOT command timed out")
            }
            Thread.sleep(100)
        }
    }

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
