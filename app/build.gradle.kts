import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    android {
        minSdk = 21
        compileSdk = 37
        buildToolsVersion = "37.0.0"
        namespace = "com.kyant.backdrop.catalog.common"
        androidResources.enable = true
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    applyDefaultHierarchyTemplate()

    jvm("desktop")

    js {
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
        val commonMain = getByName("commonMain") {
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

        val androidMain = getByName("androidMain") {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }

        val skikoMain = create("skikoMain") {
            dependsOn(commonMain)
        }

        val desktopMain = getByName("desktopMain") {
            dependsOn(skikoMain)
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val macosArm64Main = getByName("macosArm64Main") {
            dependsOn(skikoMain)
        }

        val iosMain = getByName("iosMain") {
            dependsOn(skikoMain)
        }

        val iosArm64Main = getByName("iosArm64Main") {
        }

        val iosSimulatorArm64Main = getByName("iosSimulatorArm64Main") {
        }

        val jsMain = getByName("jsMain") {
            dependsOn(skikoMain)
        }

        val wasmJsMain = getByName("wasmJsMain") {
            dependsOn(skikoMain)
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
