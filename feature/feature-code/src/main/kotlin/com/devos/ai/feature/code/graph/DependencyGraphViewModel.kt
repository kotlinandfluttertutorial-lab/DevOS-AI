package com.devos.ai.feature.code.graph

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

sealed interface DependencyGraphUiState {
    data object Loading : DependencyGraphUiState
    data object ComingSoon : DependencyGraphUiState
    data class Error(val message: String, val retryable: Boolean) : DependencyGraphUiState
}

// ── Nav events ────────────────────────────────────────────────────────────────

sealed interface DependencyGraphNavEvent {
    data object NavigateBack : DependencyGraphNavEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class DependencyGraphViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repoId: String = savedStateHandle["repoId"] ?: ""

    private val _uiState = MutableStateFlow<DependencyGraphUiState>(DependencyGraphUiState.Loading)
    val uiState: StateFlow<DependencyGraphUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<DependencyGraphNavEvent>()
    val navEvent: SharedFlow<DependencyGraphNavEvent> = _navEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            _uiState.value = DependencyGraphUiState.ComingSoon
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _navEvent.emit(DependencyGraphNavEvent.NavigateBack)
        }
    }
}
