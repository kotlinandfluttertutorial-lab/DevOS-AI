package com.devos.ai.feature.chat.evidence

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import com.devos.ai.feature.chat.answer.stubAnswerDetail
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
 * Unit tests for [SourceEvidenceViewModel].
 *
 * DEVOS-030 / DA-43
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SourceEvidenceViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(answerId: String = "answer-stub-001"): SourceEvidenceViewModel =
        SourceEvidenceViewModel(
            savedStateHandle = SavedStateHandle(mapOf("answerId" to answerId)),
        )

    // ── Initial load ──────────────────────────────────────────────────────────

    @Test
    fun `init loads stub sources into Success state`() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value).isInstanceOf(SourceEvidenceUiState.Success::class)
    }

    @Test
    fun `loaded sources list is non-empty`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as SourceEvidenceUiState.Success
        assertThat(state.sources.size).isGreaterThan(0)
    }

    @Test
    fun `loaded sources match stub answer sources`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as SourceEvidenceUiState.Success
        assertThat(state.sources.size).isEqualTo(stubAnswerDetail.sources.size)
    }

    @Test
    fun `loaded question summary is non-empty`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as SourceEvidenceUiState.Success
        assertThat(state.questionSummary).isEqualTo(stubAnswerDetail.question)
    }

    // ── onOpenInCodeViewer ────────────────────────────────────────────────────

    @Test
    fun `onOpenInCodeViewer emits NavigateToCode with correct filePath and line`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onOpenInCodeViewer("feature/AIChatViewModel.kt", 8)
            val event = awaitItem() as EvidenceNavEvent.NavigateToCode
            assertThat(event.filePath).isEqualTo("feature/AIChatViewModel.kt")
            assertThat(event.line).isEqualTo(8)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onOpenInCodeViewer emits NavigateToCode instance`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onOpenInCodeViewer("domain/AIRepository.kt", 1)
            assertThat(awaitItem()).isInstanceOf(EvidenceNavEvent.NavigateToCode::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onNavigateBack ────────────────────────────────────────────────────────

    @Test
    fun `onNavigateBack emits NavigateBack`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onNavigateBack()
            assertThat(awaitItem()).isEqualTo(EvidenceNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── retry ─────────────────────────────────────────────────────────────────

    @Test
    fun `retry reloads sources into Success state`() = runTest {
        val viewModel = createViewModel()

        viewModel.retry()

        assertThat(viewModel.uiState.value).isInstanceOf(SourceEvidenceUiState.Success::class)
    }
}
