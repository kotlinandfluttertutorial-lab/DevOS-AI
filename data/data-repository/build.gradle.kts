plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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
        unitTests.all { it.useJUnitPlatform() }
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Domain contract — never import data-repository from feature modules
    implementation(project(":domain:domain-repository"))

    // DAOs + entities from core-database
    implementation(project(":core:core-database"))

    // Secure token storage
    implementation(project(":core:core-security"))

    // Auth provider (OAuthProvider implements TokenKey)
    implementation(project(":feature:feature-auth"))

    // WorkManager + Hilt worker support
    implementation(libs.androidx.work)
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines)

    // Logging (token masking; never log raw values)
    implementation(libs.timber)

    // JGit — no alias in libs.versions.toml; pinned version per spec
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")

    // Testing
    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.test.room)
    testImplementation(libs.test.android.junit)
    testRuntimeOnly(libs.test.junit5.engine)
    testRuntimeOnly(libs.test.junit5.launcher)
    // WorkManager test helpers (WorkInfo constructor, TestListenableWorkerBuilder)
    testImplementation("androidx.work:work-testing:2.9.1")
}
