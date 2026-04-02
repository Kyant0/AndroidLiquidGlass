import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    androidLibrary {
        namespace = "com.kyant.backdrop"
        compileSdk = 36
        minSdk = 21
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    wasmJs {
        browser()
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters"
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.kyant.shapes)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("io.github.kyant0", "backdrop", "1.0.7")

    pom {
        name.set("Backdrop")
        description.set("Compose Multiplatform blur and Liquid Glass effects")
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
