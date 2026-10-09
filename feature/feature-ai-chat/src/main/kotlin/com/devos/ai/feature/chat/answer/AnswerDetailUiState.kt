package com.devos.ai.feature.chat.answer

/**
 * UI state for [AIAnswerDetailScreen].
 *
 * DEVOS-029 / DA-40
 */
sealed interface AnswerDetailUiState {
    data object Loading : AnswerDetailUiState
    data class Success(val answer: AnswerDetail) : AnswerDetailUiState
    data class Error(val message: String, val retryable: Boolean = true) : AnswerDetailUiState
}

/**
 * One-shot navigation events emitted by [AnswerDetailViewModel].
 * Collected in [answerDetailNavigation] — the ViewModel never imports NavController.
 */
sealed class AnswerNavEvent {
    data object NavigateBack : AnswerNavEvent()
    data class NavigateToSources(val answerId: String) : AnswerNavEvent()
    data class NavigateToCode(val filePath: String, val line: Int) : AnswerNavEvent()
    data class NavigateToFollowUp(val question: String) : AnswerNavEvent()
}
