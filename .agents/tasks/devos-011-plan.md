# Implementation Plan — DEVOS-011: Login Screen with GitHub/GitLab OAuth

**Ticket:** DEVOS-011  
**Module:** `feature-auth`  
**Status:** Ready for implementation

---

## Context

The `feature-auth` module already has:
- `SplashScreen` / `SplashViewModel` — pattern to follow
- `OnboardingScreen` / `OnboardingViewModel` — pattern to follow
- `AuthNavigation.kt` — has `ROUTE_LOGIN = "login"` constant and nav helpers
- `OAuthProvider.kt` — enum, but does NOT implement `TokenKey` yet; uses `displayName` not `name`
- `CheckAuthStateUseCase.kt` — stub that needs real token check
- `AuthModule.kt` — only provides DataStore; needs `@Binds` for `SecureTokenRepository`
- `DevOSNavGraph.kt` — has `PlaceholderScreen` for LOGIN route; must be replaced
- `AndroidManifest.xml` — has `devos://` deep link scheme but no host/path
- `feature-auth/build.gradle.kts` — already has `androidx.browser`, hilt, navigation.compose, lifecycle.compose; no new deps needed

---

## Implementation Steps

- [ ] 1. **Modify `OAuthProvider.kt`** — implement `TokenKey` interface, rename `displayName` → `name`

  `OAuthProvider` must implement `TokenKey` (from `core-security`) so it can be passed directly to `SecureTokenRepository.saveToken()` / `hasToken()`. The field rename from `displayName` to `name` is required because `TokenKey` declares `val name: String`. Search the entire `feature-auth` module for `.displayName` usages and update them to `.name`.

  **File:** `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/model/OAuthProvider.kt`

  ```kotlin
  package com.devos.ai.feature.auth.model

  import com.devos.ai.core.security.TokenKey

  enum class OAuthProvider(
      override val prefKey: String,
      override val name: String,
  ) : TokenKey {
      GITHUB(prefKey = "github_token", name = "GitHub"),
      GITLAB(prefKey = "gitlab_token", name = "GitLab"),
  }
  ```

  **Verify:** `./gradlew :feature:feature-auth:compileDebugKotlin` — no unresolved reference errors.

---

- [ ] 2. **Fix `CheckAuthStateUseCase.kt`** — inject `SecureTokenRepository`, implement real token check

  The existing stub does not actually check for a saved token. Inject `SecureTokenRepository` and use `hasToken(OAuthProvider.GITHUB) || hasToken(OAuthProvider.GITLAB)` for the `isAuthenticated` result. Also introduce `AuthCheckResult` as a data class in the same file (the `SplashViewModel` will consume it).

  **File:** `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/usecase/CheckAuthStateUseCase.kt`

  ```kotlin
  package com.devos.ai.feature.auth.usecase

  import androidx.datastore.core.DataStore
  import androidx.datastore.preferences.core.Preferences
  import androidx.datastore.preferences.core.booleanPreferencesKey
  import com.devos.ai.core.security.SecureTokenRepository
  import com.devos.ai.feature.auth.model.OAuthProvider
  import kotlinx.coroutines.flow.first
  import javax.inject.Inject

  data class AuthCheckResult(
      val onboardingComplete: Boolean,
      val isAuthenticated: Boolean,
  )

  class CheckAuthStateUseCase @Inject constructor(
      private val dataStore: DataStore<Preferences>,
      private val tokenRepository: SecureTokenRepository,
  ) {
      companion object {
          val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
      }

      suspend operator fun invoke(): AuthCheckResult = runCatching {
          val prefs = dataStore.data.first()
          val onboardingComplete = prefs[KEY_ONBOARDING_COMPLETE] ?: false
          val isAuthenticated = tokenRepository.hasToken(OAuthProvider.GITHUB)
                             || tokenRepository.hasToken(OAuthProvider.GITLAB)
          AuthCheckResult(onboardingComplete = onboardingComplete, isAuthenticated = isAuthenticated)
      }.getOrDefault(AuthCheckResult(onboardingComplete = false, isAuthenticated = false))
  }
  ```

  **Verify:** `./gradlew :feature:feature-auth:compileDebugKotlin` — compiles cleanly.

---

- [ ] 3. **Fix `AuthModule.kt`** — add `@Binds` for `SecureTokenRepository`, convert to abstract class with companion object

  Hilt requires `@Binds` methods to live in an abstract class. The existing `@Provides` for DataStore must move to a `companion object` inside the abstract class. This is the standard Hilt pattern for mixing `@Binds` + `@Provides`.

  **File:** `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/di/AuthModule.kt`

  ```kotlin
  package com.devos.ai.feature.auth.di

  import android.content.Context
  import androidx.datastore.core.DataStore
  import androidx.datastore.preferences.core.Preferences
  import androidx.datastore.preferences.preferencesDataStore
  import com.devos.ai.core.security.SecureTokenRepository
  import com.devos.ai.core.security.SecureTokenRepositoryImpl
  import dagger.Binds
  import dagger.Module
  import dagger.Provides
  import dagger.hilt.InstallIn
  import dagger.hilt.android.qualifiers.ApplicationContext
  import dagger.hilt.components.SingletonComponent
  import javax.inject.Singleton

  private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "devos_prefs")

  @Module
  @InstallIn(SingletonComponent::class)
  abstract class AuthModule {

      @Binds
      @Singleton
      abstract fun bindSecureTokenRepository(
          impl: SecureTokenRepositoryImpl,
      ): SecureTokenRepository

      companion object {
          @Provides
          @Singleton
          fun provideDataStore(
              @ApplicationContext context: Context,
          ): DataStore<Preferences> = context.authDataStore
      }
  }
  ```

  **Verify:** `./gradlew :feature:feature-auth:compileDebugKotlin` — no Hilt or Dagger errors.

---

- [ ] 4. **Create `LoginUiState.kt`**

  Simple sealed interface for login screen state. No `Success` variant because successful auth immediately navigates away via `navEvent`.

  **File:** `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/LoginUiState.kt`

  ```kotlin
  package com.devos.ai.feature.auth.login

  sealed interface LoginUiState {
      data object Idle : LoginUiState
      data object Loading : LoginUiState
      data class Error(val message: String) : LoginUiState
  }
  ```

  **Verify:** File compiles as part of the next `compileDebugKotlin` run.

---

- [ ] 5. **Create `LoginNavEvent.kt`**

  One-shot navigation events emitted via `SharedFlow`. Only `ToHome` for now.

  **File:** `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/LoginNavEvent.kt`

  ```kotlin
  package com.devos.ai.feature.auth.login

  sealed class LoginNavEvent {
      data object ToHome : LoginNavEvent()
  }
  ```

  **Verify:** File compiles as part of the next `compileDebugKotlin` run.

---

- [ ] 6. **Create `AuthViewModel.kt`**

  `@HiltViewModel` that owns:
  - `_uiState: MutableStateFlow<LoginUiState>` — exposed as `StateFlow`
  - `_navEvent: MutableSharedFlow<LoginNavEvent>` — exposed as `SharedFlow`
  - `loginWithGitHub()` / `loginWithGitLab()` — set Loading then launch Chrome Custom Tab
  - `handleAuthCallback(code, provider)` — called from deep link; exchanges code → token via OkHttp on `Dispatchers.IO`; saves token via `SecureTokenRepository`; emits `ToHome` on success
  - `exchangeCodeForToken()` — private suspend fun, OkHttp POST to GitHub/GitLab token endpoint, returns `String?`

  **Security note:** Never log full token — only `token.take(4) + "****"`.  
  **Note:** `DEVOS_GITHUB_CLIENT_ID` and `DEVOS_GITLAB_CLIENT_ID` are placeholder strings in URL constants; they will be replaced by real client IDs loaded from `EncryptedSharedPreferences` in a later ticket (DEVOS-012). Do NOT put real client IDs in source.

  **File:** `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/AuthViewModel.kt`

  ```kotlin
  package com.devos.ai.feature.auth.login

  import android.content.Context
  import androidx.browser.customtabs.CustomTabsIntent
  import androidx.core.net.toUri
  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.devos.ai.core.security.SecureTokenRepository
  import com.devos.ai.feature.auth.model.OAuthProvider
  import dagger.hilt.android.lifecycle.HiltViewModel
  import dagger.hilt.android.qualifiers.ApplicationContext
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.flow.MutableSharedFlow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.SharedFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asSharedFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.withContext
  import okhttp3.FormBody
  import okhttp3.OkHttpClient
  import okhttp3.Request
  import org.json.JSONObject
  import timber.log.Timber
  import javax.inject.Inject

  private const val GITHUB_AUTH_URL =
      "https://github.com/login/oauth/authorize?client_id=DEVOS_GITHUB_CLIENT_ID&scope=repo,read:user"
  private const val GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token"
  private const val GITLAB_AUTH_URL =
      "https://gitlab.com/oauth/authorize?client_id=DEVOS_GITLAB_CLIENT_ID&response_type=code&scope=api+read_user"
  private const val GITLAB_TOKEN_URL = "https://gitlab.com/oauth/token"

  @HiltViewModel
  class AuthViewModel @Inject constructor(
      private val secureTokenRepository: SecureTokenRepository,
      @ApplicationContext private val context: Context,
  ) : ViewModel() {

      private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
      val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

      private val _navEvent = MutableSharedFlow<LoginNavEvent>()
      val navEvent: SharedFlow<LoginNavEvent> = _navEvent.asSharedFlow()

      private val httpClient = OkHttpClient()

      fun loginWithGitHub() {
          _uiState.value = LoginUiState.Loading
          val intent = CustomTabsIntent.Builder().build()
          intent.launchUrl(context, GITHUB_AUTH_URL.toUri())
      }

      fun loginWithGitLab() {
          _uiState.value = LoginUiState.Loading
          val intent = CustomTabsIntent.Builder().build()
          intent.launchUrl(context, GITLAB_AUTH_URL.toUri())
      }

      fun handleAuthCallback(code: String, provider: OAuthProvider) {
          viewModelScope.launch {
              _uiState.value = LoginUiState.Loading
              try {
                  val token = exchangeCodeForToken(code, provider)
                  if (token != null) {
                      secureTokenRepository.saveToken(provider, token)
                      Timber.d("Token saved for ${provider.name}: ${token.take(4)}****")
                      _navEvent.emit(LoginNavEvent.ToHome)
                  } else {
                      _uiState.value = LoginUiState.Error("Authentication failed. Please try again.")
                  }
              } catch (e: Exception) {
                  Timber.e(e, "OAuth callback handling failed for ${provider.name}")
                  _uiState.value = LoginUiState.Error(e.message ?: "Authentication failed")
              }
          }
      }

      private suspend fun exchangeCodeForToken(
          code: String,
          provider: OAuthProvider,
      ): String? = withContext(Dispatchers.IO) {
          val tokenUrl = when (provider) {
              OAuthProvider.GITHUB -> GITHUB_TOKEN_URL
              OAuthProvider.GITLAB -> GITLAB_TOKEN_URL
          }
          val body = FormBody.Builder()
              .add("code", code)
              .add("grant_type", "authorization_code")
              .build()
          val request = Request.Builder()
              .url(tokenUrl)
              .post(body)
              .addHeader("Accept", "application/json")
              .build()
          val response = httpClient.newCall(request).execute()
          if (!response.isSuccessful) return@withContext null
          val responseBody = response.body?.string() ?: return@withContext null
          JSONObject(responseBody).optString("access_token").takeIf { it.isNotEmpty() }
      }
  }
  ```

  **Verify:** `./gradlew :feature:feature-auth:compileDebugKotlin` — Hilt component generates without error.

---

- [ ] 7. **Create `LoginScreen.kt`**

  Stateless composable. Receives `uiState` and `navEvent` from the nav entry point.
  - `LaunchedEffect(Unit)` collects `navEvent` and calls `onNavigateToHome()`
  - Shows `DevOSTopBar(title = "Sign In")`
  - Center-column layout with title, subtitle, two `DevOSButton`s (GitHub primary, GitLab secondary)
  - `CircularProgressIndicator` when `Loading`
  - `DevOSErrorState` when `Error`
  - All spacing via `MaterialTheme.spacing.*` tokens; no hardcoded dp values except icon size (20.dp — not a spacing token)
  - Both buttons disabled when `Loading`
  - Icons reference `R.drawable.ic_github` and `R.drawable.ic_gitlab` (created in step 8)

  **File:** `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/LoginScreen.kt`

  ```kotlin
  package com.devos.ai.feature.auth.login

  import androidx.compose.foundation.layout.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.*
  import androidx.compose.ui.res.painterResource
  import androidx.compose.ui.text.style.TextAlign
  import androidx.compose.ui.unit.dp
  import com.devos.ai.designsystem.components.DevOSButton
  import com.devos.ai.designsystem.components.DevOSButtonStyle
  import com.devos.ai.designsystem.components.DevOSErrorState
  import com.devos.ai.designsystem.components.DevOSTopBar
  import com.devos.ai.designsystem.theme.DevOSSpacing
  import com.devos.ai.feature.auth.R
  import kotlinx.coroutines.flow.SharedFlow

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun LoginScreen(
      uiState: LoginUiState,
      navEvent: SharedFlow<LoginNavEvent>,
      onLoginGitHub: () -> Unit,
      onLoginGitLab: () -> Unit,
      onNavigateToHome: () -> Unit,
      modifier: Modifier = Modifier,
  ) {
      LaunchedEffect(Unit) {
          navEvent.collect { event ->
              when (event) {
                  is LoginNavEvent.ToHome -> onNavigateToHome()
              }
          }
      }

      Scaffold(
          modifier = modifier,
          topBar = { DevOSTopBar(title = "Sign In") },
      ) { padding ->
          Column(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(padding)
                  .padding(horizontal = MaterialTheme.spacing.base),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
          ) {
              Text(
                  text = "Welcome to DevOS AI",
                  style = MaterialTheme.typography.titleLarge,
              )
              Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
              Text(
                  text = "Connect your repository host to get started.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
              DevOSButton(
                  text = "Continue with GitHub",
                  onClick = onLoginGitHub,
                  leadingIcon = {
                      Icon(
                          painter = painterResource(R.drawable.ic_github),
                          contentDescription = null,
                          modifier = Modifier.size(20.dp),
                      )
                  },
                  modifier = Modifier.fillMaxWidth(),
                  enabled = uiState !is LoginUiState.Loading,
              )
              Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
              DevOSButton(
                  text = "Continue with GitLab",
                  onClick = onLoginGitLab,
                  style = DevOSButtonStyle.Secondary,
                  leadingIcon = {
                      Icon(
                          painter = painterResource(R.drawable.ic_gitlab),
                          contentDescription = null,
                          modifier = Modifier.size(20.dp),
                      )
                  },
                  modifier = Modifier.fillMaxWidth(),
                  enabled = uiState !is LoginUiState.Loading,
              )
              if (uiState is LoginUiState.Loading) {
                  Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                  CircularProgressIndicator()
              }
              if (uiState is LoginUiState.Error) {
                  Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                  DevOSErrorState(
                      description = uiState.message,
                      onRetry = null,
                  )
              }
          }
      }
  }
  ```

  **Note on spacing:** `MaterialTheme.spacing` is an extension from the design system. If the design system uses `DevOSSpacing` as a standalone object, use that instead. Check `designsystem/theme/` for the actual accessor before finalising — use whatever pattern the existing screens use.

  **Verify:** `./gradlew :feature:feature-auth:compileDebugKotlin` — composable resolves all design system references.

---

- [ ] 8. **Create drawable resources** — `ic_github.xml` and `ic_gitlab.xml`

  Both are Vector Drawables. Place in the `feature-auth` res directory (not app-level), since these icons are auth-feature-specific.

  **Files:**
  - `feature/feature-auth/src/main/res/drawable/ic_github.xml`
  - `feature/feature-auth/src/main/res/drawable/ic_gitlab.xml`

  **ic_github.xml:**
  ```xml
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      android:width="24dp"
      android:height="24dp"
      android:viewportWidth="24"
      android:viewportHeight="24">
      <path
          android:fillColor="@android:color/white"
          android:pathData="M12,2C6.477,2 2,6.477 2,12c0,4.418 2.865,8.166 6.839,9.489 0.5,0.092 0.682,-0.217 0.682,-0.482 0,-0.237 -0.008,-0.866 -0.013,-1.7 -2.782,0.604 -3.369,-1.34 -3.369,-1.34 -0.454,-1.155 -1.11,-1.462 -1.11,-1.462 -0.908,-0.62 0.069,-0.608 0.069,-0.608 1.003,0.07 1.531,1.03 1.531,1.03 0.892,1.529 2.341,1.087 2.91,0.831 0.092,-0.646 0.349,-1.086 0.635,-1.337 -2.22,-0.253 -4.555,-1.11 -4.555,-4.943 0,-1.091 0.39,-1.984 1.029,-2.683 -0.103,-0.253 -0.446,-1.27 0.098,-2.647 0,0 0.84,-0.269 2.75,1.025A9.578,9.578 0,0 1,12 6.836a9.59,9.59 0,0 1,2.504 0.337c1.909,-1.294 2.747,-1.025 2.747,-1.025 0.546,1.377 0.202,2.394 0.1,2.647 0.64,0.699 1.026,1.592 1.026,2.683 0,3.841 -2.337,4.687 -4.565,4.935 0.359,0.309 0.679,0.919 0.679,1.852 0,1.336 -0.012,2.415 -0.012,2.743 0,0.267 0.18,0.578 0.688,0.48C19.138,20.163 22,16.418 22,12c0,-5.523 -4.477,-10 -10,-10Z" />
  </vector>
  ```

  **ic_gitlab.xml:**
  ```xml
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      android:width="24dp"
      android:height="24dp"
      android:viewportWidth="24"
      android:viewportHeight="24">
      <path
          android:fillColor="#FC6D26"
          android:pathData="M23.955,13.587l-1.342,-4.135 -2.664,-8.189a0.455,0.455 0,0 0,-0.867 0L16.418,9.45H7.582L4.918,1.263a0.455,0.455 0,0 0,-0.867 0L1.386,9.452 0.044,13.587a0.924,0.924 0,0 0,0.331 1.023L12,23l11.625,-8.39a0.924,0.924 0,0 0,0.33,-1.023Z" />
  </vector>
  ```

  **Verify:** `./gradlew :feature:feature-auth:mergeDebugResources` — no resource merge errors.

---

- [ ] 9. **Add `loginNavigation()` to `AuthNavigation.kt`**

  Append the new `NavGraphBuilder` extension function after the existing `onboardingNavigation`. It must:
  - Register deep link `devos://auth/callback?code={code}&provider={provider}`
  - Use `hiltViewModel<AuthViewModel>()`
  - Extract `code` + `provider` from `backStackEntry.arguments` via `LaunchedEffect`
  - Wire `LoginScreen` callbacks to ViewModel

  Add these imports to the file:
  ```
  androidx.compose.runtime.LaunchedEffect
  androidx.lifecycle.compose.collectAsStateWithLifecycle
  androidx.navigation.navDeepLink
  com.devos.ai.feature.auth.login.AuthViewModel
  com.devos.ai.feature.auth.login.LoginScreen
  com.devos.ai.feature.auth.model.OAuthProvider
  ```

  **File:** `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/navigation/AuthNavigation.kt`

  ```kotlin
  fun NavGraphBuilder.loginNavigation(navController: NavController) {
      composable(
          route = ROUTE_LOGIN,
          deepLinks = listOf(
              navDeepLink { uriPattern = "devos://auth/callback?code={code}&provider={provider}" }
          ),
      ) { backStackEntry ->
          val viewModel: AuthViewModel = hiltViewModel()
          val uiState by viewModel.uiState.collectAsStateWithLifecycle()

          val code = backStackEntry.arguments?.getString("code")
          val providerName = backStackEntry.arguments?.getString("provider")
          LaunchedEffect(code, providerName) {
              if (!code.isNullOrEmpty() && !providerName.isNullOrEmpty()) {
                  runCatching { OAuthProvider.valueOf(providerName.uppercase()) }
                      .getOrNull()
                      ?.let { provider -> viewModel.handleAuthCallback(code, provider) }
              }
          }

          LoginScreen(
              uiState = uiState,
              navEvent = viewModel.navEvent,
              onLoginGitHub = viewModel::loginWithGitHub,
              onLoginGitLab = viewModel::loginWithGitLab,
              onNavigateToHome = {
                  navController.navigate("home") {
                      popUpTo(ROUTE_LOGIN) { inclusive = true }
                  }
              },
          )
      }
  }
  ```

  **Verify:** `./gradlew :feature:feature-auth:compileDebugKotlin` — no unresolved references.

---

- [ ] 10. **Update `DevOSNavGraph.kt`** — replace `PlaceholderScreen` for LOGIN route

  Find and replace:
  ```kotlin
  // TODO(FEAT-004): Replace with authNavigation(navController)
  composable(route = DevOSRoutes.LOGIN) {
      PlaceholderScreen(route = DevOSRoutes.LOGIN)
  }
  ```
  With:
  ```kotlin
  loginNavigation(navController)
  ```

  Add import: `import com.devos.ai.feature.auth.navigation.loginNavigation`

  **File:** `app/src/main/kotlin/com/devos/ai/navigation/DevOSNavGraph.kt`

  **Verify:** `./gradlew :app:compileDebugKotlin` — no unresolved reference for `loginNavigation`.

---

- [ ] 11. **Update `AndroidManifest.xml`** — add host + path to OAuth callback deep link

  The existing intent-filter has only `<data android:scheme="devos" />`. Replace the entire intent-filter with one that specifies host and path, so the deep link `devos://auth/callback` is handled by `MainActivity`:

  ```xml
  <intent-filter android:autoVerify="true">
      <action android:name="android.intent.action.VIEW" />
      <category android:name="android.intent.category.DEFAULT" />
      <category android:name="android.intent.category.BROWSABLE" />
      <data android:scheme="devos" android:host="auth" android:path="/callback" />
  </intent-filter>
  ```

  **File:** `app/src/main/AndroidManifest.xml`

  **Verify:** `./gradlew :app:processDebugManifest` — manifest merges without errors.

---

- [ ] 12. **Update `MainActivity.kt`** — add `onNewIntent` override for deep link routing

  When the browser redirects to `devos://auth/callback?code=XXX&provider=github`, Android delivers the intent via `onNewIntent`. Calling `setIntent(intent)` ensures the `NavHost` deep link handler picks it up.

  **File:** `app/src/main/kotlin/com/devos/ai/MainActivity.kt`

  ```kotlin
  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      // NavHost handles devos://auth/callback?code=XXX&provider=github automatically
      // after setIntent updates the activity's current intent.
  }
  ```

  Add import: `android.content.Intent` (likely already present).

  **Verify:** `./gradlew :app:assembleDebug` — full app build succeeds.

---

- [ ] 13. **Write unit tests**

  **File 1:** `feature/feature-auth/src/test/kotlin/com/devos/ai/feature/auth/AuthViewModelTest.kt`

  Tests to write (using MockK + Turbine + `UnconfinedTestDispatcher`):
  - `loginWithGitHub sets uiState to Loading` — call `loginWithGitHub()`, assert `uiState.value is LoginUiState.Loading`
  - `loginWithGitLab sets uiState to Loading` — same pattern for GitLab
  - `handleAuthCallback success emits ToHome nav event` — mock `secureTokenRepository.saveToken()` to succeed; call `handleAuthCallback("code123", OAuthProvider.GITHUB)`; use `turbine` on `navEvent` to assert `LoginNavEvent.ToHome` emitted
  - `handleAuthCallback failure sets Error state` — mock token exchange to throw; verify `uiState.value is LoginUiState.Error`
  - `handleAuthCallback null token sets Error state` — mock HTTP response to return empty body; verify `LoginUiState.Error`

  **File 2:** `feature/feature-auth/src/test/kotlin/com/devos/ai/feature/auth/CheckAuthStateUseCaseTest.kt`

  Tests to write:
  - `returns isAuthenticated true when GitHub token exists` — mock `tokenRepository.hasToken(OAuthProvider.GITHUB)` returns `true`
  - `returns isAuthenticated false when no tokens` — both `hasToken` return `false`
  - `returns onboardingComplete true when pref is set` — DataStore emits `true` for `KEY_ONBOARDING_COMPLETE`
  - `returns safe default on exception` — DataStore throws; verify `AuthCheckResult(false, false)` returned

  **Verify:** `./gradlew :feature:feature-auth:testDebugUnitTest` — all tests pass.

---

- [ ] 14. **Run full build and test verification**

  ```bash
  # 1. Module-level tests
  ./gradlew :feature:feature-auth:testDebugUnitTest

  # 2. Module-level assemble (fast compilation check)
  ./gradlew :feature:feature-auth:assembleDebug

  # 3. Full app build
  ./gradlew assembleDebug
  ```

  All three commands must exit with `BUILD SUCCESSFUL`.

---

- [ ] 15. **Update `implementation-status.md`**

  Mark DEVOS-011 as complete in `.kiro/implementation-status.md`. Set:
  - Status: `✅ Done`
  - Notes: "Login screen with GitHub/GitLab OAuth implemented. OAuthProvider now implements TokenKey. AuthModule converted to abstract class with @Binds. Deep link handler wired in MainActivity."

  **File:** `.kiro/implementation-status.md`

---

## Files Created / Modified Summary

| Action | File |
|--------|------|
| Modify | `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/model/OAuthProvider.kt` |
| Modify | `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/usecase/CheckAuthStateUseCase.kt` |
| Modify | `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/di/AuthModule.kt` |
| Create | `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/LoginUiState.kt` |
| Create | `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/LoginNavEvent.kt` |
| Create | `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/AuthViewModel.kt` |
| Create | `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/login/LoginScreen.kt` |
| Create | `feature/feature-auth/src/main/res/drawable/ic_github.xml` |
| Create | `feature/feature-auth/src/main/res/drawable/ic_gitlab.xml` |
| Modify | `feature/feature-auth/src/main/kotlin/com/devos/ai/feature/auth/navigation/AuthNavigation.kt` |
| Modify | `app/src/main/kotlin/com/devos/ai/navigation/DevOSNavGraph.kt` |
| Modify | `app/src/main/AndroidManifest.xml` |
| Modify | `app/src/main/kotlin/com/devos/ai/MainActivity.kt` |
| Create | `feature/feature-auth/src/test/kotlin/com/devos/ai/feature/auth/AuthViewModelTest.kt` |
| Create | `feature/feature-auth/src/test/kotlin/com/devos/ai/feature/auth/CheckAuthStateUseCaseTest.kt` |
| Modify | `.kiro/implementation-status.md` |

---

## Acceptance Criteria Mapping

| AC | Coverage |
|----|---------|
| AC1: Login screen renders with GitHub and GitLab buttons | `LoginScreen.kt` step 7 |
| AC2: Tapping a button launches Chrome Custom Tab to OAuth URL | `AuthViewModel.loginWithGitHub/GitLab` step 6 |
| AC3: Deep link `devos://auth/callback` returns to app and triggers token exchange | Steps 9, 11, 12 |
| AC4: Token stored in `EncryptedSharedPreferences` via `SecureTokenRepository` | `AuthViewModel.handleAuthCallback` + `AuthModule` steps 3, 6 |
| AC5: On success, user navigates to Home with back stack cleared | `LoginNavEvent.ToHome` + nav pop step 9 |
| AC6: Error state shown on auth failure | `LoginUiState.Error` + `DevOSErrorState` step 7 |
| AC7: Loading indicator shown while awaiting callback | `LoginUiState.Loading` + `CircularProgressIndicator` step 7 |
| AC8: `OAuthProvider` implements `TokenKey` interface | Step 1 |
| AC9: Unit tests pass | Step 13 |

---

## Assumptions

1. `MaterialTheme.spacing` is the Compose extension accessor for `DevOSSpacing` values. If the design system exposes a standalone `DevOSSpacing` object instead of a `MaterialTheme` extension, use `DevOSSpacing.base` etc. in `LoginScreen.kt`. Check `designsystem/theme/DevOSTheme.kt` to confirm before finalising step 7.
2. `DevOSButtonStyle.Secondary` is the correct enum value for a secondary button style. Verify against `DevOSButton.kt` if the enum name differs.
3. The `home` route string used in `loginNavigation` (step 9) matches the constant defined in `DevOSRoutes`. Verify in `DevOSNavGraph.kt` before finalising.
4. `SecureTokenRepositoryImpl` has `@Singleton @Inject constructor` and does NOT require a manually provided binding beyond what `AuthModule` adds. This was confirmed from file read #8.
