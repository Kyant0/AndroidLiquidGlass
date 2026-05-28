import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    id("com.vanniktech.maven.publish")
}

kotlin {
    android {
        minSdk = 21
        compileSdk = 37
        buildToolsVersion = "37.0.0"
        namespace = "com.kyant.backdrop"
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

    macosArm64()
    iosArm64("iosArm64")
    iosSimulatorArm64("iosSimulatorArm64")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.graphics)
                implementation(libs.kyant.shapes)
                implementation("org.jetbrains:annotations:26.1.0")
            }
        }

        val skikoMain by creating {
            dependsOn(commonMain)
        }

        val desktopMain by getting {
            dependsOn(skikoMain)
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
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("io.github.kyant0", "backdrop", "2.0.0")

    pom {
        name.set("Backdrop")
        description.set("Compose Multiplatform Liquid Glass effects")
        inceptionYear.set("2025")
        url.set("https://github.com/Kyant0/AndroidLiquidGlass")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("Kyant0")
                name.set("Kyant")
                url.set("https://github.com/Kyant0")
            }
        }
        scm {
            url.set("https://github.com/Kyant0/AndroidLiquidGlass")
            connection.set("scm:git:git://github.com/Kyant0/AndroidLiquidGlass.git")
            developerConnection.set("scm:git:ssh://git@github.com/Kyant0/AndroidLiquidGlass.git")
        }
    }
}
