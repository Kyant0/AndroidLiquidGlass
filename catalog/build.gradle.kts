plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.kyant.backdrop.catalog"
    compileSdk {
        version = release(36)
    }
    buildToolsVersion = "36.1.0"

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/vormex-release.keystore")
            storePassword = "vormex123"
            keyAlias = "vormex"
            keyPassword = "vormex123"
        }
    }

    defaultConfig {
        applicationId = "com.vormex.android"
        minSdk = 23
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.2"
        androidResources.localeFilters += arrayOf("en")
        buildConfigField("String", "API_BASE_URL", "\"https://vormex-backend.onrender.com/api\"")
        buildConfigField("String", "SOCKET_BASE_URL", "\"https://vormex-backend.onrender.com\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            vcsInfo.include = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += arrayOf(
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "kotlin/**",
                "META-INF/*.version",
                "META-INF/**/LICENSE.txt"
            )
        }
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    lint {
        checkReleaseBuilds = false
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xlambdas=class"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material.ripple)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kyant.shapes)
    implementation(project(":backdrop"))
    
    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Network
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // DataStore for token storage
    implementation(libs.androidx.datastore.preferences)
    
    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    
    // Google Sign-In (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)
    
    // Google Play Services Location
    implementation(libs.google.play.services.location)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Media3 (ExoPlayer) for video playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Socket.IO for real-time chat
    implementation(libs.socketio.client) {
        exclude(group = "org.json", module = "json")
    }
    
    // Firebase (Push Notifications)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
}
