package com.devos.ai.feature.memory

import app.cash.turbine.test
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
 * Unit tests for [DeveloperMemoryViewModel].
 *
 * DEVOS-055 / DA-68
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperMemoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DeveloperMemoryViewModel {
        val vm = DeveloperMemoryViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    // ── Initial state ────────────────────────────────────────────────────────────

    @Test
    fun `initial state transitions to Success with stub entries`() = runTest {
        val viewModel = DeveloperMemoryViewModel()
        viewModel.uiState.test {
            val first = awaitItem()
            if (first is DeveloperMemoryUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                val second = awaitItem()
                assertTrue(second is DeveloperMemoryUiState.Success, "Expected Success but got $second")
            } else {
                assertTrue(first is DeveloperMemoryUiState.Success, "Expected Success but got $first")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stub entries contain 2 code preferences and 2 recent decisions`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        assertEquals(2, state.data.codePreferences.size)
        assertEquals(2, state.data.recentDecisions.size)
    }

    @Test
    fun `code preferences entries are dismissible`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        assertTrue(state.data.codePreferences.all { it.isDismissible })
    }

    @Test
    fun `recent decisions entries are not dismissible`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        assertFalse(state.data.recentDecisions.any { it.isDismissible })
    }

    // ── dismissEntry ─────────────────────────────────────────────────────────────

    @Test
    fun `dismissEntry removes entry from codePreferences`() = runTest {
        val viewModel = createViewModel()
        val initialState = viewModel.uiState.value as DeveloperMemoryUiState.Success
        val idToRemove = initialState.data.codePreferences.first().id

        viewModel.dismissEntry(idToRemove)

        val newState = viewModel.uiState.value as DeveloperMemoryUiState.Success
        assertFalse(newState.data.codePreferences.any { it.id == idToRemove })
        assertEquals(1, newState.data.codePreferences.size)
    }

    @Test
    fun `dismissEntry on last two items transitions to Empty`() = runTest {
        val viewModel = createViewModel()

        // Dismiss all code preferences (2) — recent decisions don't have dismiss
        var state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        state.data.codePreferences.forEach { viewModel.dismissEntry(it.id) }

        // Recent decisions also non-dismissible but let's directly dismiss via id (allowed)
        state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        state.data.recentDecisions.forEach { viewModel.dismissEntry(it.id) }

        // All entries gone → Empty state
        assertTrue(viewModel.uiState.value is DeveloperMemoryUiState.Empty)
    }

    // ── clearAll ─────────────────────────────────────────────────────────────────

    @Test
    fun `showClearAllDialog sets showClearDialog to true`() = runTest {
        val viewModel = createViewModel()
        viewModel.showClearAllDialog()
        val state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        assertTrue(state.showClearDialog)
    }

    @Test
    fun `cancelClearAll sets showClearDialog to false`() = runTest {
        val viewModel = createViewModel()
        viewModel.showClearAllDialog()
        viewModel.cancelClearAll()
        val state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        assertFalse(state.showClearDialog)
    }

    @Test
    fun `confirmClearAll clears all entries and transitions to Empty`() = runTest {
        val viewModel = createViewModel()
        viewModel.showClearAllDialog()
        viewModel.confirmClearAll()
        assertTrue(viewModel.uiState.value is DeveloperMemoryUiState.Empty)
    }

    // ── query filtering ──────────────────────────────────────────────────────────

    @Test
    fun `onQueryChange filters entries by title`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("coroutines")
        val state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        assertEquals(1, state.data.codePreferences.size)
        assertTrue(state.data.codePreferences.first().title.contains("coroutines", ignoreCase = true))
        assertEquals(0, state.data.recentDecisions.size)
    }

    @Test
    fun `onQueryChange with empty string shows all entries`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("coroutines")
        viewModel.onQueryChange("")
        val state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        assertEquals(2, state.data.codePreferences.size)
        assertEquals(2, state.data.recentDecisions.size)
    }

    @Test
    fun `onQueryChange with no-match query shows empty sections`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("zzznomatch")
        // With query set but no results, the state remains Success with empty sections
        // (not Empty, because a query is active)
        val state = viewModel.uiState.value as DeveloperMemoryUiState.Success
        assertEquals(0, state.data.codePreferences.size)
        assertEquals(0, state.data.recentDecisions.size)
        assertEquals("zzznomatch", state.query)
    }
}
