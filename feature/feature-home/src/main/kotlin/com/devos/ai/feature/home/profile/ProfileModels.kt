package com.devos.ai.feature.home.profile

import androidx.compose.runtime.Immutable

/**
 * Subscription plan tier.
 */
enum class DevOSPlan {
    FREE,
    PRO,
    TEAM;

    val displayLabel: String
        get() = when (this) {
            FREE -> "Free"
            PRO  -> "Pro"
            TEAM -> "Team"
        }
}

/**
 * User profile data model.
 *
 * [githubHandle] and [gitlabHandle] are null if not connected.
 */
data class UserProfile(
    val name: String,
    val email: String,
    val githubHandle: String?,
    val gitlabHandle: String?,
    val aiSessions: Int,
    val streak: Int,
    val projects: Int,
    val plan: DevOSPlan,
    val planRenewal: String,
)

/**
 * UiState for [ProfileViewModel].
 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    @Immutable
    data class Success(val profile: UserProfile) : ProfileUiState
    data object Empty : ProfileUiState
    data class Error(val message: String, val retryable: Boolean) : ProfileUiState
}

/**
 * One-shot navigation events emitted by [ProfileViewModel].
 */
sealed interface ProfileNavEvent {
    data object NavigateBack : ProfileNavEvent
    data object NavigateToLogin : ProfileNavEvent
}
