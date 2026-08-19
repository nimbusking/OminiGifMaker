import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.ominigifmaker.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "OminiGifMaker"
            packageVersion = "1.0.0"

            // 绑定各平台专属图标
            windows {
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.icns"))
            }
            linux {
                iconFile.set(project.file("src/desktopMain/resources/icons/icon.png"))
            }
        }
    }
}
