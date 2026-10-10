package com.devos.ai.feature.agents.tool

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.model.AgentRunStatus
import com.devos.ai.domain.ai.model.AgentStep
import com.devos.ai.domain.ai.model.AgentStepStatus
import com.devos.ai.domain.ai.repository.AgentRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Unit tests for [AgentToolDetailViewModel].
 * DEVOS-036 / DA-50
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgentToolDetailViewModelTest {

    private lateinit var agentRepository: AgentRepository

    private fun stubStep(
        stepId: String     = "step-1",
        toolName: String   = "search_code",
        status: AgentStepStatus = AgentStepStatus.COMPLETED,
        durationMs: Long   = 1240L,
        toolOutput: String = """{"results":[],"total":0}""",
    ) = AgentStep(
        id         = stepId,
        index      = 0,
        thought    = "Using $toolName",
        toolName   = toolName,
        toolInput  = mapOf("query" to "StateFlow"),
        toolOutput = toolOutput,
        status     = status,
        durationMs = durationMs,
    )

    private fun stubRun(step: AgentStep) = AgentRun(
        id     = "run-1",
        goal   = "test",
        repoId = null,
        status = AgentRunStatus.COMPLETED,
        steps  = listOf(step),
    )

    private fun makeViewModel(runId: String = "run-1", stepId: String = "step-1") =
        AgentToolDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("runId" to runId, "stepId" to stepId)),
            agentRepository  = agentRepository,
        )

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxed = true)
        agentRepository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Success state ─────────────────────────────────────────────────────────

    @Test
    fun `loads step and emits Success state`() = runTest(UnconfinedTestDispatcher()) {
        val step = stubStep()
        coEvery { agentRepository.getRun("run-1") } returns stubRun(step)

        val vm    = makeViewModel()
        val state = vm.uiState.value

        assertThat(state).isInstanceOf(AgentToolDetailUiState.Success::class)
    }

    @Test
    fun `Success state contains correct toolName`() = runTest(UnconfinedTestDispatcher()) {
        val step = stubStep(toolName = "read_file")
        coEvery { agentRepository.getRun("run-1") } returns stubRun(step)

        val state = makeViewModel().uiState.value as AgentToolDetailUiState.Success
        assertThat(state.toolName).isEqualTo("read_file")
    }

    @Test
    fun `Success state isSuccess true for COMPLETED step`() = runTest(UnconfinedTestDispatcher()) {
        val step = stubStep(status = AgentStepStatus.COMPLETED)
        coEvery { agentRepository.getRun("run-1") } returns stubRun(step)

        val state = makeViewModel().uiState.value as AgentToolDetailUiState.Success
        assertThat(state.isSuccess).isTrue()
    }

    @Test
    fun `Success state contains durationMs`() = runTest(UnconfinedTestDispatcher()) {
        val step = stubStep(durationMs = 2345L)
        coEvery { agentRepository.getRun("run-1") } returns stubRun(step)

        val state = makeViewModel().uiState.value as AgentToolDetailUiState.Success
        assertThat(state.durationMs).isEqualTo(2345L)
    }

    @Test
    fun `Success state inputJson is non-empty`() = runTest(UnconfinedTestDispatcher()) {
        val step = stubStep()
        coEvery { agentRepository.getRun("run-1") } returns stubRun(step)

        val state = makeViewModel().uiState.value as AgentToolDetailUiState.Success
        assertThat(state.inputJson.isNotBlank()).isTrue()
    }

    @Test
    fun `Success state outputJson is non-empty`() = runTest(UnconfinedTestDispatcher()) {
        val step = stubStep()
        coEvery { agentRepository.getRun("run-1") } returns stubRun(step)

        val state = makeViewModel().uiState.value as AgentToolDetailUiState.Success
        assertThat(state.outputJson.isNotBlank()).isTrue()
    }

    // ── Error state ───────────────────────────────────────────────────────────

    @Test
    fun `emits Error when run not found`() = runTest(UnconfinedTestDispatcher()) {
        coEvery { agentRepository.getRun(any()) } returns null
        val state = makeViewModel().uiState.value
        assertThat(state).isInstanceOf(AgentToolDetailUiState.Error::class)
    }

    @Test
    fun `emits Error when stepId not found in run`() = runTest(UnconfinedTestDispatcher()) {
        val run = stubRun(stubStep(stepId = "other-step"))
        coEvery { agentRepository.getRun("run-1") } returns run
        val state = makeViewModel(stepId = "missing-step").uiState.value
        assertThat(state).isInstanceOf(AgentToolDetailUiState.Error::class)
    }

    // ── onToggleRawJson ───────────────────────────────────────────────────────

    @Test
    fun `onToggleRawJson flips showRawJson`() = runTest(UnconfinedTestDispatcher()) {
        val step = stubStep()
        coEvery { agentRepository.getRun("run-1") } returns stubRun(step)

        val vm     = makeViewModel()
        val before = (vm.uiState.value as AgentToolDetailUiState.Success).showRawJson
        vm.onToggleRawJson()
        val after  = (vm.uiState.value as AgentToolDetailUiState.Success).showRawJson
        assertThat(after).isEqualTo(!before)
    }
}
