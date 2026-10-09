package com.devos.ai.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Settings Root screen. */
data class SettingsRootUiState(
    val darkMode: Boolean = true,
)

/** One-shot navigation events emitted by [SettingsRootViewModel]. */
sealed class SettingsRootNavEvent {
    data object NavigateBack : SettingsRootNavEvent()
    data object NavigateToAISettings : SettingsRootNavEvent()
    data object NavigateToProviderSettings : SettingsRootNavEvent()
    data object NavigateToDeveloperMemory : SettingsRootNavEvent()
    data object NavigateToProfile : SettingsRootNavEvent()
    data object NavigateToNotifications : SettingsRootNavEvent()
}

/**
 * ViewModel for [SettingsRootScreen].
 *
 * Exposes dark mode toggle stub and navigation events.
 * NavController is never imported here.
 *
 * DEVOS-062 / DA-78
 */
@HiltViewModel
class SettingsRootViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsRootUiState())
    val uiState: StateFlow<SettingsRootUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<SettingsRootNavEvent>()
    val navEvent: SharedFlow<SettingsRootNavEvent> = _navEvent.asSharedFlow()

    /** Toggle the dark-mode preference. */
    fun toggleDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(darkMode = enabled)
    }

    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(SettingsRootNavEvent.NavigateBack) }
    }

    fun navigateToAISettings() {
        viewModelScope.launch { _navEvent.emit(SettingsRootNavEvent.NavigateToAISettings) }
    }

    fun navigateToProviderSettings() {
        viewModelScope.launch { _navEvent.emit(SettingsRootNavEvent.NavigateToProviderSettings) }
    }

    fun navigateToDeveloperMemory() {
        viewModelScope.launch { _navEvent.emit(SettingsRootNavEvent.NavigateToDeveloperMemory) }
    }

    fun navigateToProfile() {
        viewModelScope.launch { _navEvent.emit(SettingsRootNavEvent.NavigateToProfile) }
    }

    fun navigateToNotifications() {
        viewModelScope.launch { _navEvent.emit(SettingsRootNavEvent.NavigateToNotifications) }
    }
}
