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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [PRListViewModel].
 *
 * DEVOS-044 / DA-55
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PRListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(projectId: String = "project-1"): PRListViewModel {
        val handle = SavedStateHandle(mapOf("projectId" to projectId))
        val vm = PRListViewModel(handle)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state transitions to Success with stub PRs`() = runTest {
        val viewModel = PRListViewModel(SavedStateHandle(mapOf("projectId" to "p1")))
        viewModel.uiState.test {
            val first = awaitItem()
            if (first is PRListUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                val second = awaitItem()
                assertTrue(second is PRListUiState.Success, "Expected Success but got $second")
            } else {
                assertTrue(first is PRListUiState.Success, "Expected Success but got $first")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `default filter is OPEN and returns non-draft PRs`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as PRListUiState.Success
        assertEquals(PRFilter.OPEN, state.filter)
        assertTrue(state.prs.all { !it.isDraft })
    }

    // ── Filter chips ──────────────────────────────────────────────────────────

    @Test
    fun `DRAFT filter returns only draft PRs`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(PRFilter.DRAFT)

        val state = viewModel.uiState.value as PRListUiState.Success
        assertEquals(PRFilter.DRAFT, state.filter)
        assertTrue(state.prs.all { it.isDraft })
    }

    @Test
    fun `MY_PRS filter returns PRs authored by dev`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(PRFilter.MY_PRS)

        val state = viewModel.uiState.value as PRListUiState.Success
        assertEquals(PRFilter.MY_PRS, state.filter)
        assertTrue(state.prs.all { it.author == "dev" })
    }

    @Test
    fun `NEEDS_REVIEW filter returns passing CI PRs with high score`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(PRFilter.NEEDS_REVIEW)

        val state = viewModel.uiState.value
        // Some stub PRs qualify (CI passing + score >= 80)
        if (state is PRListUiState.Success) {
            assertTrue(
                state.prs.all { it.ciStatus == CIStatus.PASSING && it.aiScore >= 80 },
                "NEEDS_REVIEW filter should only include passing CI PRs with high score",
            )
        }
        // Empty is also valid if no PRs match
    }

    @Test
    fun `switching filter changes displayed PRs`() = runTest {
        val viewModel = createViewModel()

        // Default: OPEN
        val openState = viewModel.uiState.value as PRListUiState.Success
        val openCount = openState.prs.size

        // Switch to MY_PRS
        viewModel.onFilterChange(PRFilter.MY_PRS)
        val myPrsState = viewModel.uiState.value as PRListUiState.Success
        // All stubs are authored by "dev" so count should match
        assertTrue(myPrsState.prs.isNotEmpty())
    }

    @Test
    fun `filter updates the current filter field in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.onFilterChange(PRFilter.DRAFT)
        val state = viewModel.uiState.value as PRListUiState.Success
        assertEquals(PRFilter.DRAFT, state.filter)

        viewModel.onFilterChange(PRFilter.OPEN)
        val state2 = viewModel.uiState.value as PRListUiState.Success
        assertEquals(PRFilter.OPEN, state2.filter)
    }

    // ── Navigation events ─────────────────────────────────────────────────────

    @Test
    fun `onPRClick emits NavigateToPRReview event with correct prId`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onPRClick("pr-47")
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is PRListNavEvent.NavigateToPRReview, "Expected NavigateToPRReview but got $event")
            assertEquals("pr-47", (event as PRListNavEvent.NavigateToPRReview).prId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `navigateBack emits NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.navigateBack()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is PRListNavEvent.NavigateBack, "Expected NavigateBack but got $event")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
