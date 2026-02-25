plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.arinterior.engine"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.arinterior.engine"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // API Gateway base URL — replace with your actual endpoint after SAM deploy
        buildConfigField("String", "API_BASE_URL", "\"https://rmaqk5mi10.execute-api.us-east-1.amazonaws.com/prod\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // ─── Kotlin & Coroutines ─────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ─── Jetpack Compose ─────────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ─── ARCore + SceneView ──────────────────────────────
    implementation("com.google.ar:core:1.41.0")
    implementation("io.github.sceneview:arsceneview:2.1.0")

    // ─── Google ML Kit (Subject Segmentation) ────────────
    implementation("com.google.mlkit:subject-segmentation:16.0.0-beta1")

    // ─── Networking (Retrofit → API Gateway) ─────────────
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ─── Image Loading ───────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ─── AndroidX Core ───────────────────────────────────
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
