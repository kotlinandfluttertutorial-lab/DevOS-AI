package com.devos.ai.feature.repository.overview

import com.devos.ai.feature.repository.model.OverviewTab
import com.devos.ai.feature.repository.model.RepoOverview

/**
 * UI state for the Repository Overview screen.
 *
 * Every screen must handle all 4 states — [Loading], [Success], [Error].
 * The Empty state is covered by the per-tab "Coming soon" placeholders, so it
 * is not a top-level branch here.
 */
sealed interface OverviewUiState {
    data object Loading : OverviewUiState
    data class Success(val overview: RepoOverview, val selectedTab: OverviewTab) : OverviewUiState
    data class Error(val message: String, val retryable: Boolean) : OverviewUiState
}
