plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.devos.ai.feature.testing"
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
    implementation(project(":designsystem"))
    implementation(project(":core:core-common"))
    implementation(project(":core:core-ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.bundles.test.unit)
    testRuntimeOnly(libs.test.junit5.engine)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(libs.test.hilt)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.test.compose.manifest)
}
