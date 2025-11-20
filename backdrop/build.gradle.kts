plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.kyant.backdrop"
    compileSdk {
        version = release(36)
    }
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
    }
}
kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters"
        )
    }
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("io.github.kyant0", "backdrop", "1.0.0")

    pom {
        name.set("Backdrop")
        description.set("Jetpack Compose blur and Liquid Glass effects")
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
