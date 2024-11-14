import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion by extra("1.8.20")
val jacksonVersion by extra("2.15.2")

val sinceVersion by extra("223.7571.182")
val untilVersion by extra("243.*")

val certificateChainValue: String = findProperty("certificateChainValue") as String
val privateKeyValue: String = findProperty("privateKeyValue") as String
val passwordValue: String = findProperty("passwordValue") as String
val tokenValue: String = findProperty("tokenValue") as String

plugins {
    id("org.jetbrains.intellij") version "1.13.2"
    id("java")
    kotlin("jvm") version "1.8.20"
}

group = "top.fallenangel"
version = "0.3.11"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set("JimmerGenerator")
    type.set("IU") // Target IDE Platform
    version.set("2022.3")
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
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<KotlinCompile> {
        kotlinOptions.apply {
            jvmTarget = "17"
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
