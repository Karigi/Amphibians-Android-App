plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Hilt + Kotlin Serialization + ksp
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.example.amphibians"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.amphibians"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    //=========== New --> check [libs.versions.toml] file===========
    // Material3 Window Size Class
    implementation(libs.androidx.compose.material3.windowSize)
    // Material3 Icons
    implementation(libs.androidx.compose.material.icons.extended)
    // ViewModel Libraries
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Runtime Libraries
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Serialization Libraries
    implementation(libs.kotlinx.serialization.core)
    // Retrofit Libraries
    implementation(libs.retrofit2.retrofit)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    // OkHttp Libraries
    implementation(libs.okhttp3.okhttp)
    // Kotlin Serialization Json Libraries
    implementation(libs.kotlinx.serialization.json)
    // Hilt Libraries
    implementation(libs.dagger.hilt.android)
    // Foundation Layout Libraries
    implementation(libs.androidx.compose.foundation.layout)
    // Hilt Compiler Libraries
    ksp(libs.androidx.hilt.ksp.compiler)
    // Hilt ViewModel Libraries
    implementation(libs.androidx.hilt.viewmodel)
    // Hilt Navigation Libraries
    implementation(libs.androidx.hilt.navigation.compose)
    // Core Navigation3 Libraries
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    // Navigation3 + ViewModel Libraries
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    // Navigation3 + Adaptive Material3 Libraries
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    // Coil
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
    //===================================================================

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}