package com.devos.ai.feature.learning.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.learning.model.Quiz
import com.devos.ai.feature.learning.model.QuizQuestion
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
 * ViewModel for the Quiz screen (DEVOS-053).
 *
 * Manages quiz progression entirely in-ViewModel:
 * Active → Reviewing → Active (next) → ... → Complete
 *
 * TODO(DEVOS-054): replace stub data with GetQuizUseCase; save final score via SaveQuizScoreUseCase.
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val courseId: String = checkNotNull(savedStateHandle["courseId"])
    private val quizId: String = checkNotNull(savedStateHandle["quizId"])

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<QuizNavEvent>()
    val navEvent: SharedFlow<QuizNavEvent> = _navEvent.asSharedFlow()

    /** In-memory quiz data. */
    private var quiz: Quiz? = null
    private var currentIndex: Int = 0
    private var correctCount: Int = 0

    init {
        loadQuiz()
    }

    fun loadQuiz() {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            try {
                quiz = stubQuiz(courseId, quizId)
                currentIndex = 0
                correctCount = 0
                emitActiveState(selectedOption = null)
            } catch (e: Exception) {
                _uiState.value = QuizUiState.Error(
                    message = e.message ?: "Failed to load quiz",
                    retryable = true,
                )
            }
        }
    }

    /** User tapped an option; transitions Active → Active (with selection). */
    fun selectOption(optionIndex: Int) {
        val currentState = _uiState.value as? QuizUiState.Success.Active ?: return
        _uiState.value = currentState.copy(selectedOption = optionIndex)
    }

    /** User pressed Submit; transitions Active → Reviewing. */
    fun submitAnswer() {
        val currentState = _uiState.value as? QuizUiState.Success.Active ?: return
        val selected = currentState.selectedOption ?: return
        if (selected == currentState.question.correctIndex) {
            correctCount++
        }
        _uiState.value = QuizUiState.Success.Reviewing(
            quizTitle = currentState.quizTitle,
            questionIndex = currentState.questionIndex,
            totalQuestions = currentState.totalQuestions,
            question = currentState.question,
            selectedOption = selected,
        )
    }

    /** User pressed Next Question from Reviewing state. */
    fun nextQuestion() {
        val q = quiz ?: return
        currentIndex++
        if (currentIndex >= q.questions.size) {
            // Quiz complete
            _uiState.value = QuizUiState.Success.Complete(
                quizTitle = q.title,
                score = correctCount,
                totalQuestions = q.questions.size,
            )
            // TODO(DEVOS-054): persist score via SaveQuizScoreUseCase
        } else {
            emitActiveState(selectedOption = null)
        }
    }

    fun navigateBack() = emit(QuizNavEvent.NavigateBack)

    fun finishQuiz() = emit(QuizNavEvent.NavigateToCourse(courseId))

    private fun emitActiveState(selectedOption: Int?) {
        val q = quiz ?: return
        val question = q.questions.getOrNull(currentIndex) ?: return
        _uiState.value = QuizUiState.Success.Active(
            quizTitle = q.title,
            questionIndex = currentIndex,
            totalQuestions = q.questions.size,
            question = question,
            selectedOption = selectedOption,
        )
    }

    private fun emit(event: QuizNavEvent) {
        viewModelScope.launch { _navEvent.emit(event) }
    }

    // ── Stub data ──────────────────────────────────────────────────────────────

    private fun stubQuiz(courseId: String, quizId: String) = Quiz(
        id = quizId,
        courseId = courseId,
        title = "Flow Operators Quiz",
        questions = listOf(
            QuizQuestion(
                id = "q1",
                text = "Which Flow operator transforms each emitted value to a new value?",
                options = listOf("filter", "map", "combine", "collect"),
                correctIndex = 1,
                explanation = "`map` transforms each emitted value using a transform lambda, similar to List.map but lazy and asynchronous.",
            ),
            QuizQuestion(
                id = "q2",
                text = "What type of operator is `collect`?",
                options = listOf("Intermediate operator", "Terminal operator", "Cold operator", "Hot operator"),
                correctIndex = 1,
                explanation = "`collect` is a terminal operator — it triggers collection of the flow. Without a terminal operator, a cold flow will not emit any values.",
            ),
            QuizQuestion(
                id = "q3",
                text = "Which operator removes values from a flow that do not satisfy a predicate?",
                options = listOf("map", "take", "filter", "drop"),
                correctIndex = 2,
                explanation = "`filter` passes only those values for which the predicate returns true, dropping the rest.",
            ),
        ),
    )
}
