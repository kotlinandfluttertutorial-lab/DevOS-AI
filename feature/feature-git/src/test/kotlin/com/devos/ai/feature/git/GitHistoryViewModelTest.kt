package com.devos.ai.feature.git

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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [GitHistoryViewModel].
 *
 * DEVOS-040 / DA-53
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GitHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(repoId: String = "repo-1"): GitHistoryViewModel {
        val handle = SavedStateHandle(mapOf("repoId" to repoId))
        val vm = GitHistoryViewModel(handle)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    // ── Initial state ────────────────────────────────────────────────────────────

    @Test
    fun `initial state transitions to Success with stub commits`() = runTest {
        val viewModel = GitHistoryViewModel(SavedStateHandle(mapOf("repoId" to "repo-1")))
        viewModel.uiState.test {
            val first = awaitItem()
            if (first is GitHistoryUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                val second = awaitItem()
                assertTrue(second is GitHistoryUiState.Success, "Expected Success but got $second")
            } else {
                assertTrue(first is GitHistoryUiState.Success, "Expected Success but got $first")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `commits are grouped correctly with TODAY and YESTERDAY`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as GitHistoryUiState.Success
        val groupLabels = state.groups.map { it.dateLabel }
        assertTrue(groupLabels.contains("TODAY"), "Expected TODAY group")
        assertTrue(groupLabels.contains("YESTERDAY"), "Expected YESTERDAY group")
    }

    @Test
    fun `TODAY group contains 2 commits for main branch`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as GitHistoryUiState.Success
        val todayGroup = state.groups.first { it.dateLabel == "TODAY" }
        assertEquals(2, todayGroup.commits.size)
    }

    @Test
    fun `default active branch is main`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as GitHistoryUiState.Success
        assertEquals("main", state.activeBranch)
    }

    @Test
    fun `three branches are available`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as GitHistoryUiState.Success
        assertEquals(3, state.branches.size)
        assertTrue(state.branches.contains("main"))
        assertTrue(state.branches.contains("feature/ai-chat"))
        assertTrue(state.branches.contains("fix/nav-leak"))
    }

    @Test
    fun `AI summary is present`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as GitHistoryUiState.Success
        assertNotNull(state.aiSummary)
    }

    // ── Branch switching ─────────────────────────────────────────────────────────

    @Test
    fun `switching to feature branch updates active branch`() = runTest {
        val viewModel = createViewModel()
        viewModel.onBranchSelected("feature/ai-chat")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as GitHistoryUiState.Success
        assertEquals("feature/ai-chat", state.activeBranch)
    }

    @Test
    fun `switching to feature branch returns fewer commits`() = runTest {
        val viewModel = createViewModel()
        val mainState = viewModel.uiState.value as GitHistoryUiState.Success
        val mainCount = mainState.groups.sumOf { it.commits.size }

        viewModel.onBranchSelected("feature/ai-chat")
        testDispatcher.scheduler.advanceUntilIdle()

        val featureState = viewModel.uiState.value as GitHistoryUiState.Success
        val featureCount = featureState.groups.sumOf { it.commits.size }
        assertTrue(featureCount < mainCount, "Feature branch should have fewer commits than main")
    }

    @Test
    fun `selecting already active branch does not reload`() = runTest {
        val viewModel = createViewModel()
        val stateBefore = viewModel.uiState.value as GitHistoryUiState.Success

        viewModel.onBranchSelected("main") // Same branch

        val stateAfter = viewModel.uiState.value as GitHistoryUiState.Success
        assertEquals(stateBefore.activeBranch, stateAfter.activeBranch)
    }

    // ── Navigation events ────────────────────────────────────────────────────────

    @Test
    fun `navigateBack emits NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.navigateBack()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is GitNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCommitClick emits NavigateToCommitDetail with correct sha`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onCommitClick("a4f2c3d1e5b6")
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is GitNavEvent.NavigateToCommitDetail)
            assertEquals("a4f2c3d1e5b6", (event as GitNavEvent.NavigateToCommitDetail).sha)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
