package com.devos.ai.feature.chat.answer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
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
 * Unit tests for [AnswerDetailViewModel].
 *
 * DEVOS-029 / DA-40
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnswerDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(answerId: String = "answer-stub-001"): AnswerDetailViewModel =
        AnswerDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("answerId" to answerId)),
        )

    // ── Initial load ──────────────────────────────────────────────────────────

    @Test
    fun `init loads stub answer into Success state`() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value).isInstanceOf(AnswerDetailUiState.Success::class)
    }

    @Test
    fun `loaded answer has correct id`() = runTest {
        val viewModel = createViewModel(answerId = "my-answer-id")

        val state = viewModel.uiState.value as AnswerDetailUiState.Success
        assertThat(state.answer.id).isEqualTo("my-answer-id")
    }

    @Test
    fun `loaded answer has non-empty question`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as AnswerDetailUiState.Success
        assertThat(state.answer.question).isEqualTo(stubAnswerDetail.question)
    }

    @Test
    fun `loaded answer has sources populated`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as AnswerDetailUiState.Success
        assertThat(state.answer.sources.size).isEqualTo(stubAnswerDetail.sources.size)
    }

    // ── onViewSources ─────────────────────────────────────────────────────────

    @Test
    fun `onViewSources emits NavigateToSources with correct answerId`() = runTest {
        val viewModel = createViewModel(answerId = "test-answer-42")

        viewModel.navEvent.test {
            viewModel.onViewSources()
            val event = awaitItem() as AnswerNavEvent.NavigateToSources
            assertThat(event.answerId).isEqualTo("test-answer-42")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onAskFollowUp ─────────────────────────────────────────────────────────

    @Test
    fun `onAskFollowUp emits NavigateToFollowUp`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onAskFollowUp()
            val event = awaitItem()
            assertThat(event).isInstanceOf(AnswerNavEvent.NavigateToFollowUp::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAskFollowUp includes question text in event`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onAskFollowUp()
            val event = awaitItem() as AnswerNavEvent.NavigateToFollowUp
            assertThat(event.question).isEqualTo(stubAnswerDetail.question)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onSourceTap ───────────────────────────────────────────────────────────

    @Test
    fun `onSourceTap emits NavigateToCode with correct filePath and line`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onSourceTap("feature/AIChatViewModel.kt", 15)
            val event = awaitItem() as AnswerNavEvent.NavigateToCode
            assertThat(event.filePath).isEqualTo("feature/AIChatViewModel.kt")
            assertThat(event.line).isEqualTo(15)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onNavigateBack ────────────────────────────────────────────────────────

    @Test
    fun `onNavigateBack emits NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onNavigateBack()
            assertThat(awaitItem()).isEqualTo(AnswerNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── retry ─────────────────────────────────────────────────────────────────

    @Test
    fun `retry reloads answer into Success state`() = runTest {
        val viewModel = createViewModel()

        viewModel.retry()

        assertThat(viewModel.uiState.value).isInstanceOf(AnswerDetailUiState.Success::class)
    }
}
