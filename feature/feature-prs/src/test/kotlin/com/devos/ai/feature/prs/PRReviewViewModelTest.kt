package com.devos.ai.feature.prs

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [PRReviewViewModel].
 *
 * DEVOS-045 / DA-57
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PRReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(prId: String = "pr-47"): PRReviewViewModel {
        val handle = SavedStateHandle(mapOf("prId" to prId, "projectId" to "project-1"))
        val vm = PRReviewViewModel(handle)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state transitions to Success with stub review`() = runTest {
        val viewModel = PRReviewViewModel(SavedStateHandle(mapOf("prId" to "pr-47", "projectId" to "p1")))
        viewModel.uiState.test {
            val first = awaitItem()
            if (first is PRReviewUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                val second = awaitItem()
                assertTrue(second is PRReviewUiState.Success, "Expected Success but got $second")
            } else {
                assertTrue(first is PRReviewUiState.Success, "Expected Success but got $first")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial success state has showPostConfirmation false`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as PRReviewUiState.Success
        assertFalse(state.showPostConfirmation, "Dialog should not be shown initially")
    }

    @Test
    fun `stub review has correct AI score 94`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as PRReviewUiState.Success
        assertEquals(94, state.review.aiScore)
    }

    @Test
    fun `stub review has APPROVE verdict`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as PRReviewUiState.Success
        assertEquals(PRVerdict.APPROVE, state.review.verdict)
    }

    @Test
    fun `stub review has 5 summary points`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as PRReviewUiState.Success
        assertEquals(5, state.review.summaryPoints.size)
    }

    @Test
    fun `stub review has 8 file reviews`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as PRReviewUiState.Success
        assertEquals(8, state.review.fileReviews.size)
    }

    // ── Post confirmation dialog ───────────────────────────────────────────────

    @Test
    fun `onPostReview sets showPostConfirmation to true`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPostReview()

        val state = viewModel.uiState.value as PRReviewUiState.Success
        assertTrue(state.showPostConfirmation, "Dialog should be shown after onPostReview")
    }

    @Test
    fun `dismissPost clears showPostConfirmation`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPostReview()
        viewModel.dismissPost()

        val state = viewModel.uiState.value as PRReviewUiState.Success
        assertFalse(state.showPostConfirmation, "Dialog should be dismissed after dismissPost")
    }

    @Test
    fun `confirmPost clears showPostConfirmation and emits PostToGitHub`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPostReview()

        viewModel.navEvent.test {
            viewModel.confirmPost()
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is PRReviewNavEvent.PostToGitHub, "Expected PostToGitHub but got $event")

            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.uiState.value as PRReviewUiState.Success
        assertFalse(state.showPostConfirmation, "Dialog should be cleared after confirmPost")
    }

    @Test
    fun `dismissPost does not emit PostToGitHub event`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPostReview()
        viewModel.dismissPost()

        // Verify no event was emitted
        viewModel.navEvent.test {
            // No events should be pending
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Navigation events ──────────────────────────────────────────────────────

    @Test
    fun `navigateBack emits NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.navigateBack()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is PRReviewNavEvent.NavigateBack, "Expected NavigateBack but got $event")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCopyReview emits CopyReview event with non-empty review text`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onCopyReview()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is PRReviewNavEvent.CopyReview, "Expected CopyReview but got $event")
            val copyEvent = event as PRReviewNavEvent.CopyReview
            assertTrue(copyEvent.reviewText.isNotEmpty(), "Review text should not be empty")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
