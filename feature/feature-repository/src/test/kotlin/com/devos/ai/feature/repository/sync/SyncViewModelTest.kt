package com.devos.ai.feature.repository.sync

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.devos.ai.feature.repository.model.StepState
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
 * Unit tests for [SyncViewModel].
 *
 * Uses [UnconfinedTestDispatcher] so coroutines run eagerly.
 * Turbine is used for SharedFlow assertions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SyncViewModel()

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Syncing`() = runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(SyncUiState.Syncing::class)
    }

    @Test
    fun `initial state has 5 steps`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SyncUiState.Syncing
        assertThat(state.steps.size).isEqualTo(5)
    }

    @Test
    fun `initial state currentStepIndex is 1`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SyncUiState.Syncing
        assertThat(state.currentStepIndex).isEqualTo(1)
    }

    @Test
    fun `initial state overallProgress is 0_29`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SyncUiState.Syncing
        assertThat(state.overallProgress).isEqualTo(0.29f)
    }

    @Test
    fun `step 0 is COMPLETE`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SyncUiState.Syncing
        assertThat(state.steps[0].state).isEqualTo(StepState.COMPLETE)
    }

    @Test
    fun `step 1 is IN_PROGRESS`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SyncUiState.Syncing
        assertThat(state.steps[1].state).isEqualTo(StepState.IN_PROGRESS)
    }

    @Test
    fun `steps 2 to 4 are PENDING`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SyncUiState.Syncing
        for (i in 2..4) {
            assertThat(state.steps[i].state).isEqualTo(StepState.PENDING)
        }
    }

    @Test
    fun `first step name is Fetch remote`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SyncUiState.Syncing
        assertThat(state.steps[0].name).isEqualTo("Fetch remote")
    }

    @Test
    fun `second step name is Index files`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value as SyncUiState.Syncing
        assertThat(state.steps[1].name).isEqualTo("Index files")
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Test
    fun `cancel emits NavigateBack nav event`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.cancel()
            assertThat(awaitItem()).isEqualTo(SyncNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Navigate back (top-bar back arrow) ─────────────────────────────────────

    @Test
    fun `navigateBack emits NavigateBack nav event`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.navigateBack()
            assertThat(awaitItem()).isEqualTo(SyncNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
