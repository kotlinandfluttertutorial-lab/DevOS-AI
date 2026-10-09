package com.devos.ai.feature.learning

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isNotNull
import com.devos.ai.feature.learning.quiz.QuizNavEvent
import com.devos.ai.feature.learning.quiz.QuizUiState
import com.devos.ai.feature.learning.quiz.QuizViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [QuizViewModel].
 *
 * Covers:
 * - stub data loading → Active state
 * - selectOption updates state
 * - submitAnswer marks correct/incorrect, transitions to Reviewing
 * - nextQuestion advances to next Active or Complete
 * - final score emitted on last answer
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        courseId: String = "course-1",
        quizId: String = "quiz-1",
    ) = QuizViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf("courseId" to courseId, "quizId" to quizId)
        ),
    )

    // ── Init / loading ─────────────────────────────────────────────────────────

    @Test
    fun `init loads stub quiz and emits Active state`() = runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(QuizUiState.Success.Active::class)
    }

    @Test
    fun `initial Active state has questionIndex 0`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as QuizUiState.Success.Active
        assertThat(state.questionIndex).isEqualTo(0)
    }

    @Test
    fun `initial Active state has totalQuestions 3`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as QuizUiState.Success.Active
        assertThat(state.totalQuestions).isEqualTo(3)
    }

    @Test
    fun `initial Active state has no selected option`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as QuizUiState.Success.Active
        assertThat(state.selectedOption).isNull()
    }

    // ── selectOption ───────────────────────────────────────────────────────────

    @Test
    fun `selectOption updates selectedOption in Active state`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(1)

        val state = viewModel.uiState.value as QuizUiState.Success.Active
        assertThat(state.selectedOption).isEqualTo(1)
    }

    @Test
    fun `selectOption changing from 0 to 2 updates correctly`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(0)
        viewModel.selectOption(2)

        val state = viewModel.uiState.value as QuizUiState.Success.Active
        assertThat(state.selectedOption).isEqualTo(2)
    }

    @Test
    fun `selectOption on Reviewing state does nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectOption(1)
        viewModel.submitAnswer()

        // Now in Reviewing state
        assertThat(viewModel.uiState.value).isInstanceOf(QuizUiState.Success.Reviewing::class)

        // Should not crash or change state
        viewModel.selectOption(0)

        // Still Reviewing
        assertThat(viewModel.uiState.value).isInstanceOf(QuizUiState.Success.Reviewing::class)
    }

    // ── submitAnswer ───────────────────────────────────────────────────────────

    @Test
    fun `submitAnswer with no selection does nothing`() = runTest {
        val viewModel = createViewModel()

        // No selection made
        viewModel.submitAnswer()

        // Still Active, not Reviewing
        assertThat(viewModel.uiState.value).isInstanceOf(QuizUiState.Success.Active::class)
    }

    @Test
    fun `submitAnswer transitions Active to Reviewing`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(1)
        viewModel.submitAnswer()

        assertThat(viewModel.uiState.value).isInstanceOf(QuizUiState.Success.Reviewing::class)
    }

    @Test
    fun `submitAnswer Reviewing state preserves selectedOption`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(2)
        viewModel.submitAnswer()

        val state = viewModel.uiState.value as QuizUiState.Success.Reviewing
        assertThat(state.selectedOption).isEqualTo(2)
    }

    @Test
    fun `submitAnswer Reviewing state has question with explanation`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(1)
        viewModel.submitAnswer()

        val state = viewModel.uiState.value as QuizUiState.Success.Reviewing
        assertThat(state.question.explanation.isNotEmpty()).isEqualTo(true)
    }

    // ── nextQuestion ───────────────────────────────────────────────────────────

    @Test
    fun `nextQuestion from Reviewing state advances to next question`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(1)
        viewModel.submitAnswer()
        viewModel.nextQuestion()

        val state = viewModel.uiState.value as QuizUiState.Success.Active
        assertThat(state.questionIndex).isEqualTo(1)
    }

    @Test
    fun `next question after advance has no selected option`() = runTest {
        val viewModel = createViewModel()

        viewModel.selectOption(0)
        viewModel.submitAnswer()
        viewModel.nextQuestion()

        val state = viewModel.uiState.value as QuizUiState.Success.Active
        assertThat(state.selectedOption).isNull()
    }

    // ── Complete state and scoring ─────────────────────────────────────────────

    @Test
    fun `completing all questions transitions to Complete state`() = runTest {
        val viewModel = createViewModel()

        // Answer all 3 questions
        repeat(3) {
            val active = viewModel.uiState.value as QuizUiState.Success.Active
            viewModel.selectOption(active.question.correctIndex)
            viewModel.submitAnswer()
            viewModel.nextQuestion()
        }

        assertThat(viewModel.uiState.value).isInstanceOf(QuizUiState.Success.Complete::class)
    }

    @Test
    fun `all correct answers yields perfect score`() = runTest {
        val viewModel = createViewModel()

        repeat(3) {
            val active = viewModel.uiState.value as QuizUiState.Success.Active
            viewModel.selectOption(active.question.correctIndex)
            viewModel.submitAnswer()
            viewModel.nextQuestion()
        }

        val complete = viewModel.uiState.value as QuizUiState.Success.Complete
        assertThat(complete.score).isEqualTo(3)
        assertThat(complete.totalQuestions).isEqualTo(3)
    }

    @Test
    fun `all wrong answers yields zero score`() = runTest {
        val viewModel = createViewModel()

        repeat(3) {
            val active = viewModel.uiState.value as QuizUiState.Success.Active
            // Pick a wrong answer (correct is at correctIndex, pick first non-correct)
            val wrongIndex = (0 until active.question.options.size)
                .first { it != active.question.correctIndex }
            viewModel.selectOption(wrongIndex)
            viewModel.submitAnswer()
            viewModel.nextQuestion()
        }

        val complete = viewModel.uiState.value as QuizUiState.Success.Complete
        assertThat(complete.score).isEqualTo(0)
    }

    @Test
    fun `mixed answers yields partial score`() = runTest {
        val viewModel = createViewModel()

        // Q1: correct
        val q1 = viewModel.uiState.value as QuizUiState.Success.Active
        viewModel.selectOption(q1.question.correctIndex)
        viewModel.submitAnswer()
        viewModel.nextQuestion()

        // Q2: wrong
        val q2 = viewModel.uiState.value as QuizUiState.Success.Active
        val wrongIndex = (0 until q2.question.options.size).first { it != q2.question.correctIndex }
        viewModel.selectOption(wrongIndex)
        viewModel.submitAnswer()
        viewModel.nextQuestion()

        // Q3: correct
        val q3 = viewModel.uiState.value as QuizUiState.Success.Active
        viewModel.selectOption(q3.question.correctIndex)
        viewModel.submitAnswer()
        viewModel.nextQuestion()

        val complete = viewModel.uiState.value as QuizUiState.Success.Complete
        assertThat(complete.score).isEqualTo(2)
    }

    @Test
    fun `Complete state has quiz title set`() = runTest {
        val viewModel = createViewModel()

        repeat(3) {
            val active = viewModel.uiState.value as QuizUiState.Success.Active
            viewModel.selectOption(active.question.correctIndex)
            viewModel.submitAnswer()
            viewModel.nextQuestion()
        }

        val complete = viewModel.uiState.value as QuizUiState.Success.Complete
        assertThat(complete.quizTitle.isNotEmpty()).isEqualTo(true)
    }

    // ── Navigation events ──────────────────────────────────────────────────────

    @Test
    fun `navigateBack emits NavigateBack event`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.navigateBack()
            assertThat(awaitItem()).isInstanceOf(QuizNavEvent.NavigateBack::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `finishQuiz emits NavigateToCourse event with courseId`() = runTest {
        val viewModel = createViewModel(courseId = "course-kotlin")

        viewModel.navEvent.test {
            viewModel.finishQuiz()
            val event = awaitItem()
            assertThat(event).isInstanceOf(QuizNavEvent.NavigateToCourse::class)
            assertThat((event as QuizNavEvent.NavigateToCourse).courseId).isEqualTo("course-kotlin")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── loadQuiz retry ─────────────────────────────────────────────────────────

    @Test
    fun `loadQuiz resets quiz state to first question`() = runTest {
        val viewModel = createViewModel()

        // Advance to question 2
        viewModel.selectOption(0)
        viewModel.submitAnswer()
        viewModel.nextQuestion()
        assertThat((viewModel.uiState.value as QuizUiState.Success.Active).questionIndex).isEqualTo(1)

        // Reload
        viewModel.loadQuiz()

        assertThat((viewModel.uiState.value as QuizUiState.Success.Active).questionIndex).isEqualTo(0)
    }
}
