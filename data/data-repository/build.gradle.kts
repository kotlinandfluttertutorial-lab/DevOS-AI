plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.devos.ai.data.repository"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Domain contract this module implements
    implementation(project(":domain:domain-repository"))

    // Database (entities + DAOs)
    implementation(project(":core:core-database"))

    // Secure token access
    implementation(project(":core:core-security"))

    // Auth model (OAuthProvider implements TokenKey)
    implementation(project(":feature:feature-auth"))

    // Common utilities (IoDispatcher qualifier)
    implementation(project(":core:core-common"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // WorkManager + Hilt-Work integration
    implementation(libs.androidx.work)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // JGit — repository cloning
    implementation(libs.jgit)

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.test.room)
    testImplementation(libs.test.android.junit)
    testImplementation(libs.test.robolectric)
    testRuntimeOnly(libs.test.junit5.engine)
}
