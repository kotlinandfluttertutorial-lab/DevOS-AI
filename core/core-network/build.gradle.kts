plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.devos.ai.core.network"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:core-security"))
    api(libs.bundles.network)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.test.mockwebserver)
    testRuntimeOnly(libs.test.junit5.engine)
}
