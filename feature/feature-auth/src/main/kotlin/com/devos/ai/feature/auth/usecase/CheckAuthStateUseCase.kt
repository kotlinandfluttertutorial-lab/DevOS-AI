package com.devos.ai.feature.auth.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Result returned by [CheckAuthStateUseCase].
 *
 * @param onboardingComplete true if the user has already completed onboarding.
 * @param isAuthenticated true if a valid access token is present.
 */
data class AuthCheckResult(
    val onboardingComplete: Boolean,
    val isAuthenticated: Boolean,
)

/**
 * Checks DataStore for the onboarding flag and token presence.
 *
 * Returns a safe default of (false, false) on any error so the flow
 * degrades gracefully if dependencies are not fully wired.
 */
class CheckAuthStateUseCase @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    suspend operator fun invoke(): AuthCheckResult = runCatching {
        val prefs = dataStore.data.first()
        val onboardingComplete = prefs[KEY_ONBOARDING_COMPLETE] ?: false
        // Token check: a real implementation would verify via SecureTokenRepository.
        // Stub returns false until DEVOS-011 wires the auth repository.
        AuthCheckResult(onboardingComplete = onboardingComplete, isAuthenticated = false)
    }.getOrDefault(AuthCheckResult(onboardingComplete = false, isAuthenticated = false))
}
