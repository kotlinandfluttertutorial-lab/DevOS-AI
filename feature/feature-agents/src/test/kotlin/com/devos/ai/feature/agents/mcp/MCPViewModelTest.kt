package com.devos.ai.feature.agents.mcp

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isNotNull
import com.devos.ai.domain.ai.model.MCPServer
import com.devos.ai.domain.ai.model.MCPServerStatus
import com.devos.ai.domain.ai.model.MCPTool
import com.devos.ai.domain.ai.repository.MCPRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
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

/**
 * Unit tests for [MCPViewModel].
 * DEVOS-039 / DA-51
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MCPViewModelTest {

    private lateinit var repository: MCPRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private val serverA = MCPServer(
        id          = "github-mcp",
        name        = "GitHub MCP",
        url         = "https://mcp.github.com",
        isConnected = true,
        toolCount   = 4,
        status      = MCPServerStatus.CONNECTED,
    )

    private val serverB = MCPServer(
        id          = "aws-docs",
        name        = "AWS Docs",
        url         = "https://mcp.aws.amazon.com",
        isConnected = false,
        toolCount   = 3,
        status      = MCPServerStatus.IDLE,
    )

    private val safeTool = MCPTool(
        name          = "search_repositories",
        description   = "Search GitHub repositories by query",
        serverId      = "github-mcp",
        isDestructive = false,
    )

    private val destructiveTool = MCPTool(
        name          = "merge_pull_request",
        description   = "Merges a pull request — irreversible",
        serverId      = "github-mcp",
        isDestructive = true,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this, relaxed = true)
        repository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(): MCPViewModel {
        every { repository.observeServers() } returns flowOf(listOf(serverA, serverB))
        coEvery { repository.getToolsForServer("github-mcp") } returns listOf(safeTool, destructiveTool)
        coEvery { repository.getToolsForServer("aws-docs")  } returns emptyList()
        return MCPViewModel(repository)
    }

    // ── Load state ────────────────────────────────────────────────────────────

    @Test
    fun `initial state transitions to Success after load`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        assertThat(vm.uiState.value).isInstanceOf(MCPUiState.Success::class)
    }

    @Test
    fun `Success state has servers`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val state = vm.uiState.value as MCPUiState.Success
        assertThat(state.servers.size).isEqualTo(2)
    }

    @Test
    fun `Success state auto-selects first server`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val state = vm.uiState.value as MCPUiState.Success
        assertThat(state.selectedServer?.id).isEqualTo("github-mcp")
    }

    @Test
    fun `Success state loads tools for first server`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val state = vm.uiState.value as MCPUiState.Success
        assertThat(state.tools.size).isEqualTo(2)
    }

    // ── selectServer ──────────────────────────────────────────────────────────

    @Test
    fun `selectServer changes selectedServer`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.selectServer("aws-docs")
        val state = vm.uiState.value as MCPUiState.Success
        assertThat(state.selectedServer?.id).isEqualTo("aws-docs")
    }

    @Test
    fun `selectServer loads tools for new server`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.selectServer("aws-docs")
        val state = vm.uiState.value as MCPUiState.Success
        assertThat(state.tools).isEqualTo(emptyList<MCPTool>())
    }

    // ── runTool safe ──────────────────────────────────────────────────────────

    @Test
    fun `runTool with safe tool emits NavigateToToolExecution event`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.navEvent.test {
            vm.runTool(safeTool)
            val event = awaitItem() as MCPNavEvent.NavigateToToolExecution
            assertThat(event.toolName).isEqualTo("search_repositories")
            assertThat(event.serverId).isEqualTo("github-mcp")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `runTool with safe tool does NOT set pendingDestructiveTool`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.runTool(safeTool)
        val state = vm.uiState.value as MCPUiState.Success
        assertThat(state.pendingDestructiveTool).isNull()
    }

    // ── runTool destructive ───────────────────────────────────────────────────

    @Test
    fun `runTool with destructive tool sets pendingDestructiveTool`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.runTool(destructiveTool)
        val state = vm.uiState.value as MCPUiState.Success
        assertThat(state.pendingDestructiveTool).isNotNull()
        assertThat(state.pendingDestructiveTool?.name).isEqualTo("merge_pull_request")
    }

    @Test
    fun `runTool with destructive tool does NOT emit nav event immediately`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.navEvent.test {
            vm.runTool(destructiveTool)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── confirmDestructiveTool ────────────────────────────────────────────────

    @Test
    fun `confirmDestructiveTool clears pendingDestructiveTool`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.runTool(destructiveTool)
        vm.confirmDestructiveTool()
        val state = vm.uiState.value as MCPUiState.Success
        assertThat(state.pendingDestructiveTool).isNull()
    }

    @Test
    fun `confirmDestructiveTool emits NavigateToToolExecution event`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.navEvent.test {
            vm.runTool(destructiveTool)
            vm.confirmDestructiveTool()
            val event = awaitItem() as MCPNavEvent.NavigateToToolExecution
            assertThat(event.toolName).isEqualTo("merge_pull_request")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── cancelDestructiveTool ─────────────────────────────────────────────────

    @Test
    fun `cancelDestructiveTool clears pendingDestructiveTool without nav event`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.navEvent.test {
            vm.runTool(destructiveTool)
            vm.cancelDestructiveTool()
            val state = vm.uiState.value as MCPUiState.Success
            assertThat(state.pendingDestructiveTool).isNull()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── NavigateBack ──────────────────────────────────────────────────────────

    @Test
    fun `onNavigateBack emits NavigateBack event`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.navEvent.test {
            vm.onNavigateBack()
            assertThat(awaitItem()).isEqualTo(MCPNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    @Test
    fun `empty server list produces Empty state`() = runTest(testDispatcher) {
        every { repository.observeServers() } returns flowOf(emptyList())
        val vm = MCPViewModel(repository)
        assertThat(vm.uiState.value).isInstanceOf(MCPUiState.Empty::class)
    }

    // ── retry ─────────────────────────────────────────────────────────────────

    @Test
    fun `retry reloads servers after error`() = runTest(testDispatcher) {
        val failingRepo = mockk<MCPRepository>(relaxed = true)
        var callCount = 0
        every { failingRepo.observeServers() } answers {
            callCount++
            if (callCount == 1) throw RuntimeException("Network error")
            else flowOf(listOf(serverA))
        }
        coEvery { failingRepo.getToolsForServer(any()) } returns emptyList()

        // First call fails, sets error state
        val vm = MCPViewModel(failingRepo)
        assertThat(vm.uiState.value).isInstanceOf(MCPUiState.Error::class)

        // Retry should succeed
        vm.retry()
        assertThat(vm.uiState.value).isInstanceOf(MCPUiState.Success::class)
    }
}
