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
        unitTests.all { it.useJUnitPlatform() }
        unitTests.isReturnDefaultValues = true
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
    testImplementation(libs.test.junit4)
    testImplementation(libs.test.robolectric)
    testRuntimeOnly(libs.test.junit5.engine)
    testRuntimeOnly(libs.test.junit5.vintage)
    testRuntimeOnly(libs.test.junit5.launcher)
}
