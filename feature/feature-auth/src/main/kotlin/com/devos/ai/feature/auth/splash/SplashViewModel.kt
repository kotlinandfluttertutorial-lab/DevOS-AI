package com.devos.ai.feature.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.auth.usecase.CheckAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Minimum duration (ms) the splash screen is visible before navigation fires. */
private const val MINIMUM_SPLASH_DISPLAY_MS = 1500L

/**
 * ViewModel for the Splash screen.
 *
 * On creation, checks auth state via [CheckAuthStateUseCase] after a minimum display
 * delay of 1500 ms. Emits a one-shot [SplashNavEvent] via [navEvent] to direct the
 * NavGraph to the correct destination:
 *
 * - Not onboarded → [SplashNavEvent.ToOnboarding]
 * - Onboarded + authenticated → [SplashNavEvent.ToHome]
 * - Onboarded + unauthenticated → [SplashNavEvent.ToLogin]
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAuthState: CheckAuthStateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<SplashNavEvent>()
    val navEvent: SharedFlow<SplashNavEvent> = _navEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            // Ensure minimum visible duration before navigating
            delay(MINIMUM_SPLASH_DISPLAY_MS)

            val result = checkAuthState()
            val event = when {
                result.isAuthenticated -> SplashNavEvent.ToHome
                !result.onboardingComplete -> SplashNavEvent.ToOnboarding
                else -> SplashNavEvent.ToLogin
            }

            _uiState.value = when (event) {
                SplashNavEvent.ToHome -> SplashUiState.NavigateToHome
                SplashNavEvent.ToOnboarding -> SplashUiState.NavigateToOnboarding
                SplashNavEvent.ToLogin -> SplashUiState.NavigateToOnboarding // onboarding done; reuse as interim
            }

            _navEvent.emit(event)
        }
    }
}
