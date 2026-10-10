package com.devos.ai.feature.security

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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SecurityFindingsViewModel].
 *
 * DEVOS-046 / DA-59
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecurityFindingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SecurityFindingsViewModel {
        val vm = SecurityFindingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    // ── Initial state ────────────────────────────────────────────────────────────

    @Test
    fun `initial state transitions to Success with stub findings`() = runTest {
        val viewModel = SecurityFindingsViewModel()
        viewModel.uiState.test {
            val first = awaitItem()
            if (first is SecurityFindingsUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                val second = awaitItem()
                assertTrue(second is SecurityFindingsUiState.Success, "Expected Success but got $second")
            } else {
                assertTrue(first is SecurityFindingsUiState.Success, "Expected Success but got $first")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stub findings contain 3 items matching mockup`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertEquals(3, state.findings.size)
    }

    @Test
    fun `stub findings include 2 CRITICAL and 1 HIGH`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertEquals(2, state.findings.count { it.severity == Severity.CRITICAL })
        assertEquals(1, state.findings.count { it.severity == Severity.HIGH })
    }

    @Test
    fun `initial filter is null (All)`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertNull(state.activeFilter)
    }

    @Test
    fun `counts map has correct values`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertEquals(2, state.counts[Severity.CRITICAL])
        assertEquals(1, state.counts[Severity.HIGH])
        assertEquals(0, state.counts[Severity.MEDIUM])
        assertEquals(0, state.counts[Severity.LOW])
    }

    // ── onFilterChange ───────────────────────────────────────────────────────────

    @Test
    fun `onFilterChange to CRITICAL filters list to CRITICAL findings only`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(Severity.CRITICAL)
        val state = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertEquals(Severity.CRITICAL, state.activeFilter)
        assertTrue(state.findings.all { it.severity == Severity.CRITICAL })
        assertEquals(2, state.findings.size)
    }

    @Test
    fun `onFilterChange to HIGH filters list to HIGH findings only`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(Severity.HIGH)
        val state = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertEquals(Severity.HIGH, state.activeFilter)
        assertTrue(state.findings.all { it.severity == Severity.HIGH })
        assertEquals(1, state.findings.size)
    }

    @Test
    fun `onFilterChange to null shows all findings`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(Severity.CRITICAL)
        viewModel.onFilterChange(null)
        val state = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertNull(state.activeFilter)
        assertEquals(3, state.findings.size)
    }

    @Test
    fun `onFilterChange to MEDIUM shows 0 findings (none in stub)`() = runTest {
        val viewModel = createViewModel()
        viewModel.onFilterChange(Severity.MEDIUM)
        val state = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertEquals(0, state.findings.size)
        assertEquals(Severity.MEDIUM, state.activeFilter)
    }

    // ── markFixed ────────────────────────────────────────────────────────────────

    @Test
    fun `markFixed removes finding from the list`() = runTest {
        val viewModel = createViewModel()
        val initialState = viewModel.uiState.value as SecurityFindingsUiState.Success
        val idToFix = initialState.findings.first().id

        viewModel.markFixed(idToFix)

        val newState = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertFalse(newState.findings.any { it.id == idToFix })
        assertEquals(2, newState.findings.size)
    }

    @Test
    fun `markFixed updates counts map`() = runTest {
        val viewModel = createViewModel()
        val initialState = viewModel.uiState.value as SecurityFindingsUiState.Success
        val criticalId = initialState.findings.first { it.severity == Severity.CRITICAL }.id

        viewModel.markFixed(criticalId)

        val newState = viewModel.uiState.value as SecurityFindingsUiState.Success
        assertEquals(1, newState.counts[Severity.CRITICAL])
    }

    @Test
    fun `markFixed all findings transitions to Empty`() = runTest {
        val viewModel = createViewModel()
        var state = viewModel.uiState.value as SecurityFindingsUiState.Success
        val ids = state.findings.map { it.id }
        ids.forEach { viewModel.markFixed(it) }

        assertTrue(viewModel.uiState.value is SecurityFindingsUiState.Empty)
    }

    // ── onAskAI ──────────────────────────────────────────────────────────────────

    @Test
    fun `onAskAI emits NavigateToAIChat event with finding context`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SecurityFindingsUiState.Success
        val finding = state.findings.first()

        viewModel.navEvent.test {
            viewModel.onAskAI(finding.id)
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is SecurityNavEvent.NavigateToAIChat)
            val chatEvent = event as SecurityNavEvent.NavigateToAIChat
            assertTrue(chatEvent.context.contains(finding.title))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── navigateBack ─────────────────────────────────────────────────────────────

    @Test
    fun `navigateBack emits NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.navigateBack()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is SecurityNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
