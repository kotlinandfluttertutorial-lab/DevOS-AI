package com.devos.ai.feature.learning.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.learning.model.CourseProgress
import com.devos.ai.feature.learning.model.CourseRecommendation
import com.devos.ai.feature.learning.model.IconCategory
import com.devos.ai.feature.learning.model.QuizScore
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
 * ViewModel for the Learning Dashboard screen (DEVOS-050).
 *
 * Loads stub learning data and emits one-shot navigation events via [navEvent].
 * TODO(DEVOS-054): replace stub data with real domain use cases.
 */
@HiltViewModel
class LearningDashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<LearningDashboardUiState>(LearningDashboardUiState.Loading)
    val uiState: StateFlow<LearningDashboardUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<LearningDashboardNavEvent>()
    val navEvent: SharedFlow<LearningDashboardNavEvent> = _navEvent.asSharedFlow()

    init {
        loadDashboard()
    }

    /** Loads dashboard data. Call again on retry. */
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = LearningDashboardUiState.Loading
            try {
                // TODO(DEVOS-054): call GetLearningDashboardUseCase
                _uiState.value = LearningDashboardUiState.Success(
                    lessonsToday = 3,
                    dailyGoal = 5,
                    streak = 12,
                    currentCourse = stubCurrentCourse(),
                    recommendations = stubRecommendations(),
                    recentScores = stubRecentScores(),
                )
            } catch (e: Exception) {
                _uiState.value = LearningDashboardUiState.Error(
                    message = e.message ?: "Failed to load learning dashboard",
                    retryable = true,
                )
            }
        }
    }

    fun onContinueCourse(courseId: String) = emit(LearningDashboardNavEvent.NavigateToCourse(courseId))

    fun onRecommendationTap(courseId: String) = emit(LearningDashboardNavEvent.NavigateToCourse(courseId))

    fun onSearchTap() = emit(LearningDashboardNavEvent.NavigateToSearch)

    private fun emit(event: LearningDashboardNavEvent) {
        viewModelScope.launch { _navEvent.emit(event) }
    }

    // ── Stub data — remove after DEVOS-054 ────────────────────────────────────

    private fun stubCurrentCourse() = CourseProgress(
        courseId = "course-kotlin-coroutines",
        title = "Kotlin Coroutines & Flow",
        subtitle = "Based on patterns in DevOS AI repo",
        totalLessons = 8,
        completedLessons = 4,
        estimatedMinutesLeft = 20,
        repoConnection = "DevOS AI",
    )

    private fun stubRecommendations() = listOf(
        CourseRecommendation(
            id = "rec-clean-arch",
            title = "Clean Architecture Patterns",
            subtitle = "5 lessons · 30 min",
            iconCategory = IconCategory.ARCHITECTURE,
            isNew = true,
        ),
        CourseRecommendation(
            id = "rec-testing",
            title = "Android Testing with Turbine",
            subtitle = "4 lessons · 25 min",
            iconCategory = IconCategory.TESTING,
            isNew = false,
        ),
        CourseRecommendation(
            id = "rec-rag",
            title = "RAG with Kotlin & LLMs",
            subtitle = "6 lessons · 40 min",
            iconCategory = IconCategory.AI,
            isNew = true,
        ),
    )

    private fun stubRecentScores() = listOf(
        QuizScore(quizTitle = "Coroutines Quiz", score = 9, total = 10),
        QuizScore(quizTitle = "Flow Operators Quiz", score = 7, total = 10),
    )
}
