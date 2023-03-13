import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion by extra("1.8.0")
val kotlinxSerializationVersion by extra("1.5.0")

val sinceVersion by extra("203")
val untilVersion by extra("223.*")

val certificateChainValue: String by project
val privateKeyValue: String by project
val passwordValue: String by project
val tokenValue: String by project

plugins {
    id("org.jetbrains.intellij") version "1.13.2"
    id("java")
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

group = "top.fallenangel"
version = "0.3.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set("JimmerGenerator")
    type.set("IU") // Target IDE Platform
    version.set("2020.3.4")
    plugins.set(
        listOf(
            "com.intellij.java",
            "com.intellij.database"
        )
    )
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    withType<KotlinCompile> {
        kotlinOptions.apply {
            jvmTarget = "11"
            apiVersion = "1.8"
        }
    }

    patchPluginXml {
        sinceBuild.set(sinceVersion)
        untilBuild.set(untilVersion)
    }

    runIde {
        jvmArgs("-Xms128m", "-Xmx4096m", "-XX:ReservedCodeCacheSize=512m")
    }

    signPlugin {
        certificateChain.set(certificateChainValue)
        privateKey.set(privateKeyValue)
        password.set(passwordValue)
    }

    publishPlugin {
        channels.add("Stable")
        token.set(tokenValue)
    }
}
