plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.devos.ai.domain.learning"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines)
    // javax.inject for @Inject annotations in use cases (pure JVM — no Android class)
    compileOnly("javax.inject:javax.inject:1")
    testImplementation(libs.bundles.test.unit)
    testRuntimeOnly(libs.test.junit5.engine)
    testRuntimeOnly(libs.test.junit5.launcher)
}
