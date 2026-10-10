package com.devos.ai.feature.agents.run

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.model.AgentRunStatus
import com.devos.ai.domain.ai.usecase.CancelAgentRunUseCase
import com.devos.ai.domain.ai.usecase.RunAgentUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Unit tests for [AgentRunViewModel].
 * DEVOS-035 / DA-48
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgentRunViewModelTest {

    private lateinit var runAgentUseCase: RunAgentUseCase
    private lateinit var cancelAgentRunUseCase: CancelAgentRunUseCase

    private fun makeRun(status: AgentRunStatus = AgentRunStatus.RUNNING) = AgentRun(
        id     = UUID.randomUUID().toString(),
        goal   = "test goal",
        repoId = null,
        status = status,
    )

    private fun makeViewModel(goal: String = "test goal"): AgentRunViewModel {
        val handle = SavedStateHandle(mapOf("goal" to goal))
        return AgentRunViewModel(handle, runAgentUseCase, cancelAgentRunUseCase)
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxed = true)
        runAgentUseCase       = mockk(relaxed = true)
        cancelAgentRunUseCase = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Loading`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { runAgentUseCase(any(), any(), any()) } returns flowOf(null)
        val vm = makeViewModel()
        // After null emission stays Loading
        assertThat(vm.uiState.value).isInstanceOf(AgentRunUiState.Loading::class)
    }

    // ── Running state ─────────────────────────────────────────────────────────

    @Test
    fun `uiState transitions to Running when agent emits RUNNING status`() = runTest(UnconfinedTestDispatcher()) {
        val run = makeRun(AgentRunStatus.RUNNING)
        coEvery { runAgentUseCase(any(), any(), any()) } returns flowOf(run)

        val vm = makeViewModel()
        assertThat(vm.uiState.value).isInstanceOf(AgentRunUiState.Running::class)
    }

    @Test
    fun `Running state contains the correct goal`() = runTest(UnconfinedTestDispatcher()) {
        val run = makeRun(AgentRunStatus.RUNNING)
        coEvery { runAgentUseCase(any(), any(), any()) } returns flowOf(run)

        val vm    = makeViewModel("my goal")
        val state = vm.uiState.value as AgentRunUiState.Running
        assertThat(state.run.goal).isEqualTo("test goal")
    }

    // ── Completed state ───────────────────────────────────────────────────────

    @Test
    fun `uiState transitions to Completed when agent finishes`() = runTest(UnconfinedTestDispatcher()) {
        val run = makeRun(AgentRunStatus.COMPLETED).copy(finalAnswer = "All done!")
        coEvery { runAgentUseCase(any(), any(), any()) } returns flowOf(run)

        val vm = makeViewModel()
        assertThat(vm.uiState.value).isInstanceOf(AgentRunUiState.Completed::class)
        val state = vm.uiState.value as AgentRunUiState.Completed
        assertThat(state.finalAnswer).isEqualTo("All done!")
    }

    // ── Failed state ──────────────────────────────────────────────────────────

    @Test
    fun `uiState transitions to Error when agent fails`() = runTest(UnconfinedTestDispatcher()) {
        val run = makeRun(AgentRunStatus.FAILED).copy(finalAnswer = "Something went wrong")
        coEvery { runAgentUseCase(any(), any(), any()) } returns flowOf(run)

        val vm = makeViewModel()
        assertThat(vm.uiState.value).isInstanceOf(AgentRunUiState.Error::class)
    }

    // ── Cancelled state ───────────────────────────────────────────────────────

    @Test
    fun `uiState transitions to Cancelled when agent is cancelled`() = runTest(UnconfinedTestDispatcher()) {
        val run = makeRun(AgentRunStatus.CANCELLED)
        coEvery { runAgentUseCase(any(), any(), any()) } returns flowOf(run)

        val vm = makeViewModel()
        assertThat(vm.uiState.value).isInstanceOf(AgentRunUiState.Cancelled::class)
    }

    // ── onCancelRun ───────────────────────────────────────────────────────────

    @Test
    fun `onCancelRun calls CancelAgentRunUseCase`() = runTest(UnconfinedTestDispatcher()) {
        val run = makeRun(AgentRunStatus.RUNNING)
        coEvery { runAgentUseCase(any(), any(), any()) } returns flowOf(run)

        val vm = makeViewModel()
        vm.onCancelRun()
        coVerify(atLeast = 1) { cancelAgentRunUseCase(any()) }
    }

    // ── onNavigateBack ────────────────────────────────────────────────────────

    @Test
    fun `onNavigateBack emits NavigateBack event`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { runAgentUseCase(any(), any(), any()) } returns flowOf(null)
        val vm = makeViewModel()

        vm.navEvent.test {
            vm.onNavigateBack()
            assertThat(awaitItem()).isEqualTo(AgentRunNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onStepTap ─────────────────────────────────────────────────────────────

    @Test
    fun `onStepTap emits NavigateToToolDetail with stepId`() = runTest(UnconfinedTestDispatcher()) {
        val run = makeRun(AgentRunStatus.RUNNING)
        coEvery { runAgentUseCase(any(), any(), any()) } returns flowOf(run)
        val vm = makeViewModel()

        vm.navEvent.test {
            vm.onStepTap("step-abc")
            val event = awaitItem() as AgentRunNavEvent.NavigateToToolDetail
            assertThat(event.stepId).isEqualTo("step-abc")
            assertThat(event.runId).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
