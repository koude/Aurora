rootProject.name = "Aurora"

include(":app")
include(":core")
include(":service")
include(":design")
include(":common")
include(":hideapi")
include(":designsystem")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
