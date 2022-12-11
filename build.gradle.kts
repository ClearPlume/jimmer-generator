plugins {
    id("org.jetbrains.intellij") version "1.10.1"
    id("java")
    kotlin("jvm") version "1.7.21"
}

group = "top.fallenangel"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.21")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.20")
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

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.apply {
            jvmTarget = "11"
            apiVersion = "1.7"
        }
    }

    patchPluginXml {
        sinceBuild.set("203")
        untilBuild.set("223.*")
    }

    runIde {
        jvmArgs("-Xms128m", "-Xmx4096m", "-XX:ReservedCodeCacheSize=512m")
    }

    publishPlugin {
        channels.add("Stable")
    }
}
