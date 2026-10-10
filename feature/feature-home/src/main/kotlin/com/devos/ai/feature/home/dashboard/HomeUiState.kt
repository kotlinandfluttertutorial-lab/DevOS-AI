package com.devos.ai.feature.home.dashboard

import androidx.compose.runtime.Immutable

import com.devos.ai.feature.home.model.AIRecommendation
import com.devos.ai.feature.home.model.ChatSessionSummary
import com.devos.ai.feature.home.model.ProjectHealth
import com.devos.ai.feature.home.model.ProjectSummary

/**
 * UI state for the Home Dashboard screen.
 *
 * Follows the DevOS 4-state pattern: Loading | Success | Empty | Error.
 */
sealed interface HomeUiState {

    /** Data is being loaded. Show [DevOSLoadingState]. */
    data object Loading : HomeUiState

    /**
     * Data loaded successfully. Show the full dashboard content.
     *
     * @param recentProjects Up to 5 most recently touched projects.
     * @param recommendations Active AI recommendations (dismissed ones filtered out).
     * @param health Four-quadrant health snapshot.
     * @param recentSessions Up to 5 most recent AI chat sessions.
     * @param greeting Time-of-day greeting e.g. "Good morning, Dev 👋"
     * @param dateLabel Formatted date string e.g. "Wednesday, Oct 7"
     */
    @Immutable
    data class Success(
        val recentProjects: List<ProjectSummary>,
        val recommendations: List<AIRecommendation>,
        val health: ProjectHealth,
        val recentSessions: List<ChatSessionSummary>,
        val greeting: String = "Good morning, Dev \uD83D\uDC4B",
        val dateLabel: String = "",
    ) : HomeUiState

    /** No projects imported yet. Show the welcome empty state. */
    data object Empty : HomeUiState

    /**
     * An error occurred loading the dashboard.
     *
     * @param message Human-readable error description.
     * @param retryable Whether the user can tap Retry to re-attempt.
     */
    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : HomeUiState
}
