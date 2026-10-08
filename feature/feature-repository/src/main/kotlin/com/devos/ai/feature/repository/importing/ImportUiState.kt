package com.devos.ai.feature.repository.importing

import com.devos.ai.feature.repository.model.RepositoryPreview

/**
 * UI state for the Repository Import screen.
 */
sealed interface ImportUiState {
    /** Initial idle state — URL field empty, no validation performed. */
    data object Idle : ImportUiState

    /** URL is being validated against the remote provider. */
    data object Validating : ImportUiState

    /** Validation succeeded — preview data is available. */
    data class Preview(val preview: RepositoryPreview) : ImportUiState

    /** Import is in progress (cloning + indexing enqueued). */
    data object Importing : ImportUiState

    /** Import completed successfully. */
    data class Success(val repoId: String) : ImportUiState

    /** An error occurred during validation or import. */
    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : ImportUiState
}
