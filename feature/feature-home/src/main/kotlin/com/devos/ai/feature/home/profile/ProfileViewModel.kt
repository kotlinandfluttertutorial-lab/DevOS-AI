package com.devos.ai.feature.home.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.core.common.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
 * ViewModel for the Profile screen.
 *
 * Loads stub profile data and handles sign-out by emitting [ProfileNavEvent.NavigateToLogin].
 * Actual token clearing will be wired when real auth is plumbed in (DEVOS-061).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ProfileNavEvent>()
    val navEvent: SharedFlow<ProfileNavEvent> = _navEvent.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val profile = withContext(ioDispatcher) { stubProfile() }
                _uiState.value = ProfileUiState.Success(profile)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    message = e.message ?: "Failed to load profile",
                    retryable = true,
                )
            }
        }
    }

    /**
     * Initiates sign-out. Token clearing to be implemented when real auth is wired.
     * Emits [ProfileNavEvent.NavigateToLogin] to direct the user back to the auth flow.
     */
    fun onSignOut() {
        viewModelScope.launch {
            // TODO(DEVOS-061): clear EncryptedSharedPreferences tokens here via SignOutUseCase
            _navEvent.emit(ProfileNavEvent.NavigateToLogin)
        }
    }

    fun onBack() {
        viewModelScope.launch { _navEvent.emit(ProfileNavEvent.NavigateBack) }
    }

    // ── Stub data — replace after DEVOS-061 ──────────────────────────────────

    private fun stubProfile() = UserProfile(
        name = "Firoj Mohammad",
        email = "firoj@devos.ai",
        githubHandle = "firoj-dev",
        gitlabHandle = "firoj-gl",
        aiSessions = 247,
        streak = 12,
        projects = 4,
        plan = DevOSPlan.PRO,
        planRenewal = "Pro · Renews Jan 1, 2026",
    )
}
