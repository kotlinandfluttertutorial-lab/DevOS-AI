package com.devos.ai.feature.chat.answer

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

/**
 * ViewModel for [AIAnswerDetailScreen].
 *
 * Loads the answer for [answerId] at init (stub until DEVOS-031 lands).
 * Navigation events are emitted via [navEvent] — never touches NavController.
 *
 * DEVOS-029 / DA-40
 */
@HiltViewModel
class AnswerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val answerId: String = checkNotNull(savedStateHandle["answerId"])

    private val _uiState = MutableStateFlow<AnswerDetailUiState>(AnswerDetailUiState.Loading)
    val uiState: StateFlow<AnswerDetailUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<AnswerNavEvent>()
    val navEvent: SharedFlow<AnswerNavEvent> = _navEvent.asSharedFlow()

    init {
        loadAnswer()
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun loadAnswer() {
        viewModelScope.launch {
            // Stub: return the shared stub answer (id matches any answerId for now)
            _uiState.value = AnswerDetailUiState.Success(
                answer = stubAnswerDetail.copy(id = answerId),
            )
        }
    }

    fun retry() {
        _uiState.value = AnswerDetailUiState.Loading
        loadAnswer()
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun onNavigateBack() {
        viewModelScope.launch { _navEvent.emit(AnswerNavEvent.NavigateBack) }
    }

    fun onViewSources() {
        viewModelScope.launch { _navEvent.emit(AnswerNavEvent.NavigateToSources(answerId)) }
    }

    fun onAskFollowUp() {
        val question = (_uiState.value as? AnswerDetailUiState.Success)?.answer?.question ?: ""
        viewModelScope.launch { _navEvent.emit(AnswerNavEvent.NavigateToFollowUp(question)) }
    }

    fun onSourceTap(filePath: String, line: Int) {
        viewModelScope.launch { _navEvent.emit(AnswerNavEvent.NavigateToCode(filePath, line)) }
    }
}
