package com.devos.ai.feature.auth.login

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.core.common.di.IoDispatcher
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.feature.auth.model.OAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
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

/**
 * Placeholder client ID constants — real IDs will be loaded from EncryptedSharedPreferences
 * in a follow-up ticket (DEVOS-012). Never place real client IDs in source code.
 */
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
                    // SECURITY: token already masked inside SecureTokenRepositoryImpl
                    Timber.d("Auth callback handled successfully for ${provider.name}")
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

    /**
     * Exchanges an OAuth authorization code for an access token.
     *
     * Internal visibility so it can be tested directly in unit tests.
     * Returns null on any HTTP or parsing failure.
     */
    internal suspend fun exchangeCodeForToken(code: String, provider: OAuthProvider): String? =
        withContext(ioDispatcher) {
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
            runCatching {
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null
                val responseBody = response.body?.string() ?: return@withContext null
                JSONObject(responseBody).optString("access_token").takeIf { it.isNotEmpty() }
            }.getOrNull()
        }
}
