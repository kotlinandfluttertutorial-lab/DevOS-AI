plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.devos.ai.designsystem"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.bundles.compose)
    api(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil)
    implementation(libs.commonmark)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.test.junit5.api)
    testImplementation(libs.test.assertk)
}
