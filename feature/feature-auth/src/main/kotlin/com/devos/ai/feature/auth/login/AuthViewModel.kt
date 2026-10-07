package com.devos.ai.feature.auth.login

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.auth.model.OAuthProvider
import com.devos.ai.feature.auth.usecase.ExchangeCodeForTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * OAuth URL constants.
 *
 * client_id is PLACEHOLDER — real values must be loaded from EncryptedSharedPreferences,
 * NOT from BuildConfig or hardcoded source. These placeholders ensure the OAuth flow
 * is exercisable without a live registration.
 */
private const val GITHUB_OAUTH_URL =
    "https://github.com/login/oauth/authorize?client_id=PLACEHOLDER&scope=repo"
private const val GITLAB_OAUTH_URL =
    "https://gitlab.com/oauth/authorize?client_id=PLACEHOLDER&response_type=code&scope=api"

/**
 * ViewModel for the Login screen.
 *
 * Responsibilities:
 * - Set [LoginUiState.Loading] and open a Chrome Custom Tab for OAuth.
 * - Handle the deep-link callback containing the authorization code.
 * - Delegate token exchange + secure storage to [ExchangeCodeForTokenUseCase].
 * - Emit [AuthNavEvent.NavigateToHome] on success via [navEvent] (SharedFlow).
 *
 * Architecture constraints:
 * - Does NOT import NavController — navigation via SharedFlow.
 * - Does NOT use WebView — Chrome Custom Tab only.
 * - Never logs a raw token — only masked form.
 *
 * The [ioDispatcher] is not Hilt-injected to avoid requiring a Hilt binding for
 * [CoroutineDispatcher]. Tests construct via the secondary constructor.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val exchangeCodeForTokenUseCase: ExchangeCodeForTokenUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // Overridable in tests via secondary constructor
    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /** Secondary constructor for test injection of a controlled dispatcher. */
    constructor(
        exchangeCodeForTokenUseCase: ExchangeCodeForTokenUseCase,
        context: Context,
        ioDispatcher: CoroutineDispatcher,
    ) : this(exchangeCodeForTokenUseCase, context) {
        this.ioDispatcher = ioDispatcher
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<AuthNavEvent>()
    val navEvent: SharedFlow<AuthNavEvent> = _navEvent.asSharedFlow()

    /**
     * Launch GitHub OAuth flow via Chrome Custom Tab.
     * Sets [LoginUiState.Loading] before attempting the CCT launch.
     */
    fun loginWithGitHub() {
        _uiState.value = LoginUiState.Loading
        runCatching { openCustomTab(GITHUB_OAUTH_URL) }
            .onFailure { _uiState.value = LoginUiState.Error(it.message ?: "Failed to open browser") }
    }

    /**
     * Launch GitLab OAuth flow via Chrome Custom Tab.
     * Sets [LoginUiState.Loading] before attempting the CCT launch.
     */
    fun loginWithGitLab() {
        _uiState.value = LoginUiState.Loading
        runCatching { openCustomTab(GITLAB_OAUTH_URL) }
            .onFailure { _uiState.value = LoginUiState.Error(it.message ?: "Failed to open browser") }
    }

    /**
     * Process the deep-link callback from the OAuth provider.
     *
     * Called from the NavGraph after it receives `devos://auth/callback?code=…`.
     *
     * @param code Authorization code extracted from the deep-link URI.
     * @param provider Which provider the callback came from.
     */
    fun handleAuthCallback(code: String, provider: OAuthProvider) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = withContext(ioDispatcher) {
                exchangeCodeForTokenUseCase(code, provider)
            }
            result
                .onSuccess {
                    _uiState.value = LoginUiState.Success
                    _navEvent.emit(AuthNavEvent.NavigateToHome)
                }
                .onFailure { throwable ->
                    _uiState.value = LoginUiState.Error(
                        throwable.message ?: "Login failed",
                    )
                }
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun openCustomTab(url: String) {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(context, url.toUri())
    }
}
