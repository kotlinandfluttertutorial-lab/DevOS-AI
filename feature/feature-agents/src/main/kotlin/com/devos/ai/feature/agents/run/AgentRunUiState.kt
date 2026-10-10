package com.devos.ai.feature.agents.run

import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.model.AgentStep

/**
 * UI state for the Agent Run screen (DEVOS-035 / FIGMA-19).
 *
 * Follows the DevOS 4-state pattern: Loading | Running | Completed | Error.
 */
sealed interface AgentRunUiState {
    data object Loading : AgentRunUiState

    data class Running(
        val run: AgentRun,
        /** 0.0–1.0 progress fraction for the LinearProgressIndicator. */
        val progress: Float,
        /** Human-readable elapsed time, e.g. "47s". */
        val elapsedLabel: String,
        /** Step label shown under the progress bar, e.g. "Step 3 of 6". */
        val stepLabel: String,
    ) : AgentRunUiState

    data class Completed(
        val run: AgentRun,
        val finalAnswer: String,
    ) : AgentRunUiState

    data class Cancelled(val run: AgentRun) : AgentRunUiState

    data class Error(
        val run: AgentRun?,
        val message: String,
        val retryable: Boolean = false,
    ) : AgentRunUiState
}

/** One-shot navigation events emitted by [AgentRunViewModel]. */
sealed class AgentRunNavEvent {
    data object NavigateBack : AgentRunNavEvent()
    data class NavigateToToolDetail(val runId: String, val stepId: String) : AgentRunNavEvent()
}
