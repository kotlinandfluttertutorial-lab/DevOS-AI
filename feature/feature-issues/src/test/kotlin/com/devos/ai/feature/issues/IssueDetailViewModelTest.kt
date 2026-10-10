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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [IssueDetailViewModel].
 *
 * DEVOS-043 / DA-56
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IssueDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(issueId: String = "issue-45"): IssueDetailViewModel {
        val handle = SavedStateHandle(
            mapOf("projectId" to "project-1", "issueId" to issueId),
        )
        val vm = IssueDetailViewModel(handle)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    // ── Initial state ────────────────────────────────────────────────────────────

    @Test
    fun `issue loads successfully from stub data`() = runTest {
        val viewModel = IssueDetailViewModel(
            SavedStateHandle(mapOf("projectId" to "p1", "issueId" to "issue-45")),
        )
        viewModel.uiState.test {
            val first = awaitItem()
            if (first is IssueDetailUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                val second = awaitItem()
                assertTrue(second is IssueDetailUiState.Success, "Expected Success but got $second")
            } else {
                assertTrue(first is IssueDetailUiState.Success, "Expected Success but got $first")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `issue 45 loads with correct title`() = runTest {
        val viewModel = createViewModel(issueId = "issue-45")
        val state = viewModel.uiState.value as IssueDetailUiState.Success
        assertEquals(45, state.issue.number)
        assertTrue(state.issue.title.contains("memory leak"))
    }

    @Test
    fun `issue 45 has related code refs`() = runTest {
        val viewModel = createViewModel(issueId = "issue-45")
        val state = viewModel.uiState.value as IssueDetailUiState.Success
        assertTrue(state.relatedCodeRefs.isNotEmpty(), "Issue 45 should have related code refs")
        assertTrue(state.relatedCodeRefs.any { it.filePath == "DevOSNavGraph.kt" })
    }

    @Test
    fun `AI summary is present and non-empty`() = runTest {
        val viewModel = createViewModel(issueId = "issue-45")
        val state = viewModel.uiState.value as IssueDetailUiState.Success
        assertNotNull(state.aiSummary)
        assertTrue(state.aiSummary!!.isNotBlank())
    }

    @Test
    fun `unknown issue id transitions to Error`() = runTest {
        val viewModel = createViewModel(issueId = "unknown-id")
        val state = viewModel.uiState.value
        assertTrue(state is IssueDetailUiState.Error, "Unknown issue should produce Error state")
    }

    // ── Close confirmation ───────────────────────────────────────────────────────

    @Test
    fun `requestCloseIssue shows confirmation dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.requestCloseIssue()

        val state = viewModel.uiState.value as IssueDetailUiState.Success
        assertTrue(state.showCloseConfirmation, "Close confirmation should be shown")
    }

    @Test
    fun `dismissCloseConfirmation hides dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.requestCloseIssue()
        viewModel.dismissCloseConfirmation()

        val state = viewModel.uiState.value as IssueDetailUiState.Success
        assertFalse(state.showCloseConfirmation, "Close confirmation should be dismissed")
    }

    @Test
    fun `confirmCloseIssue marks issue as CLOSED`() = runTest {
        val viewModel = createViewModel()
        val before = viewModel.uiState.value as IssueDetailUiState.Success
        assertEquals(IssueState.OPEN, before.issue.state)

        viewModel.requestCloseIssue()
        viewModel.confirmCloseIssue()

        val after = viewModel.uiState.value as IssueDetailUiState.Success
        assertEquals(IssueState.CLOSED, after.issue.state, "Issue should be CLOSED after confirmation")
        assertFalse(after.showCloseConfirmation, "Dialog should be dismissed after confirmation")
    }

    // ── Navigation events ────────────────────────────────────────────────────────

    @Test
    fun `navigateBack emits NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.navigateBack()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is IssueDetailNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCodeRefClick emits NavigateToCodeViewer event`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as IssueDetailUiState.Success
        val ref = state.relatedCodeRefs.first()

        viewModel.navEvent.test {
            viewModel.onCodeRefClick(ref)
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is IssueDetailNavEvent.NavigateToCodeViewer)
            val navEvent = event as IssueDetailNavEvent.NavigateToCodeViewer
            assertEquals(ref.filePath, navEvent.path)
            assertEquals(ref.lineNumber, navEvent.line)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAIFixSuggestion emits NavigateToAIChat event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onAIFixSuggestion()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is IssueDetailNavEvent.NavigateToAIChat)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
