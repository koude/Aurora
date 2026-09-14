plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val auroraCoreVersion = "0.3.1"

val auroraKeystorePath = System.getenv("AURORA_KEYSTORE_PATH")
val auroraStorePassword = System.getenv("AURORA_STORE_PASSWORD")
val auroraKeyPassword = System.getenv("AURORA_KEY_PASSWORD")
val auroraCoreAar = layout.buildDirectory.file("core/aurora-core-v$auroraCoreVersion.aar")

val downloadAuroraCore = tasks.register("downloadAuroraCore") {
    inputs.property("auroraCoreVersion", auroraCoreVersion)
    outputs.file(auroraCoreAar)

    doLast {
        val target = auroraCoreAar.get().asFile
        if (!target.exists()) {
            target.parentFile.mkdirs()
            val source = uri(
                "https://github.com/oviron/libmihomo-android/releases/download/" +
                    "v$auroraCoreVersion/libmihomo-android-v$auroraCoreVersion.aar"
            ).toURL()
            source.openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

android {
    namespace = "com.koude.aurora"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.koude.aurora"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "0.3.1"

        vectorDrawables.useSupportLibrary = true

        // Aurora is currently aimed at modern physical Android phones.
        // Shipping only arm64 keeps the APK much smaller than bundling all three core ABIs.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (!auroraKeystorePath.isNullOrBlank() &&
            !auroraStorePassword.isNullOrBlank() &&
            !auroraKeyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(auroraKeystorePath)
                storePassword = auroraStorePassword
                keyAlias = "aurora"
                keyPassword = auroraKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    buildFeatures { compose = true }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    implementation(files(auroraCoreAar).builtBy(downloadAuroraCore))

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
