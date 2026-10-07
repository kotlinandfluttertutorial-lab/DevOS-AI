# Implementation Plan — DEVOS-005 through DEVOS-012
# DevOS AI Foundation: Design System + Navigation + Auth

**Generated:** 2026-10-07  
**Scope:** DEVOS-005, DEVOS-006, DEVOS-007, DEVOS-008, DEVOS-009, DEVOS-010, DEVOS-011, DEVOS-012  
**Build command:** `./gradlew assembleDebug`  
**Test command:** `./gradlew testDebugUnitTest`

---

## What Already Exists (DO NOT recreate)

### `:designsystem` module — theme tokens only
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Color.kt` ✅ complete
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Typography.kt` ✅ complete (references R.font.jetbrains_mono_*)
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Shape.kt` ✅ complete
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Spacing.kt` ✅ complete
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/theme/Theme.kt` ✅ complete
- **MISSING:** `designsystem/src/main/res/font/` directory is empty — JetBrains Mono TTF files not present
- **MISSING:** All components under `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/`

### `:app` module
- `app/src/main/kotlin/com/devos/ai/MainActivity.kt` ✅ (calls installSplashScreen, uses DevOSTheme)
- `app/src/main/kotlin/com/devos/ai/DevOSApp.kt` ✅
- `app/src/main/kotlin/com/devos/ai/di/AppModule.kt` ✅ (skeleton)
- `app/src/main/kotlin/com/devos/ai/navigation/DevOSRoutes.kt` ✅ complete — all routes defined
- `app/src/main/kotlin/com/devos/ai/navigation/DevOSNavGraph.kt` ✅ skeleton — all registrations commented out
- `app/src/main/AndroidManifest.xml` ✅ has deep link filter for `devos://` scheme
- `app/src/main/res/values/strings.xml` ✅
- **MISSING:** splash theme XML (`res/values/themes.xml` for `Theme.DevOSAI.Splash`)
- **MISSING:** vector drawables: `ic_devos_logo`, `ic_github`, `ic_gitlab`
- **MISSING:** `mipmap/` launcher icons (referenced in manifest)

### `:core:core-security`
- `build.gradle.kts` ✅ already declares `androidx.security-crypto`
- **MISSING:** All source files — directories only, no `.kt` files

### `:feature:feature-auth`
- `build.gradle.kts` MISSING — no build file exists
- Source directories exist but are empty

### `:feature:feature-ai-chat`
- `build.gradle.kts` ✅ (good pattern to copy for other features)
- `AIChatUiState.kt`, `AIChatViewModel.kt` ✅

### All other feature modules
- Directories exist, build.gradle.kts files MISSING — need creating

---

## Gradle Dependency Gaps

### `libs.versions.toml` — already has all needed entries
- `androidx-security-crypto` ✅ defined as `androidx.security:security-crypto:1.1.0-alpha06`
- `androidx-splash` ✅ `androidx.core:core-splashscreen:1.0.1`
- `navigation-compose` ✅
- `commonmark` ✅ for markdown rendering
- `paging-runtime` / `paging-compose` ✅
- `androidx-datastore` ✅ for onboarding flag
- **NOTE:** `bundles.test.unit` is missing from `libs.versions.toml` — `core-security/build.gradle.kts` references `libs.bundles.test.unit` but only `test-unit` bundle exists. **Fix:** change `bundles.test.unit` → `bundles.test-unit` in `core-security/build.gradle.kts`, OR add `test-unit = [...]` alias.

### `feature-auth/build.gradle.kts` needs
- `project(":designsystem")`
- `project(":core:core-common")`
- `project(":core:core-security")`
- `project(":core:core-ui")` 
- `libs.androidx.datastore` (for onboarding_complete flag)
- `libs.navigation.compose`
- `libs.hilt.android`, `ksp(libs.hilt.compiler)`
- `libs.bundles.compose`
- `libs.androidx.lifecycle.compose`
- `libs.hilt.navigation.compose`
- `libs.androidx.browser` — **MISSING from libs.versions.toml** — needed for Chrome Custom Tab

### Add to `libs.versions.toml`
```toml
# In [versions]
browser = "1.8.0"

# In [libraries]
androidx-browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }
```

### `core-security/build.gradle.kts` needs
- `libs.hilt.android` + `ksp(libs.hilt.compiler)` — for Hilt injection
- `libs.androidx.lifecycle.vm` — for coroutine dispatchers
- Fix: `libs.bundles.test.unit` → `libs.bundles.test-unit`

---

## Potential Compilation Issues

1. **JetBrains Mono fonts:** `Typography.kt` references `R.font.jetbrains_mono_regular` etc. The `res/font/` directory exists but is empty. The build will FAIL until TTF files are placed there. **Resolution:** Use system monospace font as fallback with a code comment, OR download from JetBrains GitHub and place in `designsystem/src/main/res/font/`.

2. **Splash theme XML:** `AndroidManifest.xml` references `@style/Theme.DevOSAI.Splash`. There is no `res/values/themes.xml` in `:app`. Build will fail. **Resolution:** Create `app/src/main/res/values/themes.xml` with the splash screen theme.

3. **Launcher icons:** Manifest references `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`. Neither exist. **Resolution:** Add placeholder vector mipmap resources.

4. **NavGraph empty body:** `DevOSNavGraph.kt` has `NavHost {}` with all routes commented out and no `composable {}` entries. The `NavHost` requires at least one destination matching `startDestination`. Build will compile but crash at runtime. **Resolution:** Add placeholder screens for SPLASH, then progressively wire each feature.

5. **`core-ui` module:** Referenced in `app/build.gradle.kts` and `feature-ai-chat/build.gradle.kts` as `project(":core:core-ui")` but no `core/core-ui/` directory exists. Will fail Gradle sync. **Resolution:** Create stub `core/core-ui/build.gradle.kts` and `settings.gradle.kts` include.

6. **`domain-*` and `data-*` modules:** App's `build.gradle.kts` doesn't reference them directly, but features depend on them. Must exist as stubs for Gradle sync.

7. **HiltTestRunner:** `app/build.gradle.kts` references `com.devos.ai.HiltTestRunner` — this class doesn't exist yet. Only affects instrumented tests, not unit tests or assembleDebug.

---

## Implementation Order

### Stage 1 — DEVOS-005/006: Design System Components
**Blockers resolved:** Font TTF files + splash theme + missing resource stubs  
**Target:** `:designsystem:assembleDebug` passes

1. Add JetBrains Mono font TTF files to `designsystem/src/main/res/font/`
2. Create `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/` package
3. Implement P0 components: DevOSButton, DevOSCard, DevOSTopBar, DevOSBottomBar
4. Implement P0 state components: DevOSLoadingState, DevOSEmptyState, DevOSErrorState
5. Implement P1 components: DevOSSearchBar, DevOSCodeBlock, DevOSMarkdownText, DevOSStatusBadge, DevOSAIMessage, DevOSChatInput, DevOSChip, DevOSTabRow, DevOSSectionHeader, DevOSHealthIndicator

### Stage 2 — DEVOS-007/008: App Shell + Navigation
**Blockers resolved:** Stage 1 complete, stubs for missing core/feature modules  
**Target:** `app:assembleDebug` passes (no crash)

1. Create stub modules: `core-ui`, all feature modules without build files
2. Create splash theme XML in `app/src/main/res/values/themes.xml`
3. Create placeholder vector drawables (ic_devos_logo, ic_github, ic_gitlab, mipmap icons)
4. Update `DevOSNavGraph.kt` with placeholder `composable {}` blocks for all routes
5. Wire `DevOSBottomBar` into `MainActivity` Scaffold

### Stage 3 — DEVOS-009/010: Splash + Onboarding
**Module:** `feature/feature-auth/`

1. Create `feature-auth/build.gradle.kts`
2. Create domain models: `OAuthProvider`, `AuthState`
3. Implement `SplashViewModel` + `SplashScreen`
4. Implement `OnboardingViewModel` + `OnboardingScreen` (4-page HorizontalPager)
5. Wire `splashNavigation` and `onboardingNavigation` into `DevOSNavGraph`

### Stage 4 — DEVOS-011/012: Login + Secure Token Storage
**Modules:** `feature/feature-auth/`, `core/core-security/`

1. Fix `core-security/build.gradle.kts` (bundle name, add Hilt)
2. Implement `SecureTokenRepository` interface + `SecureTokenRepositoryImpl`
3. Implement `AuthRepository` interface
4. Implement `ExchangeCodeForTokenUseCase`, `CheckAuthStateUseCase`
5. Implement `AuthViewModel` + `LoginScreen`
6. Wire `authNavigation` into `DevOSNavGraph`
7. Update `AndroidManifest.xml` deep link host to `auth/callback`
8. Add `androidx-browser` dependency and Chrome Custom Tab helper

---

## File Creation Checklist

### New files needed

#### `designsystem` module
- `designsystem/src/main/res/font/jetbrains_mono_regular.ttf`
- `designsystem/src/main/res/font/jetbrains_mono_medium.ttf`
- `designsystem/src/main/res/font/jetbrains_mono_bold.ttf`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSButton.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSCard.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSTopBar.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSBottomBar.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSLoadingState.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSEmptyState.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSErrorState.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSSearchBar.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSCodeBlock.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSMarkdownText.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSStatusBadge.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSAIMessage.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSChatInput.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSChip.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSTabRow.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSSectionHeader.kt`
- `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSHealthIndicator.kt`

#### `:app` module resources
- `app/src/main/res/values/themes.xml` (Theme.DevOSAI + Theme.DevOSAI.Splash)
- `app/src/main/res/drawable/ic_devos_logo.xml` (vector)
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

#### Stub modules (build.gradle.kts only — empty source sets)
- `core/core-ui/build.gradle.kts`
- `feature/feature-home/build.gradle.kts`
- `feature/feature-project/build.gradle.kts`
- `feature/feature-repository/build.gradle.kts`
- `feature/feature-code/build.gradle.kts`
- `feature/feature-agents/build.gradle.kts`
- `feature/feature-git/build.gradle.kts`
- `feature/feature-issues/build.gradle.kts`
- `feature/feature-prs/build.gradle.kts`
- `feature/feature-security/build.gradle.kts`
- `feature/feature-testing/build.gradle.kts`
- `feature/feature-learning/build.gradle.kts`
- `feature/feature-memory/build.gradle.kts`
- `feature/feature-settings/build.gradle.kts`

#### `core-security` module
- `core/core-security/src/main/kotlin/com/devos/ai/core/security/SecureTokenRepository.kt`
- `core/core-security/src/main/kotlin/com/devos/ai/core/security/SecureTokenRepositoryImpl.kt`
- `core/core-security/src/main/kotlin/com/devos/ai/core/security/SecurityModule.kt`
- `core/core-security/src/test/kotlin/com/devos/ai/core/security/SecureTokenRepositoryTest.kt`

#### `feature-auth` module
- `feature/feature-auth/build.gradle.kts`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/model/OAuthProvider.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/model/AuthState.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/repository/AuthRepository.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/usecase/CheckAuthStateUseCase.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/usecase/ExchangeCodeForTokenUseCase.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/splash/SplashViewModel.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/splash/SplashScreen.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/onboarding/OnboardingViewModel.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/onboarding/OnboardingScreen.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/AuthViewModel.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/LoginScreen.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/navigation/AuthNavigation.kt`
- `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/di/AuthModule.kt`
- `feature/feature-auth/src/main/res/drawable/ic_github.xml`
- `feature/feature-auth/src/main/res/drawable/ic_gitlab.xml`
- `feature/feature-auth/src/test/kotlin/com/devos/ai/feature/auth/SplashViewModelTest.kt`
- `feature/feature-auth/src/test/kotlin/com/devos/ai/feature/auth/OnboardingViewModelTest.kt`
- `feature/feature-auth/src/test/kotlin/com/devos/ai/feature/auth/AuthViewModelTest.kt`
- `feature/feature-auth/src/test/kotlin/com/devos/ai/feature/auth/ExchangeCodeForTokenUseCaseTest.kt`

---

## Per-Module build.gradle.kts Changes

| Module | Change |
|--------|--------|
| `libs.versions.toml` | Add `browser = "1.8.0"` + `androidx-browser` library entry |
| `core/core-security/build.gradle.kts` | Fix `bundles.test.unit` → `bundles.test-unit`; add `libs.hilt.android`, `ksp(libs.hilt.compiler)`, `libs.kotlinx.coroutines`; add `alias(libs.plugins.hilt)` + `alias(libs.plugins.ksp)` plugins |
| `feature/feature-auth/build.gradle.kts` | New file: Hilt + Compose + designsystem + core-security + core-common + datastore + browser |
| All stub feature modules | New minimal `build.gradle.kts` with android-library plugin |

---

## Implementation Plan (Ordered Steps)

- [ ] 1. Fix `libs.versions.toml`: add `browser` version + `androidx-browser` library entry.
      Files: `gradle/libs.versions.toml`
      Verify: `./gradlew :app:dependencies` — no missing library errors.

- [ ] 2. Fix `core-security/build.gradle.kts`: rename bundle ref, add Hilt + Coroutines + KSP plugin.
      Files: `core/core-security/build.gradle.kts`
      Verify: `./gradlew :core:core-security:assembleDebug` passes.

- [ ] 3. Create stub `core/core-ui/build.gradle.kts` and `AndroidManifest.xml`.
      Needed because `app` and `feature-ai-chat` both depend on `:core:core-ui`.
      Files: `core/core-ui/build.gradle.kts`, `core/core-ui/src/main/AndroidManifest.xml`
      Verify: `./gradlew :core:core-ui:assembleDebug` passes.

- [ ] 4. Create stub `build.gradle.kts` for all 14 feature modules that are missing one.
      Pattern from `feature-ai-chat/build.gradle.kts`. Start without Hilt for stub modules.
      Files: all feature build.gradle.kts files listed above
      Verify: `./gradlew assembleDebug` — Gradle sync succeeds (even if screens empty).

- [ ] 5. Create `app/src/main/res/values/themes.xml` with splash screen theme.
      Required by AndroidManifest `android:theme="@style/Theme.DevOSAI.Splash"`.
      Files: `app/src/main/res/values/themes.xml`
      Verify: `./gradlew :app:assembleDebug` — no resource reference errors.

- [ ] 6. Create placeholder vector drawables: `ic_devos_logo.xml`, mipmap icons.
      Required by splash screen layout and launcher manifest refs.
      Files: `app/src/main/res/drawable/ic_devos_logo.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
      Verify: `./gradlew :app:assembleDebug` — no unresolved resource errors.

- [ ] 7. Download JetBrains Mono TTF files and place in `designsystem/src/main/res/font/`.
      Files: `jetbrains_mono_regular.ttf`, `jetbrains_mono_medium.ttf`, `jetbrains_mono_bold.ttf`
      Verify: `./gradlew :designsystem:assembleDebug` — font resources compile.

- [ ] 8. Implement DEVOS-005 P0 components: DevOSButton, DevOSCard.
      Button needs: Primary/Secondary/Tertiary/Destructive/Ghost variants via `DevOSButtonStyle` enum.
      Card needs: clickable + static variants, uses `MaterialTheme.shapes.medium`.
      Files: `designsystem/src/main/kotlin/com/devos/ai/designsystem/components/DevOSButton.kt`, `DevOSCard.kt`
      Verify: `./gradlew :designsystem:assembleDebug` passes.

- [ ] 9. Implement DEVOS-005 P0 components: DevOSTopBar, DevOSBottomBar.
      BottomBar: 5 tabs (Home, Projects, AI, Learn, More) using `NavigationBar` + `NavigationBarItem`.
      TopBar: wraps `CenterAlignedTopAppBar` with title/subtitle/leading/trailing action slots.
      Files: `DevOSTopBar.kt`, `DevOSBottomBar.kt`
      Verify: `./gradlew :designsystem:assembleDebug` passes.

- [ ] 10. Implement DEVOS-005 state components: DevOSLoadingState, DevOSEmptyState, DevOSErrorState.
       LoadingState: `CircularProgressIndicator` centered; shimmer variant for list skeletons.
       EmptyState: icon + title + description + optional `DevOSButton` action.
       ErrorState: error icon + description + optional retry `DevOSButton`.
       Files: `DevOSLoadingState.kt`, `DevOSEmptyState.kt`, `DevOSErrorState.kt`
       Verify: `./gradlew :designsystem:assembleDebug` passes.

- [ ] 11. Implement DEVOS-006 P1 components: DevOSSearchBar, DevOSStatusBadge, DevOSChip, DevOSTabRow, DevOSSectionHeader, DevOSHealthIndicator.
       SearchBar: `SearchBarShape` (28dp pill), leading search icon, trailing clear icon.
       StatusBadge: colored pill, states: Success/Warning/Error/Info/Running/Pending.
       Files: `DevOSSearchBar.kt`, `DevOSStatusBadge.kt`, `DevOSChip.kt`, `DevOSTabRow.kt`, `DevOSSectionHeader.kt`, `DevOSHealthIndicator.kt`
       Verify: `./gradlew :designsystem:assembleDebug` passes.

- [ ] 12. Implement DEVOS-006 code/AI components: DevOSCodeBlock, DevOSMarkdownText, DevOSAIMessage, DevOSChatInput.
       CodeBlock: JetBrainsMonoFamily, `SyntaxColors.background` always, line numbers, copy button, Kotlin/Java/JSON/XML/Bash syntax tokens.
       MarkdownText: use `commonmark` library for parse, render headings/bold/italic/code/lists.
       AIMessage: wraps `DevOSMarkdownText` + role label + copy action.
       ChatInput: multi-line TextField with send button, attach button.
       Files: `DevOSCodeBlock.kt`, `DevOSMarkdownText.kt`, `DevOSAIMessage.kt`, `DevOSChatInput.kt`
       Verify: `./gradlew :designsystem:assembleDebug` passes.

- [ ] 13. Implement DEVOS-007: Wire `DevOSBottomBar` into `MainActivity` Scaffold.
       `MainActivity` replaces raw `Surface` with a `Scaffold(bottomBar = { DevOSBottomBar(...) })`.
       Bottom nav tracks current route via `navController.currentBackStackEntryAsState()`.
       Files: `app/src/main/kotlin/com/devos/ai/MainActivity.kt`
       Verify: `./gradlew :app:assembleDebug` passes.

- [ ] 14. Implement DEVOS-008: Wire placeholder `composable {}` entries into `DevOSNavGraph`.
       Add `composable(SPLASH) { PlaceholderScreen("Splash") }` for each route in DevOSRoutes.
       Each placeholder is a `Box(Modifier.fillMaxSize()) { Text(route) }` — enough for NavHost to not crash.
       Files: `app/src/main/kotlin/com/devos/ai/navigation/DevOSNavGraph.kt`
       Verify: `./gradlew :app:assembleDebug` passes, app launches without crash.

- [ ] 15. Create `feature-auth/build.gradle.kts` with all required dependencies.
       Uses Hilt + Compose + designsystem + core-security + core-common + datastore + browser.
       Files: `feature/feature-auth/build.gradle.kts`
       Verify: `./gradlew :feature:feature-auth:assembleDebug` passes.

- [ ] 16. Implement DEVOS-012: `SecureTokenRepository` interface + `SecureTokenRepositoryImpl` in `:core:core-security`.
       Interface in `core/core-security/src/main/kotlin/com/devos/ai/core/security/SecureTokenRepository.kt`.
       Impl uses `EncryptedSharedPreferences` with AES256_GCM. Never logs token value — only `take(4)+"****"`.
       Hilt module `SecurityModule` binds impl to interface.
       Files: `SecureTokenRepository.kt`, `SecureTokenRepositoryImpl.kt`, `SecurityModule.kt`
       Verify: `./gradlew :core:core-security:assembleDebug` passes.

- [ ] 17. Implement DEVOS-012 test: `SecureTokenRepositoryTest`.
       Tests: saveToken stores value; getToken returns it; clearToken removes it; never writes to plain prefs.
       Files: `core/core-security/src/test/kotlin/.../SecureTokenRepositoryTest.kt`
       Verify: `./gradlew :core:core-security:testDebugUnitTest` passes.

- [ ] 18. Implement DEVOS-011 domain models: `OAuthProvider`, `AuthState`, `AuthRepository` interface.
       `OAuthProvider` enum: GITHUB, GITLAB — each with `prefKey` and `authUrl` properties.
       `AuthRepository` interface: `suspend fun exchangeCodeForToken(code: String, provider: OAuthProvider): Result<String>`.
       Files: `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/model/OAuthProvider.kt`, `AuthState.kt`, `repository/AuthRepository.kt`
       Verify: `./gradlew :feature:feature-auth:assembleDebug` passes.

- [ ] 19. Implement UseCases: `CheckAuthStateUseCase`, `ExchangeCodeForTokenUseCase`.
       `CheckAuthStateUseCase`: checks DataStore `onboarding_complete` flag + `SecureTokenRepository.hasToken()`.
       `ExchangeCodeForTokenUseCase`: calls `AuthRepository.exchangeCodeForToken()`, saves token via `SecureTokenRepository`.
       Files: `usecase/CheckAuthStateUseCase.kt`, `usecase/ExchangeCodeForTokenUseCase.kt`
       Verify: `./gradlew :feature:feature-auth:assembleDebug` passes.

- [ ] 20. Implement DEVOS-009: `SplashViewModel` + `SplashScreen`.
       ViewModel emits `NavigateToOnboarding` or `NavigateToHome` via `SharedFlow<SplashNavEvent>`.
       Screen: animated logo (scale+fade, 600ms), tagline text, 1500ms minimum display.
       Files: `splash/SplashViewModel.kt`, `splash/SplashScreen.kt`
       Verify: `./gradlew :feature:feature-auth:testDebugUnitTest` — SplashViewModelTest passes.

- [ ] 21. Implement DEVOS-010: `OnboardingViewModel` + `OnboardingScreen`.
       HorizontalPager with 4 pages (FolderOpen/Code/SmartToy/School icons + titles + descriptions).
       Skip/Next/Get Started buttons. DataStore flag `onboarding_complete` set on completion or skip.
       Files: `onboarding/OnboardingViewModel.kt`, `onboarding/OnboardingScreen.kt`
       Verify: `./gradlew :feature:feature-auth:testDebugUnitTest` — OnboardingViewModelTest passes.

- [ ] 22. Implement DEVOS-011: `AuthViewModel` + `LoginScreen`.
       AuthViewModel: `loginWithGitHub()` / `loginWithGitLab()` open Chrome Custom Tab. `handleAuthCallback()` calls `ExchangeCodeForTokenUseCase`. Navigation via `SharedFlow<AuthNavEvent>`.
       LoginScreen: logo + tagline + two `DevOSButton`s (GitHub primary, GitLab secondary) + `DevOSErrorState` on error.
       Files: `login/AuthViewModel.kt`, `login/LoginScreen.kt`
       Verify: `./gradlew :feature:feature-auth:testDebugUnitTest` — AuthViewModelTest passes.

- [ ] 23. Implement `AuthNavigation.kt` — NavGraphBuilder extension functions for splash/onboarding/login.
       Replace placeholder entries in `DevOSNavGraph.kt` with real `splashNavigation()`, `onboardingNavigation()`, `authNavigation()` calls.
       Files: `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/navigation/AuthNavigation.kt`, updated `app/src/main/kotlin/com/devos/ai/navigation/DevOSNavGraph.kt`
       Verify: `./gradlew :app:assembleDebug` passes.

- [ ] 24. Update `AndroidManifest.xml` deep link to include host `auth` + path `/callback`.
       Current manifest only has `android:scheme="devos"` — needs `android:host="auth"` + `android:path="/callback"` for the OAuth callback.
       Files: `app/src/main/AndroidManifest.xml`
       Verify: `./gradlew :app:assembleDebug` passes.

- [ ] 25. Write unit tests for UseCases and ViewModels per the kiro-prompt specs.
       Tests: SplashViewModelTest (3 cases), OnboardingViewModelTest (4 cases), AuthViewModelTest (3 cases), ExchangeCodeForTokenUseCaseTest (2 cases).
       Files: `src/test/kotlin/.../SplashViewModelTest.kt`, `OnboardingViewModelTest.kt`, `AuthViewModelTest.kt`, `ExchangeCodeForTokenUseCaseTest.kt`
       Verify: `./gradlew :feature:feature-auth:testDebugUnitTest` — all tests pass.

- [ ] 26. Update `.kiro/implementation-status.md` — mark DEVOS-005 through DEVOS-012 as 🟢 Complete.
       Files: `.kiro/implementation-status.md`
       Verify: File updated; completion log entries added with date.

- [ ] 27. Final build verification: `./gradlew assembleDebug testDebugUnitTest`.
       All modules must compile. All unit tests must pass.
       Verify: Exit code 0 for both commands.
