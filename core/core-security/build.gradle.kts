plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.devos.ai.core.security"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.timber)
    ksp(libs.hilt.compiler)
    testImplementation(libs.bundles.test.unit)
    testRuntimeOnly(libs.test.junit5.engine)
}
