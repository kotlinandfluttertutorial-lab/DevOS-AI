package com.devos.ai.feature.code.architecture

import androidx.lifecycle.SavedStateHandle
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

// ── UiState ──────────────────────────────────────────────────────────────────

sealed interface ArchitectureUiState {
    data object Loading : ArchitectureUiState
    data object ComingSoon : ArchitectureUiState
    data class Error(val message: String, val retryable: Boolean) : ArchitectureUiState
}

// ── Nav events ────────────────────────────────────────────────────────────────

sealed interface ArchitectureNavEvent {
    data object NavigateBack : ArchitectureNavEvent
    data class NavigateToAiChat(val context: String) : ArchitectureNavEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ArchitectureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repoId: String = savedStateHandle["repoId"] ?: ""

    private val _uiState = MutableStateFlow<ArchitectureUiState>(ArchitectureUiState.Loading)
    val uiState: StateFlow<ArchitectureUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ArchitectureNavEvent>()
    val navEvent: SharedFlow<ArchitectureNavEvent> = _navEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            _uiState.value = ArchitectureUiState.ComingSoon
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _navEvent.emit(ArchitectureNavEvent.NavigateBack)
        }
    }

    fun onAskAiTap() {
        viewModelScope.launch {
            _navEvent.emit(
                ArchitectureNavEvent.NavigateToAiChat("Analyze architecture of repository: $repoId"),
            )
        }
    }
}
