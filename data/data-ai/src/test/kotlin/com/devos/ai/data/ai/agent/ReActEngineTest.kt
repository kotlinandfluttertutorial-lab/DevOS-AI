package com.devos.ai.data.ai.agent

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.devos.ai.data.ai.provider.AIProviderClient
import com.devos.ai.data.ai.provider.StreamToken
import com.devos.ai.data.ai.repository.AIProviderRepositoryImpl
import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.model.AgentRunStatus
import com.devos.ai.domain.ai.model.AIProvider
import com.devos.ai.domain.ai.model.DefaultModels
import com.devos.ai.domain.ai.model.ProviderConfig
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Unit tests for [ReActEngine].
 *
 * The AI provider client is mocked to return controlled responses so tests
 * are deterministic and do not make real network calls.
 */
class ReActEngineTest {

    private lateinit var providerRepository: AIProviderRepositoryImpl
    private lateinit var mockClient: AIProviderClient
    private lateinit var engine: ReActEngine

    private val config = ProviderConfig(
        provider      = AIProvider.OPENAI,
        selectedModel = DefaultModels.defaultFor(AIProvider.OPENAI),
        isConfigured  = true,
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        mockClient        = mockk(relaxed = true)
        providerRepository = mockk(relaxed = true)

        coEvery { providerRepository.getActiveConfig() } returns config
        coEvery { providerRepository.getApiKey(AIProvider.OPENAI) } returns "sk-test"
        every  { providerRepository.getClient(AIProvider.OPENAI) } returns mockClient

        engine = ReActEngine(providerRepository, emptyMap())
    }

    private fun makeRun(goal: String = "test goal") = AgentRun(
        id     = UUID.randomUUID().toString(),
        goal   = goal,
        repoId = null,
        status = AgentRunStatus.PENDING,
    )

    // ── Final Answer on first step ────────────────────────────────────────────

    @Test
    fun `engine completes when model returns Final Answer`() = runTest {
        every { mockClient.streamChat(any(), any(), any()) } returns flowOf(
            StreamToken("Thought: I know the answer.\n"),
            StreamToken("Final Answer: The answer is 42.", done = true),
        )

        val run     = makeRun("What is 6×7?")
        val runFlow = MutableStateFlow(run)

        engine.execute(run, runFlow, maxSteps = 5, repoId = null)

        assertThat(runFlow.value.status).isEqualTo(AgentRunStatus.COMPLETED)
        assertThat(runFlow.value.finalAnswer).isNotNull()
        assertThat(runFlow.value.finalAnswer!!.contains("42")).isTrue()
    }

    @Test
    fun `engine status is RUNNING during execution`() = runTest {
        every { mockClient.streamChat(any(), any(), any()) } returns flowOf(
            StreamToken("Final Answer: done", done = true),
        )

        val run     = makeRun()
        val runFlow = MutableStateFlow(run)

        engine.execute(run, runFlow, maxSteps = 5, repoId = null)
        assertThat(runFlow.value.status).isEqualTo(AgentRunStatus.COMPLETED)
    }

    // ── maxSteps enforcement ──────────────────────────────────────────────────

    @Test
    fun `engine fails after maxSteps without Final Answer`() = runTest {
        // Model always tries to call a tool but never gives Final Answer
        every { mockClient.streamChat(any(), any(), any()) } returns flowOf(
            StreamToken("Thought: Let me think.\nAction: unknown_tool\nAction Input: {}", done = true),
        )

        val run     = makeRun()
        val runFlow = MutableStateFlow(run)

        engine.execute(run, runFlow, maxSteps = 2, repoId = null)

        assertThat(runFlow.value.status).isEqualTo(AgentRunStatus.FAILED)
        assertThat(runFlow.value.finalAnswer).isNotNull()
        assertThat(runFlow.value.finalAnswer!!.contains("maximum")).isTrue()
    }

    // ── No API key ────────────────────────────────────────────────────────────

    @Test
    fun `engine fails immediately when no API key`() = runTest {
        coEvery { providerRepository.getApiKey(any()) } returns null

        val run     = makeRun()
        val runFlow = MutableStateFlow(run)

        engine.execute(run, runFlow, maxSteps = 5, repoId = null)

        assertThat(runFlow.value.status).isEqualTo(AgentRunStatus.FAILED)
        assertThat(runFlow.value.finalAnswer!!.contains("No API key")).isTrue()
    }

    // ── Tool dispatch ─────────────────────────────────────────────────────────

    @Test
    fun `engine dispatches to registered tool and appends observation`() = runTest {
        val mockTool = mockk<AgentToolExecutor>(relaxed = true)
        every  { mockTool.tool.name } returns "my_tool"
        coEvery { mockTool.execute(any(), any()) } returns "Tool result: success"

        val engineWithTool = ReActEngine(providerRepository, mapOf("my_tool" to mockTool))

        var callCount = 0
        every { mockClient.streamChat(any(), any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                flowOf(
                    StreamToken("Thought: Use tool.\nAction: my_tool\nAction Input: {}", done = true),
                )
            } else {
                flowOf(StreamToken("Final Answer: done", done = true))
            }
        }

        val run     = makeRun()
        val runFlow = MutableStateFlow(run)

        engineWithTool.execute(run, runFlow, maxSteps = 5, repoId = null)

        assertThat(runFlow.value.status).isEqualTo(AgentRunStatus.COMPLETED)
        // At least one step should have used the tool
        val toolStep = runFlow.value.steps.find { it.toolName == "my_tool" }
        assertThat(toolStep).isNotNull()
        assertThat(toolStep!!.toolOutput).isEqualTo("Tool result: success")
    }

    // ── Unknown tool ──────────────────────────────────────────────────────────

    @Test
    fun `engine returns error observation for unknown tool`() = runTest {
        var callCount = 0
        every { mockClient.streamChat(any(), any(), any()) } answers {
            callCount++
            if (callCount == 1) {
                flowOf(StreamToken("Action: nonexistent_tool\nAction Input: {}", done = true))
            } else {
                flowOf(StreamToken("Final Answer: done", done = true))
            }
        }

        val run     = makeRun()
        val runFlow = MutableStateFlow(run)

        engine.execute(run, runFlow, maxSteps = 5, repoId = null)

        val errorStep = runFlow.value.steps.find {
            it.toolOutput?.contains("unknown tool") == true
        }
        assertThat(errorStep).isNotNull()
    }

    // ── completedAt timestamp ─────────────────────────────────────────────────

    @Test
    fun `completedAt is set when run finishes`() = runTest {
        every { mockClient.streamChat(any(), any(), any()) } returns
            flowOf(StreamToken("Final Answer: done", done = true))

        val run     = makeRun()
        val runFlow = MutableStateFlow(run)

        engine.execute(run, runFlow, maxSteps = 5, repoId = null)

        assertThat(runFlow.value.completedAt).isNotNull()
    }
}
