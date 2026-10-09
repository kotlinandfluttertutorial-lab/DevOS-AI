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

/** One-shot navigation events emitted by [AISettingsViewModel]. */
sealed class AISettingsNavEvent {
    /** Navigate back to the previous screen. */
    data object NavigateBack : AISettingsNavEvent()

    /** Navigate to the provider settings screen. */
    data object NavigateToProviders : AISettingsNavEvent()

    /** Navigate to the model picker screen. */
    data object NavigateToModelPicker : AISettingsNavEvent()
}

/** UI state for the AI settings screen. */
sealed interface AISettingsUiState {
    data object Loading : AISettingsUiState

    data class Success(val settings: AISettings) : AISettingsUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : AISettingsUiState
}

/**
 * ViewModel for [AISettingsScreen].
 *
 * Loads stub [AISettings] at init; each update* function mutates local state.
 * All settings will be persisted to DataStore in a future ticket.
 *
 * Navigation events are emitted via [SharedFlow] — NavController is never imported here.
 *
 * DEVOS-033 / DA-46
 */
@HiltViewModel
class AISettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<AISettingsUiState>(AISettingsUiState.Loading)
    val uiState: StateFlow<AISettingsUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<AISettingsNavEvent>()
    val navEvent: SharedFlow<AISettingsNavEvent> = _navEvent.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Stub load — replace with DataStore read in a follow-up ticket
            _uiState.value = AISettingsUiState.Success(AISettings())
        }
    }

    /** Retry loading settings after an error. */
    fun retry() {
        _uiState.value = AISettingsUiState.Loading
        loadSettings()
    }

    /** Update the top-K results (RAG) setting. */
    fun updateTopKResults(value: Int) = updateSettings { copy(topKResults = value) }

    /** Update the chunk size (RAG) setting. */
    fun updateChunkSize(value: Int) = updateSettings { copy(chunkSize = value) }

    /** Update the agent max steps setting. */
    fun updateAgentMaxSteps(value: Int) = updateSettings { copy(agentMaxSteps = value) }

    /** Toggle the auto-approve safe tools setting. */
    fun updateAutoApproveSafeTools(enabled: Boolean) =
        updateSettings { copy(autoApproveSafeTools = enabled) }

    /** Toggle the developer memory setting. */
    fun updateMemoryEnabled(enabled: Boolean) = updateSettings { copy(memoryEnabled = enabled) }

    /** Emit navigate-back event. */
    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(AISettingsNavEvent.NavigateBack) }
    }

    /** Emit navigate-to-providers event. */
    fun navigateToProviders() {
        viewModelScope.launch { _navEvent.emit(AISettingsNavEvent.NavigateToProviders) }
    }

    /** Emit navigate-to-model-picker event. */
    fun navigateToModelPicker() {
        viewModelScope.launch { _navEvent.emit(AISettingsNavEvent.NavigateToModelPicker) }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun updateSettings(update: AISettings.() -> AISettings) {
        val current = _uiState.value
        if (current is AISettingsUiState.Success) {
            _uiState.value = current.copy(settings = current.settings.update())
        }
    }
}
