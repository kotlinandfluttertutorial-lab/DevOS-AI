# Kiro Prompt — DEVOS-001: Android Project Setup

**Jira:** DEVOS-001  
**Epic:** DEVOS-E01  
**Figma:** N/A  
**Kiro Spec:** `.kiro/specs/foundation/android-setup.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are scaffolding the DevOS AI Android project — a multi-module Gradle project using Kotlin, Jetpack Compose, Material 3, Clean Architecture, Hilt, and Room.

**Existing files to read first:**
- `.kiro/specs/foundation/android-setup.md` — full module list
- `docs/architecture/android-architecture.md` — architecture patterns

**Workspace root:** `j:\Android\AndroidStudioProjects\Kiro\DevOS\DevOS-AI`

**What to create:**

### settings.gradle.kts
Include all modules:
```kotlin
rootProject.name = "DevOS-AI"
include(":app")
include(":core:core-common")
include(":core:core-network")
include(":core:core-database")
include(":core:core-security")
include(":core:core-ui")
include(":core:core-testing")
include(":designsystem")
// All feature, domain, data modules...
```

### gradle/libs.versions.toml
```toml
[versions]
kotlin = "2.0.21"
agp = "8.7.0"
compose-bom = "2024.09.03"
hilt = "2.52"
room = "2.6.1"
navigation = "2.8.3"
retrofit = "2.11.0"
okhttp = "4.12.0"
coil = "2.7.0"
coroutines = "1.9.0"
# ... etc

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-navigation = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
# ... etc

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.27" }
```

### app/build.gradle.kts
```kotlin
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
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":designsystem"))
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-database"))
    implementation(project(":core:core-security"))
    // All feature modules
    implementation(project(":feature:feature-auth"))
    // ... etc
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

### app/src/main/kotlin/com/devos/ai/
Create:
- `MainActivity.kt` — single activity, `DevOSNavGraph()`, edge-to-edge
- `DevOSApp.kt` — `@HiltAndroidApp Application`
- `navigation/DevOSNavGraph.kt` — `NavHost` with all route registrations
- `di/AppModule.kt` — Room DB, OkHttp, base URL providers

### core/core-database/
- `DevOSDatabase.kt` — Room database with version 1
- Initial entities: `MessageEntity`, `RepositoryEntity`, `FileEntity`, `SymbolEntity`, `MemoryEntity`

**Acceptance Criteria:**
- [ ] `./gradlew assembleDebug` succeeds
- [ ] All modules compile without errors
- [ ] App launches on API 26+ emulator showing SplashScreen
- [ ] Hilt injection works (no `@Inject` constructor failures)
- [ ] Room DB opens without error
- [ ] `./gradlew test` passes with no failures
