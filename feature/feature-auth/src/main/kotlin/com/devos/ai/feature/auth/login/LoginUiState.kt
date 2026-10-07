package com.devos.ai.feature.auth.login

/**
 * UI state for the Login screen.
 *
 * Sealed interface follows the 4-state rule:
 *   [Idle] — buttons enabled, no activity
 *   [Loading] — OAuth flow in progress or token exchange running
 *   [Success] — authentication completed; ViewModel emits [AuthNavEvent.NavigateToHome]
 *   [Error] — authentication failed; message shown in [DevOSErrorState]
 */
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}
