import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.osdetector") version "1.7.3"
}

group = "org.surfshield.sample"
version = "1.0-SNAPSHOT"
val appPackageVersion = "1.2.0"

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    mavenCentral()
    maven {
        url = uri("https://repo.surfshield.org/repository/maven-public")
    }
}

dependencies {
    implementation("com.github.shiroedev2024:leaf-java-sdk:1.4.0")

    val os = osdetector.os
    val arch = osdetector.arch
    val leafVersion = "2.1.0"

    when {
        os == "linux" && arch == "x86_64" -> {
            implementation("com.github.shiroedev2024:jni-wrapper:$leafVersion-linux-64")
        }
        os == "linux" && arch == "x86_32" -> {
            implementation("com.github.shiroedev2024:jni-wrapper:$leafVersion-linux-32")
        }
        os == "windows" && arch == "x86_64" -> {
            implementation("com.github.shiroedev2024:jni-wrapper:$leafVersion-windows-64")
        }
        os == "windows" && arch == "x86_32" -> {
            implementation("com.github.shiroedev2024:jni-wrapper:$leafVersion-windows-32")
        }
        os == "osx" && arch == "x86_64" -> {
            implementation("com.github.shiroedev2024:jni-wrapper:$leafVersion-macos-64")
        }
        os == "osx" && arch == "aarch_64" -> {
            implementation("com.github.shiroedev2024:jni-wrapper:$leafVersion-macos-arm64")
        }
        else -> {
            throw GradleException("Unsupported OS or architecture: $os $arch")
        }
    }

    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "surfshield-desktop-sample"
            packageVersion = appPackageVersion
        }
    }
}

tasks.register("generateVersionProperties") {
    doLast {
        val out = file("$buildDir/resources/main/version.properties")
        out.parentFile.mkdirs()
        out.writeText("version=${project.version}\npackageVersion=$appPackageVersion\n")
    }
}

tasks.named("processResources") {
    dependsOn("generateVersionProperties")
}
