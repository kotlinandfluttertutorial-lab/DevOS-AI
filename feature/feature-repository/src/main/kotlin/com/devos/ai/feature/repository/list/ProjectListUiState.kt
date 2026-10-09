package com.devos.ai.feature.repository.list

import com.devos.ai.feature.repository.model.ProjectSummary
import com.devos.ai.feature.repository.model.SortOrder

/**
 * UI state for the Project / Repository List screen (DEVOS-017).
 *
 * All 4 required states are present: Loading, Success, Empty, Error.
 * [Success] carries the filtered/sorted list plus current sort and filter selection.
 */
sealed interface ProjectListUiState {
    data object Loading : ProjectListUiState

    data class Success(
        val repositories: List<ProjectSummary>,
        val sortOrder: SortOrder,
        val filterLanguage: String?,
        val searchQuery: String,
    ) : ProjectListUiState

    data object Empty : ProjectListUiState

    data class Error(val message: String, val retryable: Boolean) : ProjectListUiState
}
