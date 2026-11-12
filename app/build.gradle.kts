import java.net.URL
import java.net.HttpURLConnection
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.zip.ZipFile
import java.util.Collections
import java.security.MessageDigest

plugins {
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.application")
}

android {
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.add("**/libaurora_root_patch.so")
        }
    }
}

val rootCoreOutput = layout.buildDirectory.dir("generated/rootExecutables")
val localBuildProperties = rootProject.file("local.properties").takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } } ?: Properties()
val rootGoExecutable = localBuildProperties.getProperty("go.executable")
val rootSdkDirectory = localBuildProperties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
val rootNdkDirectory = rootSdkDirectory?.let { file("$it/ndk/29.0.14206865") }

val buildRootCore by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine("sh", rootProject.file("tools/build-root-core.sh").absolutePath, rootCoreOutput.get().asFile.absolutePath)
    rootGoExecutable?.takeIf(String::isNotBlank)?.let { environment("AURORA_GO", it) }
    rootNdkDirectory?.let { environment("AURORA_NDK_HOME", it.absolutePath) }
    inputs.dir(rootProject.file("core/src/foss/golang/clash"))
    inputs.file(rootProject.file("tools/build-root-core.sh"))
    outputs.dir(rootCoreOutput)
}

androidComponents.onVariants { variant ->
    val variantName = variant.name
    val capitalized = variantName.replaceFirstChar { it.uppercase() }
    val patchOutput = layout.buildDirectory.dir("generated/rootPatches/$variantName")
    val coreJniDir = rootProject.layout.projectDirectory.dir(
        "core/build/intermediates/library_jni/$variantName/copy${capitalized}JniLibsProjectOnly/jni"
    )
    val patchTask = tasks.register<Exec>("build${capitalized}RootPatches") {
        dependsOn(buildRootCore, ":core:copy${capitalized}JniLibsProjectOnly")
        workingDir = rootProject.projectDir
        commandLine(
            "sh", rootProject.file("tools/build-root-patches.sh").absolutePath,
            patchOutput.get().asFile.absolutePath, variantName,
            rootCoreOutput.get().asFile.absolutePath,
            requireNotNull(rootSdkDirectory) { "Android SDK is required for ROOT patches" },
        )
        rootGoExecutable?.takeIf(String::isNotBlank)?.let { environment("AURORA_GO", it) }
        inputs.dir(coreJniDir)
        inputs.dir(rootCoreOutput)
        inputs.file(rootProject.file("tools/build-root-patches.sh"))
        inputs.file(rootProject.file("tools/root-patch/main.go"))
        inputs.file(rootProject.file("tools/root-patch-stub.c"))
        outputs.dir(patchOutput)
    }
    android.sourceSets.getByName(variantName).jniLibs.srcDir(patchOutput)
    tasks.configureEach {
        if (name == "pre${capitalized}Build") dependsOn(patchTask)
        if (name == "merge${capitalized}JniLibFolders") {
            dependsOn(patchTask)
            inputs.dir(patchOutput)
        }
    }
    val verifyTask = tasks.register("verify${capitalized}RootPatches") {
        dependsOn("package${capitalized}")
        doLast {
            val apkDir = layout.buildDirectory.dir(
                "outputs/apk/${variant.flavorName}/${variant.buildType}"
            ).get().asFile
            val apks = apkDir.listFiles { file -> file.extension == "apk" }.orEmpty()
            check(apks.isNotEmpty()) { "No APK was produced for $variantName" }
            var checked = 0
            apks.forEach { apk ->
                ZipFile(apk).use { zip ->
                    val entries = Collections.list(zip.entries())
                    entries.filter { it.name.endsWith("/libaurora_root_patch.so") }.forEach { entry ->
                        val abi = entry.name.removePrefix("lib/").substringBefore('/')
                        val patch = zip.getInputStream(entry).use { it.readBytes() }
                        val baseEntry = checkNotNull(zip.getEntry("lib/$abi/libclash.so")) {
                            "$apk: missing libclash.so for $abi"
                        }
                        val baseHash = MessageDigest.getInstance("SHA-256").digest(
                            zip.getInputStream(baseEntry).use { it.readBytes() }
                        )
                        check(patch.size >= 80 && patch.copyOfRange(patch.size - 8, patch.size)
                            .contentEquals("AURPATCH".toByteArray())) { "$apk: invalid ROOT patch for $abi" }
                        check(baseHash.contentEquals(patch.copyOfRange(patch.size - 72, patch.size - 40))) {
                            "$apk: ROOT patch does not match libclash.so for $abi"
                        }
                        checked++
                    }
                }
            }
            check(checked >= 4) { "ROOT patches are missing from $variantName APKs" }
        }
    }
    tasks.configureEach {
        if (name == "assemble$capitalized") dependsOn(verifyTask)
    }
}

dependencies {
    compileOnly(project(":hideapi"))

    implementation(project(":core"))
    implementation(project(":service"))
    implementation(project(":design"))
    implementation(project(":common"))
    implementation(project(":designsystem"))

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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
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
