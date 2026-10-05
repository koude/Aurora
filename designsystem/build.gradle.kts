plugins {
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.library")
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.ui)

    api(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
