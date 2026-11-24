plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vscode.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vscode.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.activity:activity:1.9.3")
    // WebViewAssetLoader: serves the packaged bundle from a real HTTPS origin
    // instead of file://, so ES modules, workers and IndexedDB all work.
    implementation("androidx.webkit:webkit:1.14.0")
}
