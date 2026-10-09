package com.devos.ai.feature.learning.lesson

import com.devos.ai.feature.learning.model.LessonContent

/**
 * UI state for the Lesson screen (DEVOS-052, FIGMA-30).
 */
sealed interface LessonUiState {

    data object Loading : LessonUiState

    data class Success(
        val lesson: LessonContent,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
    ) : LessonUiState

    data object Empty : LessonUiState

    data class Error(val message: String, val retryable: Boolean) : LessonUiState
}

/** One-shot navigation events emitted by [LessonViewModel]. */
sealed class LessonNavEvent {
    data class NavigateToNextLesson(val courseId: String, val lessonId: String) : LessonNavEvent()
    data class NavigateToPreviousLesson(val courseId: String, val lessonId: String) : LessonNavEvent()
    data class NavigateToCodeViewer(val code: String, val language: String) : LessonNavEvent()
    data object NavigateBack : LessonNavEvent()
}
