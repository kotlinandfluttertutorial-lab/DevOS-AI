# Kiro Prompt — DEVOS-009: Splash, Onboarding, Auth & Secure Token Storage

**Jira:** DEVOS-009 / DEVOS-010 / DEVOS-011 / DEVOS-012  
**Epic:** DEVOS-E01  
**Figma:** FIGMA-01 / FIGMA-02 / FIGMA-03  
**Kiro Spec:** `.kiro/specs/foundation/auth.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the complete authentication entry flow for DevOS AI: Splash screen, 4-step Onboarding carousel, GitHub/GitLab OAuth Login, and secure token storage.

**Existing files to read first:**
- `.kiro/specs/foundation/auth.md` — auth architecture and token security model
- `docs/figma/screen-inventory.md` — Screens FIGMA-01, FIGMA-02, FIGMA-03
- `docs/figma/component-inventory.md` — DevOSButton, DevOSCard, DevOSTopBar
- `.kiro/steering/android.md` — architecture rules

**Module:** `feature/feature-auth/` + `core/core-security/`  
**Package:** `com.devos.ai.feature.auth`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- OAuth tokens stored ONLY in EncryptedSharedPreferences with AES256_GCM — never cleartext SharedPreferences, BuildConfig, or logs.
- Never log tokens, auth codes, or client secrets anywhere in the codebase.
- Auth state checked in SplashViewModel, not in NavGraph directly.
- Deep link callback validated server-side before token exchange.
- Any UI showing token status must display `token.take(4) + "****"` — never the full token.

**What to implement:**

### 1. SplashScreen (DEVOS-009, FIGMA-01)

Route: `SPLASH`

UiState:
```kotlin
sealed interface SplashUiState {
    data object Loading : SplashUiState
    data object NavigateToOnboarding : SplashUiState
    data object NavigateToHome : SplashUiState
}
```

ViewModel: `SplashViewModel`
- Calls `CheckAuthStateUseCase` on launch
- Emits `NavigateToOnboarding` if first launch (DataStore flag `onboarding_complete = false`) or unauthenticated
- Emits `NavigateToHome` if already authenticated
- Navigation events via `SharedFlow<SplashNavEvent>`

Layout:
```kotlin
Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha),
    ) {
        // DevOS AI logo (vector drawable)
        Image(
            painter = painterResource(R.drawable.ic_devos_logo),
            contentDescription = "DevOS AI logo",
            modifier = Modifier.size(96.dp)
        )
        Spacer(Modifier.height(MaterialTheme.spacing.base))
        Text(
            "Understand your code. Learn faster. Build smarter.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl)
        )
    }
}
```
- `visible` driven by `LaunchedEffect(Unit) { delay(100); visible = true }`
- Background must be `MaterialTheme.colorScheme.background` — prevents white flash
- Minimum display time 1500ms before navigation fires

### 2. OnboardingScreen (DEVOS-010, FIGMA-02)

Route: `ONBOARDING`

UiState:
```kotlin
sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState
    data class Success(val pages: List<OnboardingPage>, val currentPage: Int) : OnboardingUiState
}
```

Onboarding pages (4):
```kotlin
val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.FolderOpen,
        title = "Repository Intelligence",
        description = "Import any GitHub or GitLab repo. DevOS indexes everything so you can explore and search instantly."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Code,
        title = "Code Intelligence",
        description = "Browse files, search symbols, and visualize dependencies — all from your phone."
    ),
    OnboardingPage(
        icon = Icons.Outlined.SmartToy,
        title = "AI Platform",
        description = "Ask questions grounded in your actual code. AI that knows your repository, not just generic answers."
    ),
    OnboardingPage(
        icon = Icons.Outlined.School,
        title = "Learn While You Build",
        description = "Get personalized learning recommendations based on the patterns in your current project."
    ),
)
```

ViewModel: `OnboardingViewModel`
- `fun skipOnboarding()` — sets `onboarding_complete = true` in DataStore, navigates to Login
- `fun completeOnboarding()` — same, navigates to Login
- `fun nextPage()` / `fun previousPage()`

Layout:
```kotlin
Scaffold(
    topBar = {
        Row(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSkip) {
                Text("Skip", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    },
    bottomBar = {
        Column(Modifier.padding(MaterialTheme.spacing.base)) {
            // Step indicator dots
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                pages.forEachIndexed { index, _ ->
                    StepDot(active = index == currentPage)
                    if (index < pages.lastIndex) Spacer(Modifier.width(MaterialTheme.spacing.xs))
                }
            }
            Spacer(Modifier.height(MaterialTheme.spacing.base))
            if (currentPage == pages.lastIndex) {
                DevOSButton(
                    text = "Get Started",
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                DevOSButton(
                    text = "Next",
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
) { padding ->
    HorizontalPager(state = pagerState, modifier = Modifier.padding(padding)) { page ->
        OnboardingPageContent(pages[page])
    }
}
```
- Only shown on first launch — checked via DataStore `onboarding_complete` flag
- `HorizontalPager` uses `rememberPagerState`; pager state synced to ViewModel via `LaunchedEffect`

### 3. LoginScreen (DEVOS-011, FIGMA-03)

Route: `LOGIN`

UiState:
```kotlin
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}
```

ViewModel: `AuthViewModel`
- `fun loginWithGitHub()` — opens Chrome Custom Tab with GitHub OAuth URL; listens for deep link
- `fun loginWithGitLab()` — opens Chrome Custom Tab with GitLab OAuth URL; listens for deep link
- `fun handleAuthCallback(code: String, provider: OAuthProvider)` — calls `ExchangeCodeForTokenUseCase`
- Navigation: `SharedFlow<AuthNavEvent>` emits `NavigateToHome` on success

Deep link: `devos://auth/callback` (registered in `AndroidManifest.xml` on `MainActivity`)

Layout:
```kotlin
Scaffold(
    topBar = { DevOSTopBar(title = "Sign In") }
) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome to DevOS AI",
            style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(MaterialTheme.spacing.sm))
        Text("Connect your repository host to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(MaterialTheme.spacing.xl))

        DevOSButton(
            text = "Continue with GitHub",
            onClick = onLoginGitHub,
            leadingIcon = { Icon(painterResource(R.drawable.ic_github), contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(MaterialTheme.spacing.base))
        DevOSButton(
            text = "Continue with GitLab",
            onClick = onLoginGitLab,
            style = DevOSButtonStyle.Secondary,
            leadingIcon = { Icon(painterResource(R.drawable.ic_gitlab), contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState is LoginUiState.Error) {
            Spacer(Modifier.height(MaterialTheme.spacing.base))
            DevOSErrorState(
                description = (uiState as LoginUiState.Error).message,
                onRetry = null
            )
        }
    }
}
```

### 4. Secure Token Storage (DEVOS-012)

```kotlin
// core/core-security/SecureTokenRepository.kt
class SecureTokenRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SecureTokenRepository {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "devos_secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun saveToken(provider: OAuthProvider, token: String) {
        prefs.edit().putString(provider.prefKey, token).apply()
        // Log only the masked token — never the real value
        Timber.d("Token saved for ${provider.name}: ${token.take(4)}****")
    }

    override suspend fun getToken(provider: OAuthProvider): String? =
        prefs.getString(provider.prefKey, null)

    override suspend fun clearToken(provider: OAuthProvider) {
        prefs.edit().remove(provider.prefKey).apply()
    }

    override suspend fun hasToken(provider: OAuthProvider): Boolean =
        getToken(provider) != null
}
```

**Token exchange UseCase:**
```kotlin
class ExchangeCodeForTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenRepository: SecureTokenRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(code: String, provider: OAuthProvider): Result<Unit> =
        withContext(dispatcher) {
            authRepository.exchangeCodeForToken(code, provider)
                .onSuccess { token -> tokenRepository.saveToken(provider, token) }
                .map { Unit }
        }
}
```

**Tests:**
- `SplashViewModelTest`: authenticated → NavigateToHome; unauthenticated + first launch → NavigateToOnboarding; unauthenticated + not first launch → NavigateToLogin
- `OnboardingViewModelTest`: page advances; skip fires navigateToLogin; complete on last page fires navigateToLogin; DataStore flag set on complete
- `AuthViewModelTest`: GitHub OAuth flow triggers Chrome Custom Tab; callback code handled; success navigates to Home; error state rendered
- `SecureTokenRepositoryTest`: token saved with EncryptedSharedPreferences; token survives process death; clearToken removes token; never writes to plain SharedPreferences
- `ExchangeCodeForTokenUseCaseTest`: successful exchange saves token; failed exchange returns error

**Acceptance Criteria:**
- AC1 (DEVOS-009): Logo animates with scale+fade on launch
- AC2 (DEVOS-009): Tagline "Understand your code. Learn faster. Build smarter." visible on splash
- AC3 (DEVOS-009): Authenticated users transition directly to Home; unauthenticated users go to Onboarding (first launch) or Login
- AC4 (DEVOS-009): No white flash during launch (background set before system splash exits)
- AC5 (DEVOS-010): 4 onboarding pages render with correct icons and descriptions
- AC6 (DEVOS-010): Horizontal pager with step indicator dots visible
- AC7 (DEVOS-010): Skip button navigates directly to Login
- AC8 (DEVOS-010): "Get Started" button on last page navigates to Login
- AC9 (DEVOS-010): Onboarding shown only on first launch — skipped on subsequent launches
- AC10 (DEVOS-011): GitHub OAuth flow completes end-to-end via Chrome Custom Tab
- AC11 (DEVOS-011): GitLab OAuth flow completes end-to-end via Chrome Custom Tab
- AC12 (DEVOS-011): Token stored in EncryptedSharedPreferences — confirmed by inspecting storage (no plaintext in app data)
- AC13 (DEVOS-011): Error state rendered when OAuth returns an error or network fails
- AC14 (DEVOS-011): Token never appears in Logcat — only masked form `XXXX****`
- AC15 (DEVOS-012): EncryptedSharedPreferences with AES256_GCM used for all token storage
- AC16 (DEVOS-012): No tokens visible in Logcat under any error condition
- AC17 (DEVOS-012): No tokens stored in cleartext (verified by inspecting app SharedPreferences directory)
