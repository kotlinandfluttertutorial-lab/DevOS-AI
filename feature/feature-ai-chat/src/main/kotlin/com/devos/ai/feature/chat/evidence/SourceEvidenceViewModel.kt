package com.devos.ai.feature.chat.evidence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.chat.answer.stubAnswerDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [AISourceEvidenceScreen].
 *
 * Loads stub sources for [answerId] at init (stub until DEVOS-031 lands).
 * Navigation events are emitted via [navEvent] — never touches NavController.
 *
 * DEVOS-030 / DA-43
 */
@HiltViewModel
class SourceEvidenceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val answerId: String = checkNotNull(savedStateHandle["answerId"])

    private val _uiState = MutableStateFlow<SourceEvidenceUiState>(SourceEvidenceUiState.Loading)
    val uiState: StateFlow<SourceEvidenceUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<EvidenceNavEvent>()
    val navEvent: SharedFlow<EvidenceNavEvent> = _navEvent.asSharedFlow()

    init {
        loadSources()
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun loadSources() {
        viewModelScope.launch {
            // Stub: return sources from the shared stub answer
            _uiState.value = SourceEvidenceUiState.Success(
                sources = stubAnswerDetail.sources,
                questionSummary = stubAnswerDetail.question,
            )
        }
    }

    fun retry() {
        _uiState.value = SourceEvidenceUiState.Loading
        loadSources()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun onNavigateBack() {
        viewModelScope.launch { _navEvent.emit(EvidenceNavEvent.NavigateBack) }
    }

    fun onOpenInCodeViewer(filePath: String, line: Int) {
        viewModelScope.launch { _navEvent.emit(EvidenceNavEvent.NavigateToCode(filePath, line)) }
    }
}
