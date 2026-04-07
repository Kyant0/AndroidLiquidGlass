plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

fun secret(name: String): String? = providers.gradleProperty(name).orNull ?: System.getenv(name)

val releaseStoreFilePath = secret("VORMEX_RELEASE_STORE_FILE")
val releaseStorePassword = secret("VORMEX_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = secret("VORMEX_RELEASE_KEY_ALIAS")
val releaseKeyPassword = secret("VORMEX_RELEASE_KEY_PASSWORD")
val releaseApiBaseUrl = secret("VORMEX_RELEASE_API_BASE_URL") ?: "https://vormex-backend.onrender.com/api"
val releaseSocketBaseUrl = secret("VORMEX_RELEASE_SOCKET_BASE_URL") ?: "https://vormex-backend.onrender.com"
// Physical devices can reach the host machine through adb reverse on localhost.
val localDebugApiBaseUrl = "http://127.0.0.1:5000/api"
val localDebugSocketBaseUrl = "http://127.0.0.1:5000"
val debugApiBaseUrl = secret("VORMEX_DEBUG_API_BASE_URL") ?: localDebugApiBaseUrl
val debugSocketBaseUrl = secret("VORMEX_DEBUG_SOCKET_BASE_URL") ?: localDebugSocketBaseUrl
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.kyant.backdrop.catalog"
    compileSdk {
        version = release(36)
    }
    buildToolsVersion = "36.1.0"

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    defaultConfig {
        applicationId = "com.vormex.android"
        minSdk = 23
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.3"
        androidResources.localeFilters += arrayOf("en")
        buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
        buildConfigField("String", "SOCKET_BASE_URL", "\"$releaseSocketBaseUrl\"")
        // Release must not allow HTTP; debug needs cleartext for local dev (e.g. adb reverse to localhost).
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            buildConfigField("String", "SOCKET_BASE_URL", "\"$debugSocketBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            vcsInfo.include = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    implementation(libs.kotlinx.collections.immutable)
    
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
    implementation("io.coil-kt:coil-gif:2.6.0")

    // Lottie (e.g. streak fire on profile)
    implementation(libs.lottie.compose)

    // Razorpay's published AARs still reuse the com.razorpay namespace across modules,
    // which AGP 9 rejects during manifest processing. These patched local copies only
    // change the library manifest package and keep the shipped classes/resources intact.
    implementation(files("libs/razorpay-standard-core-1.7.10-patched.aar"))
    implementation(files("libs/razorpay-core-1.0.10-patched.aar"))
    implementation("androidx.startup:startup-runtime:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.2")
    implementation("com.google.android.gms:play-services-tasks:17.2.1")
    implementation("com.google.android.gms:play-services-wallet:18.1.3")

    // Room cache
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
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

    testImplementation("junit:junit:4.13.2")
}
