import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    androidLibrary {
        minSdk = 21
        compileSdk = 36
        namespace = "com.kyant.backdrop.catalog.common"
        androidResources.enable = true
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    jvm("desktop")

    js(IR) {
        browser()
    }
    wasmJs {
        browser()
    }

    listOf(
        macosArm64(),
        iosArm64("iosArm64"),
        iosSimulatorArm64("iosSimulatorArm64")
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.graphics)
                implementation(libs.compose.resources)
                implementation(libs.compose.material.ripple)
                implementation(libs.kyant.shapes)
                implementation(project(":backdrop"))
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }

        val skikoMain by creating {
            dependsOn(commonMain)
        }

        val desktopMain by getting {
            dependsOn(skikoMain)
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val macosArm64Main by getting {
            dependsOn(skikoMain)
        }

        val iosMain by creating {
            dependsOn(skikoMain)
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val jsMain by getting {
            dependsOn(skikoMain)
        }

        val wasmJsMain by getting {
            dependsOn(skikoMain)
        }

        all {
            languageSettings.enableLanguageFeature("ContextParameters")
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xlambdas=class"
        )
    }
}

compose.desktop {
    application {
        mainClass = "com.kyant.backdrop.catalog.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.kyant.backdrop.catalog"
            packageVersion = "1.0.0"
        }
    }
}
