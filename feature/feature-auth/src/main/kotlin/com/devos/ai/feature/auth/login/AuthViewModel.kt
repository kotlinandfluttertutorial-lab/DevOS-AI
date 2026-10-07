package com.devos.ai.feature.auth.login

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.feature.auth.model.OAuthClientIdKey
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
import timber.log.Timber
import javax.inject.Inject

/**
 * OAuth URL templates — client_id is always loaded from [SecureTokenRepository] at
 * runtime. The %s placeholder is substituted by [buildOAuthUrl] before the CCT is
 * opened. If the client ID has not been stored yet, the URL is not opened and an
 * [LoginUiState.Error] is emitted instead.
 *
 * Format: base URL with %s where the client_id value goes.
 */
private const val GITHUB_OAUTH_URL_TEMPLATE =
    "https://github.com/login/oauth/authorize?client_id=%s&scope=repo"
private const val GITLAB_OAUTH_URL_TEMPLATE =
    "https://gitlab.com/oauth/authorize?client_id=%s&response_type=code&scope=api"

/**
 * ViewModel for the Login screen.
 *
 * Responsibilities:
 * - Load the OAuth client ID from [SecureTokenRepository] before opening the CCT.
 * - Set [LoginUiState.Loading] and open a Chrome Custom Tab for OAuth.
 * - Handle the deep-link callback containing the authorization code.
 * - Delegate token exchange + secure storage to [ExchangeCodeForTokenUseCase].
 * - Emit [AuthNavEvent.NavigateToHome] on success via [navEvent] (SharedFlow).
 *
 * Architecture constraints:
 * - Does NOT import NavController — navigation via SharedFlow.
 * - Does NOT use WebView — Chrome Custom Tab only.
 * - Never logs a raw token — only masked form.
 * - Client IDs are read from [SecureTokenRepository], never hardcoded in source.
 *
 * The [ioDispatcher] is not Hilt-injected to avoid requiring a Hilt binding for
 * [CoroutineDispatcher]. Tests construct via the secondary constructor.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val exchangeCodeForTokenUseCase: ExchangeCodeForTokenUseCase,
    private val tokenRepository: SecureTokenRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // Overridable in tests via secondary constructor
    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /** Secondary constructor for test injection of a controlled dispatcher. */
    constructor(
        exchangeCodeForTokenUseCase: ExchangeCodeForTokenUseCase,
        tokenRepository: SecureTokenRepository,
        context: Context,
        ioDispatcher: CoroutineDispatcher,
    ) : this(exchangeCodeForTokenUseCase, tokenRepository, context) {
        this.ioDispatcher = ioDispatcher
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<AuthNavEvent>()
    val navEvent: SharedFlow<AuthNavEvent> = _navEvent.asSharedFlow()

    /**
     * Launch GitHub OAuth flow via Chrome Custom Tab.
     *
     * Loads the GitHub client ID from [SecureTokenRepository]. If no client ID has
     * been stored (i.e. the app has not been configured yet), emits
     * [LoginUiState.Error] rather than opening a tab with an invalid URL.
     */
    fun loginWithGitHub() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val clientId = withContext(ioDispatcher) {
                tokenRepository.getToken(OAuthClientIdKey.GITHUB)
            }
            if (clientId.isNullOrBlank()) {
                Timber.w("GitHub client ID not configured")
                _uiState.value = LoginUiState.Error(
                    "GitHub client ID not configured. Set it via Settings → Developer Credentials."
                )
                return@launch
            }
            val url = GITHUB_OAUTH_URL_TEMPLATE.format(clientId)
            runCatching { openCustomTab(url) }
                .onFailure { _uiState.value = LoginUiState.Error(it.message ?: "Failed to open browser") }
        }
    }

    /**
     * Launch GitLab OAuth flow via Chrome Custom Tab.
     *
     * Loads the GitLab client ID from [SecureTokenRepository]. If no client ID has
     * been stored, emits [LoginUiState.Error] rather than opening a tab with an
     * invalid URL.
     */
    fun loginWithGitLab() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val clientId = withContext(ioDispatcher) {
                tokenRepository.getToken(OAuthClientIdKey.GITLAB)
            }
            if (clientId.isNullOrBlank()) {
                Timber.w("GitLab client ID not configured")
                _uiState.value = LoginUiState.Error(
                    "GitLab client ID not configured. Set it via Settings → Developer Credentials."
                )
                return@launch
            }
            val url = GITLAB_OAUTH_URL_TEMPLATE.format(clientId)
            runCatching { openCustomTab(url) }
                .onFailure { _uiState.value = LoginUiState.Error(it.message ?: "Failed to open browser") }
        }
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
