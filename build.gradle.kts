plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("org.jetbrains.intellij") version "1.9.0"
}

group = "top.fallenangel"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.15")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set("JimmerGenerator")
    version.set("2021.3.3")
    type.set("IU") // Target IDE Platform
    plugins.set(
        listOf(
            "java",
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
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("223.*")
    }

    publishPlugin {
        channels.add("Stable")
    }
}
