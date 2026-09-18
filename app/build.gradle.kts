import java.net.URL
import java.net.HttpURLConnection
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    kotlin("android")
    kotlin("kapt")
    id("com.android.application")
}

dependencies {
    compileOnly(project(":hideapi"))

    implementation(project(":core"))
    implementation(project(":service"))
    implementation(project(":design"))
    implementation(project(":common"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.activity:activity:1.5.0")
    implementation("androidx.fragment:fragment:1.5.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("io.github.g00fy2.quickie:quickie-bundled:1.11.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
}

tasks.getByName("clean", type = Delete::class) {
    delete(file("release"))
}

val geoFilesDownloadDir = "src/main/assets"

task("downloadGeoFiles") {
    val geoFilesUrls = mapOf(
        "geoip.metadb" to listOf(
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geoip.metadb",
            "https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geoip.metadb",
            "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geoip.metadb",
        ),
        "geosite.dat" to listOf(
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geosite.dat",
            "https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geosite.dat",
            "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/geosite.dat",
        ),
        "ASN.mmdb" to listOf(
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/GeoLite2-ASN.mmdb",
            "https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/GeoLite2-ASN.mmdb",
            "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/GeoLite2-ASN.mmdb",
        ),
        "BundleMRS.7z" to listOf(
            "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/BundleMRS.7z",
            "https://testingcf.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/BundleMRS.7z",
            "https://cdn.jsdelivr.net/gh/MetaCubeX/meta-rules-dat@release/BundleMRS.7z",
        ),
    )

    doLast {
        geoFilesUrls.forEach { (outputFileName, urls) ->
            val outputPath = file("$geoFilesDownloadDir/$outputFileName")
            outputPath.parentFile.mkdirs()

            var lastError: Exception? = null
            var downloaded = false

            for (downloadUrl in urls) {
                for (attempt in 1..3) {
                    if (downloaded) break
                    try {
                        println("Downloading $outputFileName from $downloadUrl (attempt $attempt/3)")
                        val connection = URL(downloadUrl).openConnection() as HttpURLConnection
                        connection.instanceFollowRedirects = true
                        connection.connectTimeout = 20000
                        connection.readTimeout = 60000
                        connection.setRequestProperty("User-Agent", "Aurora-CMFA-Build")

                        connection.inputStream.use { input ->
                            Files.copy(input, outputPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                        connection.disconnect()

                        if (!outputPath.exists() || outputPath.length() == 0L) {
                            throw IOException("$outputFileName downloaded as an empty file")
                        }

                        println("$outputFileName downloaded to $outputPath (${outputPath.length()} bytes)")
                        downloaded = true
                    } catch (e: Exception) {
                        lastError = e
                        println("Download failed for $outputFileName: ${e.message}")
                        if (outputPath.exists()) outputPath.delete()
                        Thread.sleep(1500L * attempt)
                    }
                }
                if (downloaded) break
            }

            if (!downloaded) {
                throw GradleException(
                    "Unable to download $outputFileName from all configured sources",
                    lastError
                )
            }
        }
    }
}

afterEvaluate {
    val downloadGeoFilesTask = tasks["downloadGeoFiles"]

    tasks.forEach {
        if (it.name.startsWith("assemble")) {
            it.dependsOn(downloadGeoFilesTask)
        }
    }
}

tasks.getByName("clean", type = Delete::class) {
    delete(file(geoFilesDownloadDir))
}
