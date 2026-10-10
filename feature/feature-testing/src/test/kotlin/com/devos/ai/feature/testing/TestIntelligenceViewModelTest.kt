package com.devos.ai.feature.testing

import app.cash.turbine.test
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
 * Unit tests for [TestIntelligenceViewModel].
 *
 * DEVOS-048 / DA-61
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestIntelligenceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TestIntelligenceViewModel {
        val vm = TestIntelligenceViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    // ── Initial state ────────────────────────────────────────────────────────────

    @Test
    fun `initial state transitions to Success`() = runTest {
        val viewModel = TestIntelligenceViewModel()
        viewModel.uiState.test {
            val first = awaitItem()
            if (first is TestIntelligenceUiState.Loading) {
                testDispatcher.scheduler.advanceUntilIdle()
                val second = awaitItem()
                assertTrue(second is TestIntelligenceUiState.Success, "Expected Success but got $second")
            } else {
                assertTrue(first is TestIntelligenceUiState.Success, "Expected Success but got $first")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stub coverage data matches mockup values`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as TestIntelligenceUiState.Success
        val coverage = state.coverage

        assertEquals(67, coverage.overallPercent)
        assertEquals(80, coverage.targetPercent)
        assertEquals(234, coverage.passing)
        assertEquals(8, coverage.failing)
        assertEquals(5, coverage.flaky)
    }

    @Test
    fun `stub has 2 uncovered files matching mockup`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as TestIntelligenceUiState.Success
        assertEquals(2, state.coverage.uncoveredFiles.size)
    }

    @Test
    fun `first uncovered file is AuthViewModel with 0 percent coverage`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as TestIntelligenceUiState.Success
        val first = state.coverage.uncoveredFiles.first()
        assertEquals("AuthViewModel.kt", first.name)
        assertEquals(0, first.coveragePercent)
        assertEquals(6, first.untestedFunctions)
    }

    @Test
    fun `second uncovered file is SecurityRepository with 12 percent coverage`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as TestIntelligenceUiState.Success
        val second = state.coverage.uncoveredFiles[1]
        assertEquals("SecurityRepository.kt", second.name)
        assertEquals(12, second.coveragePercent)
        assertEquals(4, second.untestedFunctions)
    }

    @Test
    fun `ai suggestion is non-empty`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as TestIntelligenceUiState.Success
        assertTrue(state.coverage.aiSuggestion.isNotBlank())
    }

    // ── generateTests ────────────────────────────────────────────────────────────

    @Test
    fun `generateTests emits GenerateTests nav event`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.generateTests("AuthViewModel.kt")
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is TestIntelligenceNavEvent.GenerateTests)
            val genEvent = event as TestIntelligenceNavEvent.GenerateTests
            assertEquals("AuthViewModel.kt", genEvent.fileName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── navigateToFile ───────────────────────────────────────────────────────────

    @Test
    fun `navigateToFile emits NavigateToCodeViewer event`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.navigateToFile("AuthViewModel.kt")
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is TestIntelligenceNavEvent.NavigateToCodeViewer)
            val navEvent = event as TestIntelligenceNavEvent.NavigateToCodeViewer
            assertEquals("AuthViewModel.kt", navEvent.filePath)
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
            assertTrue(event is TestIntelligenceNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
