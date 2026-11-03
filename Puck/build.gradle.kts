plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "fr.lip6.move"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// The IDE to build against. Set the Gradle property `puck.ide` to the path of a local
// IntelliJ installation (for instance in ~/.gradle/gradle.properties); otherwise an
// IntelliJ IDEA Community distribution is downloaded.
val localIde = providers.gradleProperty("puck.ide")

dependencies {
    intellijPlatform {
        if (localIde.isPresent) local(localIde.get()) else intellijIdeaCommunity("2025.1")
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }
    sandboxContainer = layout.projectDirectory.dir("idea-sandbox")
    buildSearchableOptions = false
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    runIde {
        jvmArgs("-Xmx4g", "-Didea.trust.all.projects=true", "-Didea.no.splash=true", "-Djava.awt.headless=true")
        // Command-line arguments for the sandbox IDE, separated by ";", e.g. the headless inspector:
        // gradlew runIde "-PideArgs=inspect;D:/repos/chessKRK/Before;D:/repos/chessKRK/Puck/inspection/puck-only.xml;D:/repos/chessKRK/Before/puck-out;-v2;-format;json"
        providers.gradleProperty("ideArgs").orNull?.let { args(it.split(";")) }
    }
}
