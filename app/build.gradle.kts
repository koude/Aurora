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

    implementation(libs.kotlin.coroutine)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.coordinator)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
    implementation(libs.quickie.bundled)
    implementation(libs.androidx.activity.ktx)
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