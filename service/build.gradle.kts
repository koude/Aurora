plugins {
    kotlin("android")
    id("kotlinx-serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
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
    implementation(project(":core"))
    implementation(project(":common"))

    ksp("com.github.kr328.kaidl:kaidl:1.15")
    ksp("androidx.room:room-compiler:2.4.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.room:room-runtime:2.4.2")
    implementation("androidx.room:room-ktx:2.4.2")
    implementation("com.github.kr328.kaidl:kaidl-runtime:1.15")
    implementation("dev.rikka.rikkax.preference:multiprocess:1.0.0")
}

afterEvaluate {
    android {
        libraryVariants.forEach { variant ->
            sourceSets[variant.name].kotlin.srcDir(buildDir.resolve("generated/ksp/${variant.name}/kotlin"))
            sourceSets[variant.name].java.srcDir(buildDir.resolve("generated/ksp/${variant.name}/java"))
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
