package com.devos.ai.feature.repository.sync

import com.devos.ai.feature.repository.model.SyncStep

/**
 * UI state for the Repository Sync progress screen.
 */
sealed interface SyncUiState {
    /** Pre-sync idle state. */
    data object Idle : SyncUiState

    /**
     * Active sync in progress.
     *
     * @param steps All pipeline steps with their current state
     * @param currentStepIndex Index of the step currently executing
     * @param overallProgress 0.0–1.0 overall sync progress
     */
    data class Syncing(
        val steps: List<SyncStep>,
        val currentStepIndex: Int,
        val overallProgress: Float,
    ) : SyncUiState

    /** All steps completed successfully. */
    data object Complete : SyncUiState

    /** Sync was cancelled by the user. */
    data object Cancelled : SyncUiState

    /** An error occurred during sync. */
    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : SyncUiState
}
