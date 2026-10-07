package com.devos.ai.feature.auth.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.feature.auth.model.OAuthProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Result returned by [CheckAuthStateUseCase].
 *
 * @param onboardingComplete true if the user has already completed onboarding.
 * @param isAuthenticated true if a valid access token is present for at least one provider.
 */
data class AuthCheckResult(
    val onboardingComplete: Boolean,
    val isAuthenticated: Boolean,
)

/**
 * Checks DataStore for the onboarding flag and queries [SecureTokenRepository] for
 * an existing token (indicating prior successful authentication).
 *
 * Returns a safe default of (false, false) on any error so the flow degrades
 * gracefully if dependencies are not fully wired.
 */
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
        // Authenticated if any provider has a stored token
        val isAuthenticated = OAuthProvider.entries.any { tokenRepository.hasToken(it) }
        AuthCheckResult(
            onboardingComplete = onboardingComplete,
            isAuthenticated = isAuthenticated,
        )
    }.getOrDefault(AuthCheckResult(onboardingComplete = false, isAuthenticated = false))
}
