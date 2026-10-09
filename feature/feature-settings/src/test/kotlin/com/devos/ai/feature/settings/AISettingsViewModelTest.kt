package com.devos.ai.feature.settings

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
 * Unit tests for [AISettingsViewModel].
 *
 * DEVOS-033 / DA-46
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AISettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading then transitions to Success with stub settings`() = runTest {
        val viewModel = AISettingsViewModel()
        viewModel.uiState.test {
            // ViewModel starts as Loading; advance dispatcher so the launch{} coroutine runs
            val first = awaitItem()
            if (first is AISettingsUiState.Loading) {
                // consume the Loading emission and wait for Success
                testDispatcher.scheduler.advanceUntilIdle()
                val second = awaitItem()
                assertTrue(second is AISettingsUiState.Success, "Expected Success but was $second")
            } else {
                // dispatcher ran synchronously — already Success
                assertTrue(first is AISettingsUiState.Success, "Expected Success but was $first")
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stub settings load with expected defaults`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AISettingsUiState.Success)
        val settings = (state as AISettingsUiState.Success).settings

        assertEquals("claude-sonnet-4.5", settings.defaultModel)
        assertEquals(10, settings.topKResults)
        assertEquals(512, settings.chunkSize)
        assertEquals(25, settings.agentMaxSteps)
        assertTrue(settings.autoApproveSafeTools)
        assertTrue(settings.memoryEnabled)
        assertEquals(248_420, settings.tokenUsage)
        assertEquals(500_000, settings.tokenLimit)
    }

    @Test
    fun `updateMemoryEnabled toggles memoryEnabled to false`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateMemoryEnabled(false)

        val state = viewModel.uiState.value
        assertTrue(state is AISettingsUiState.Success)
        assertFalse((state as AISettingsUiState.Success).settings.memoryEnabled)
    }

    @Test
    fun `updateMemoryEnabled toggles memoryEnabled back to true`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateMemoryEnabled(false)
        viewModel.updateMemoryEnabled(true)

        val state = viewModel.uiState.value
        assertTrue(state is AISettingsUiState.Success)
        assertTrue((state as AISettingsUiState.Success).settings.memoryEnabled)
    }

    @Test
    fun `updateAutoApproveSafeTools toggles autoApproveSafeTools to false`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAutoApproveSafeTools(false)

        val state = viewModel.uiState.value
        assertTrue(state is AISettingsUiState.Success)
        assertFalse((state as AISettingsUiState.Success).settings.autoApproveSafeTools)
    }

    @Test
    fun `updateAutoApproveSafeTools sets autoApproveSafeTools to true`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // start from false
        viewModel.updateAutoApproveSafeTools(false)
        viewModel.updateAutoApproveSafeTools(true)

        val state = viewModel.uiState.value
        assertTrue(state is AISettingsUiState.Success)
        assertTrue((state as AISettingsUiState.Success).settings.autoApproveSafeTools)
    }

    @Test
    fun `navigateToProviders emits NavigateToProviders nav event`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.navEvent.test {
            viewModel.navigateToProviders()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(AISettingsNavEvent.NavigateToProviders, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `navigateBack emits NavigateBack nav event`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.navEvent.test {
            viewModel.navigateBack()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(AISettingsNavEvent.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `navigateToModelPicker emits NavigateToModelPicker nav event`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.navEvent.test {
            viewModel.navigateToModelPicker()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(AISettingsNavEvent.NavigateToModelPicker, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateTopKResults updates topKResults in state`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTopKResults(20)

        val state = viewModel.uiState.value as AISettingsUiState.Success
        assertEquals(20, state.settings.topKResults)
    }

    @Test
    fun `updateChunkSize updates chunkSize in state`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateChunkSize(1024)

        val state = viewModel.uiState.value as AISettingsUiState.Success
        assertEquals(1024, state.settings.chunkSize)
    }

    @Test
    fun `updateAgentMaxSteps updates agentMaxSteps in state`() = runTest {
        val viewModel = AISettingsViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateAgentMaxSteps(40)

        val state = viewModel.uiState.value as AISettingsUiState.Success
        assertEquals(40, state.settings.agentMaxSteps)
    }
}
