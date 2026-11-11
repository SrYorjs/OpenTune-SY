plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.arturo254.opentune.foundation"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
    }
    resourcePrefix = "original_"
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.material3)
    implementation(libs.palette)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.original.materialkolor)
    implementation(libs.original.appcompat)
    implementation(libs.original.splashscreen)
}
