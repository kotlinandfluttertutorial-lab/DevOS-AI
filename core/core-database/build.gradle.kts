plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.devos.ai.core.database"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    room { schemaDirectory("$projectDir/schemas") }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.test.room)
    testImplementation(libs.test.android.junit)
    testImplementation(libs.test.junit4)
    testImplementation(libs.test.robolectric)
    // Robolectric needs the android-all SDK jar pre-fetched via Gradle
    // so CI doesn't attempt a runtime Maven download.
    testRuntimeOnly(libs.robolectric.android.all)
}
