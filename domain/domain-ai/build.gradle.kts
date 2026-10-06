// Pure Kotlin module — no Android dependencies
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.devos.ai.domain.ai"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Domain modules are pure Kotlin — no Android, no Compose, no Room
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.bundles.test.unit)
    testRuntimeOnly(libs.test.junit5.engine)
}
