// Lets Gradle download the JDK 21 the build needs when none is installed (toolchain auto-provisioning).
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "puck4"
