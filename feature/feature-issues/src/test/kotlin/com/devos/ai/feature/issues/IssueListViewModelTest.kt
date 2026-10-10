package com.devos.ai.feature.issues

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
 * Unit tests for [IssueListViewModel].
 *
 * DEVOS-042 / DA-54
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IssueListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(projectId: String = "project-1"): IssueListViewModel {
        val handle = SavedStateHandle(mapOf("projectId" to projectId))
        val vm = IssueListViewModel(handle)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    // ── Initial state ────────────────────────────────────────────────────────────

    @Test
    fun `initial state transitions to Success with stub issues`() = runTest {
        val viewModel = IssueListViewModel(SavedStateHandle(mapOf("projectId" to "p1")))
        viewModel.uiState.test {
            val first = awaitItem()
            if (first is IssueListUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                val second = awaitItem()
                assertTrue(second is IssueListUiState.Success, "Expected Success but got $second")
            } else {
                assertTrue(first is IssueListUiState.Success, "Expected Success but got $first")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stub data has 4 issues`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as IssueListUiState.Success
        assertEquals(4, state.issues.size)
    }

    @Test
    fun `default filter shows open issues only`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as IssueListUiState.Success
        assertTrue(state.filter.showOpen)
        assertTrue(state.issues.all { it.state == IssueState.OPEN })
    }

    // ── Filter chips ─────────────────────────────────────────────────────────────

    @Test
    fun `switching to closed filter shows no issues (all stubs are open)`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(IssueFilter(showOpen = false))

        // All stubs are OPEN so closed filter → empty
        assertTrue(
            viewModel.uiState.value is IssueListUiState.Empty,
            "Expected Empty when filtering closed issues on all-open stub data",
        )
    }

    @Test
    fun `bug label filter shows only bug issues`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(IssueFilter(showOpen = true, labelFilter = "bug"))

        val state = viewModel.uiState.value as IssueListUiState.Success
        assertTrue(state.issues.all { it.labels.any { label -> label.lowercase() == "bug" } })
    }

    @Test
    fun `bug filter matches issues 45 and 43`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(IssueFilter(showOpen = true, labelFilter = "bug"))

        val state = viewModel.uiState.value as IssueListUiState.Success
        assertEquals(2, state.issues.size)
        assertTrue(state.issues.any { it.number == 45 })
        assertTrue(state.issues.any { it.number == 43 })
    }

    @Test
    fun `clearing label filter returns all open issues`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(IssueFilter(showOpen = true, labelFilter = "bug"))
        viewModel.onFilterChange(IssueFilter(showOpen = true, labelFilter = null))

        val state = viewModel.uiState.value as IssueListUiState.Success
        assertEquals(4, state.issues.size)
    }

    // ── Close confirmation dialog ────────────────────────────────────────────────

    @Test
    fun `requestCloseIssue sets showCloseConfirmation to true`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as IssueListUiState.Success
        val issueId = state.issues.first().id

        viewModel.requestCloseIssue(issueId)

        val updated = viewModel.uiState.value as IssueListUiState.Success
        assertTrue(updated.showCloseConfirmation, "Dialog should be shown after requestCloseIssue")
        assertEquals(issueId, updated.issueToClose)
    }

    @Test
    fun `dismissCloseConfirmation hides dialog`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as IssueListUiState.Success
        val issueId = state.issues.first().id

        viewModel.requestCloseIssue(issueId)
        viewModel.dismissCloseConfirmation()

        val updated = viewModel.uiState.value as IssueListUiState.Success
        assertFalse(updated.showCloseConfirmation, "Dialog should be dismissed")
    }

    @Test
    fun `confirmCloseIssue marks issue as closed`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as IssueListUiState.Success
        val issueId = state.issues.first().id

        viewModel.requestCloseIssue(issueId)
        viewModel.confirmCloseIssue()

        // After closing, filtered state may be empty or show remaining open issues
        val updated = viewModel.uiState.value
        // The closed issue should no longer appear in the open list
        if (updated is IssueListUiState.Success) {
            assertFalse(updated.issues.any { it.id == issueId && it.state == IssueState.OPEN })
        }
    }

    // ── Navigation events ────────────────────────────────────────────────────────

    @Test
    fun `onIssueClick emits NavigateToDetail event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onIssueClick("issue-45")
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is IssueListNavEvent.NavigateToDetail)
            assertEquals("issue-45", (event as IssueListNavEvent.NavigateToDetail).issueId)
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
            assertTrue(event is IssueListNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
