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

    applyDefaultHierarchyTemplate()

    jvm("desktop")

    js {
        browser()
    }
    wasmJs {
        browser()
    }

    macosArm64()
    iosArm64("iosArm64")
    iosSimulatorArm64("iosSimulatorArm64")

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.graphics)
                implementation(libs.kyant.shapes)
                implementation("org.jetbrains:annotations:26.1.0")
            }
        }

        val skikoMain = create("skikoMain") {
            dependsOn(commonMain)
        }

        val desktopMain = getByName("desktopMain") {
            dependsOn(skikoMain)
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
