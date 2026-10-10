plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.devos.ai.data.ai"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            all { it.useJUnitPlatform() }
        }
    }
}

dependencies {
    implementation(project(":domain:domain-ai"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-security"))
    implementation(project(":core:core-common"))

    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.datastore)
    implementation(libs.bundles.network)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.work)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.timber)

    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.test.mockwebserver)
    testImplementation(libs.test.org.json)
    testRuntimeOnly(libs.test.junit5.engine)
    testRuntimeOnly(libs.test.junit5.launcher)
}
