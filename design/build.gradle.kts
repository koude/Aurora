plugins {
    kotlin("android")
    kotlin("kapt")
    id("com.android.library")
}

val alphaKotlinClassesJar = tasks.register<Jar>("alphaKotlinClassesJar") {
    dependsOn("compileAlphaReleaseKotlin")
    archiveFileName.set("alpha-kotlin-classes.jar")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates/kotlin-classpath"))
    from(layout.buildDirectory.dir("tmp/kotlin-classes/alphaRelease"))
}

val metaKotlinClassesJar = tasks.register<Jar>("metaKotlinClassesJar") {
    dependsOn("compileMetaReleaseKotlin")
    archiveFileName.set("meta-kotlin-classes.jar")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates/kotlin-classpath"))
    from(layout.buildDirectory.dir("tmp/kotlin-classes/metaRelease"))
}

dependencies {
    implementation(project(":common"))
    implementation(project(":core"))
    implementation(project(":service"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.activity:activity:1.5.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.fragment:fragment:1.5.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.6.1")
}

afterEvaluate {
    android.libraryVariants.forEach { variant ->
        if (variant.buildType.name == "release") {
            variant.javaCompileProvider.configure {
                val kotlinClassesJar = if (variant.name == "alphaRelease") {
                    alphaKotlinClassesJar
                } else {
                    metaKotlinClassesJar
                }
                dependsOn(kotlinClassesJar)
                classpath = classpath.plus(files(kotlinClassesJar.flatMap { it.archiveFile }))
            }
        }
    }
}
