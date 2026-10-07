plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.devos.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devos.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "com.devos.ai.HiltTestRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    // Core
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-security"))
    implementation(project(":core:core-ui"))

    // Data modules (Hilt bindings)
    implementation(project(":data:data-ai"))
    implementation(project(":data:data-repository"))

    // Design system
    implementation(project(":designsystem"))

    // Feature modules
    implementation(project(":feature:feature-auth"))
    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-project"))
    implementation(project(":feature:feature-repository"))
    implementation(project(":feature:feature-code"))
    implementation(project(":feature:feature-ai-chat"))
    implementation(project(":feature:feature-agents"))
    implementation(project(":feature:feature-git"))
    implementation(project(":feature:feature-issues"))
    implementation(project(":feature:feature-prs"))
    implementation(project(":feature:feature-security"))
    implementation(project(":feature:feature-testing"))
    implementation(project(":feature:feature-learning"))
    implementation(project(":feature:feature-memory"))
    implementation(project(":feature:feature-settings"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splash)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.window)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)

    // WorkManager
    implementation(libs.androidx.work)

    // Testing
    testImplementation(libs.test.junit5.api)
    testRuntimeOnly(libs.test.junit5.engine)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(libs.test.hilt)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.test.compose.manifest)
}
