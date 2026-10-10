package com.devos.ai.data.ai.repository

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.devos.ai.data.ai.agent.AgentToolExecutor
import com.devos.ai.data.ai.agent.ReActEngine
import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.model.AgentRunStatus
import com.devos.ai.domain.ai.model.AgentTool
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AgentRepositoryImpl].
 *
 * [ReActEngine] is mocked so the repo tests focus on lifecycle management:
 * - run creation and ID uniqueness
 * - flow observation
 * - cancellation
 * - getRegisteredTools returning correct tool list
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgentRepositoryImplTest {

    private lateinit var mockEngine: ReActEngine
    private lateinit var mockToolA: AgentToolExecutor
    private lateinit var mockToolB: AgentToolExecutor
    private lateinit var impl: AgentRepositoryImpl

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        mockEngine = mockk(relaxed = true)
        mockToolA  = mockk(relaxed = true)
        mockToolB  = mockk(relaxed = true)

        val toolA = AgentTool("tool_a", "Tool A description", "{}")
        val toolB = AgentTool("tool_b", "Tool B description", "{}")
        every { mockToolA.tool } returns toolA
        every { mockToolB.tool } returns toolB

        // Engine does nothing by default (relaxed) — run stays in PENDING
        coEvery { mockEngine.execute(any(), any(), any(), any()) } returns Unit

        impl = AgentRepositoryImpl(
            engine       = mockEngine,
            toolExecutors = mapOf("tool_a" to mockToolA, "tool_b" to mockToolB),
        )
    }

    // ── startRun ──────────────────────────────────────────────────────────────

    @Test
    fun `startRun returns a non-blank run ID`() = runTest {
        val id = impl.startRun("test goal", repoId = null)
        assertThat(id.isNotBlank()).isTrue()
    }

    @Test
    fun `startRun IDs are unique per call`() = runTest {
        val id1 = impl.startRun("goal 1", null)
        val id2 = impl.startRun("goal 2", null)
        assertThat(id1 != id2).isTrue()
    }

    // ── getRun ────────────────────────────────────────────────────────────────

    @Test
    fun `getRun returns AgentRun for existing ID`() = runTest {
        val id  = impl.startRun("explain auth", null)
        val run = impl.getRun(id)
        assertThat(run).isNotNull()
        assertThat(run!!.goal).isEqualTo("explain auth")
    }

    @Test
    fun `getRun returns null for unknown ID`() = runTest {
        assertThat(impl.getRun("no-such-id")).isNull()
    }

    // ── observeRun ────────────────────────────────────────────────────────────

    @Test
    fun `observeRun emits initial run state`() = runTest {
        val id  = impl.startRun("test", null)
        val run = impl.observeRun(id).first()
        assertThat(run).isNotNull()
        assertThat(run!!.id).isEqualTo(id)
    }

    @Test
    fun `observeRun for unknown ID emits null`() = runTest {
        val run = impl.observeRun("unknown").first()
        assertThat(run).isNull()
    }

    // ── cancelRun ─────────────────────────────────────────────────────────────

    @Test
    fun `cancelRun is idempotent for unknown ID`() = runTest {
        // Should not throw even for a non-existent run
        impl.cancelRun("no-such-id")
    }

    @Test
    fun `cancelRun removes scope from active scopes`() = runTest {
        val id = impl.startRun("long task", null)
        impl.cancelRun(id)
        // After cancellation scope is removed; a second cancel should not throw
        impl.cancelRun(id)
    }

    // ── getRegisteredTools ────────────────────────────────────────────────────

    @Test
    fun `getRegisteredTools returns all injected tools`() = runTest {
        val tools = impl.getRegisteredTools()
        assertThat(tools).hasSize(2)
        val names = tools.map { it.name }.toSet()
        assertThat(names.contains("tool_a")).isTrue()
        assertThat(names.contains("tool_b")).isTrue()
    }

    @Test
    fun `getRegisteredTools returns empty list when no tools injected`() = runTest {
        val emptyImpl = AgentRepositoryImpl(mockEngine, emptyMap())
        assertThat(emptyImpl.getRegisteredTools()).isEmpty()
    }

    // ── Goal preserved in run ─────────────────────────────────────────────────

    @Test
    fun `run preserves goal and repoId`() = runTest {
        val id  = impl.startRun("fix the bug", repoId = "repo-abc")
        val run = impl.getRun(id)!!
        assertThat(run.goal).isEqualTo("fix the bug")
        assertThat(run.repoId).isEqualTo("repo-abc")
    }
}
