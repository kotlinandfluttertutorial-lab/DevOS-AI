package com.devos.ai.feature.auth.login

/** One-shot navigation events emitted by [AuthViewModel] via SharedFlow. */
sealed class AuthNavEvent {
    /** Navigate to the Home screen after a successful login. */
    data object NavigateToHome : AuthNavEvent()
}
