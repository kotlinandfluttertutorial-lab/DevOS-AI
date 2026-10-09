package com.devos.ai.feature.chat.evidence

import com.devos.ai.feature.chat.answer.SourceEvidence

/**
 * UI state for [AISourceEvidenceScreen].
 *
 * DEVOS-030 / DA-43
 */
sealed interface SourceEvidenceUiState {
    data object Loading : SourceEvidenceUiState
    data class Success(
        val sources: List<SourceEvidence>,
        val questionSummary: String,
    ) : SourceEvidenceUiState
    data class Error(val message: String, val retryable: Boolean = true) : SourceEvidenceUiState
}

/**
 * One-shot navigation events emitted by [SourceEvidenceViewModel].
 * Collected in [sourceEvidenceNavigation] — the ViewModel never imports NavController.
 */
sealed class EvidenceNavEvent {
    data object NavigateBack : EvidenceNavEvent()
    data class NavigateToCode(val filePath: String, val line: Int) : EvidenceNavEvent()
}
