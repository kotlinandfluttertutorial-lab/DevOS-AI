package com.devos.ai.feature.learning.quiz

import com.devos.ai.feature.learning.model.QuizQuestion

/**
 * UI state for the Quiz screen (DEVOS-053, FIGMA-31).
 *
 * Three sub-states within Success:
 * - Active: user is selecting an option
 * - Reviewing: user submitted, showing correct/incorrect + explanation
 * - Complete: all questions answered, showing final score
 */
sealed interface QuizUiState {

    data object Loading : QuizUiState

    sealed interface Success : QuizUiState {

        data class Active(
            val quizTitle: String,
            val questionIndex: Int,
            val totalQuestions: Int,
            val question: QuizQuestion,
            /** Index of currently selected option, null if none. */
            val selectedOption: Int?,
        ) : Success

        data class Reviewing(
            val quizTitle: String,
            val questionIndex: Int,
            val totalQuestions: Int,
            val question: QuizQuestion,
            val selectedOption: Int,
        ) : Success

        data class Complete(
            val quizTitle: String,
            val score: Int,
            val totalQuestions: Int,
        ) : Success
    }

    data object Empty : QuizUiState

    data class Error(val message: String, val retryable: Boolean) : QuizUiState
}

/** State of a quiz answer option. */
enum class QuizOptionState { Default, Selected, Correct, Incorrect }

/** One-shot navigation events emitted by [QuizViewModel]. */
sealed class QuizNavEvent {
    data object NavigateBack : QuizNavEvent()
    data class NavigateToCourse(val courseId: String) : QuizNavEvent()
}
