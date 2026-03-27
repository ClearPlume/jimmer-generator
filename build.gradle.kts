import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val sinceVersion by extra("241.14494.240")
val untilVersion by extra("261.*")

val certificateChainValue: String = findProperty("certificateChainValue") as String
val privateKeyValue: String = findProperty("privateKeyValue") as String
val passwordValue: String = findProperty("passwordValue") as String
val tokenValue: String = findProperty("tokenValue") as String

plugins {
    java

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.changelog)
}

group = "top.fallenangel"
version = "0.3.15"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate(sinceVersion) {
            useInstaller = false
        }
        bundledPlugins("com.intellij.java", "com.intellij.database")

        testFramework(TestFrameworkType.Platform)
    }

    implementation(libs.kotlin.reflect)
    implementation(libs.jackson)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

changelog {
    keepUnreleasedSection = false
    unreleasedTerm = "Unreleased"
    groups = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed")
    headerParserRegex =
        """^((0|[1-9]\d*)(\.(0|[1-9]\d*)){2,3}(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)$"""
}

intellijPlatform {
    pluginConfiguration {
        id = "top.fallenangel.jimmer-generator"
        name = "Jimmer-Generator"

        description = markdownToHTML(File(projectDir, "README.md").readText())
        changeNotes = changelog.render(Changelog.OutputType.HTML)

        vendor {
            name = "the_FallenAngel"
            email = "the.fallenangel.965@gmail.com"
            url = "https://fallingangel.net"
        }

        ideaVersion {
            sinceBuild = sinceVersion
            untilBuild = untilVersion
        }
    }

    pluginVerification {
        ides.recommended()
    }

    publishing {
        token = tokenValue
        channels.add("Stable")
    }

    signing {
        certificateChain = certificateChainValue
        privateKey = privateKeyValue
        password = passwordValue
    }
}

tasks {
    runIde {
        jvmArgs("-Xms128m", "-Xmx4096m", "-XX:ReservedCodeCacheSize=512m")
    }

    test {
        systemProperty("idea.home.path", intellijPlatform.sandboxContainer.get().toString())
    }
}
