package com.devos.ai.feature.learning.course

import com.devos.ai.feature.learning.model.Course

/**
 * UI state for the Course Details screen (DEVOS-051, FIGMA-29).
 */
sealed interface CourseDetailsUiState {

    data object Loading : CourseDetailsUiState

    data class Success(val course: Course) : CourseDetailsUiState

    data object Empty : CourseDetailsUiState

    data class Error(val message: String, val retryable: Boolean) : CourseDetailsUiState
}

/** One-shot navigation events emitted by [CourseDetailsViewModel]. */
sealed class CourseDetailsNavEvent {
    data class NavigateToLesson(val courseId: String, val lessonId: String) : CourseDetailsNavEvent()
    data object NavigateBack : CourseDetailsNavEvent()
}
