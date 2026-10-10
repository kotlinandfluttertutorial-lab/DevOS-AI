package com.devos.ai.feature.learning.dashboard

import androidx.compose.runtime.Immutable

import com.devos.ai.feature.learning.model.CourseProgress
import com.devos.ai.feature.learning.model.CourseRecommendation
import com.devos.ai.feature.learning.model.QuizScore

/**
 * UI state for the Learning Dashboard screen.
 *
 * Four states: Loading | Success | Empty | Error
 */
sealed interface LearningDashboardUiState {

    data object Loading : LearningDashboardUiState

    @Immutable

    data class Success(
        /** Lessons completed today vs daily goal. */
        val lessonsToday: Int,
        val dailyGoal: Int,
        /** Current day streak. */
        val streak: Int,
        /** Course currently in progress, or null if none. */
        val currentCourse: CourseProgress?,
        /** Recommended courses. */
        val recommendations: List<CourseRecommendation>,
        /** Recent quiz scores. */
        val recentScores: List<QuizScore>,
    ) : LearningDashboardUiState {
        val dailyFraction: Float get() =
            if (dailyGoal == 0) 0f else (lessonsToday.toFloat() / dailyGoal).coerceAtMost(1f)
    }

    data object Empty : LearningDashboardUiState

    data class Error(val message: String, val retryable: Boolean) : LearningDashboardUiState
}

/** One-shot navigation events emitted by [LearningDashboardViewModel]. */
sealed class LearningDashboardNavEvent {
    data class NavigateToCourse(val courseId: String) : LearningDashboardNavEvent()
    data object NavigateToSearch : LearningDashboardNavEvent()
}
