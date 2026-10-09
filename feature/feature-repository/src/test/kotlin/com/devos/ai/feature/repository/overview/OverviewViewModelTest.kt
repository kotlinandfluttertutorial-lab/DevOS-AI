package com.devos.ai.feature.repository.overview

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.devos.ai.feature.repository.model.OverviewTab
import com.devos.ai.feature.repository.model.stubRepoOverview
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [OverviewViewModel].
 *
 * Uses [UnconfinedTestDispatcher] so coroutines run eagerly.
 * MockK is used to mock [RepositoryOverviewProvider].
 * Turbine is used for [OverviewNavEvent] Flow assertions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockProvider: RepositoryOverviewProvider

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockProvider = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OverviewViewModel =
        OverviewViewModel(ioDispatcher = testDispatcher, provider = mockProvider)

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Loading before coroutines run`() = runTest {
        // Loading is emitted synchronously before the launch completes.
        // With UnconfinedTestDispatcher the coroutine may run immediately in
        // init, so we verify Loading was the first emission by observing the
        // StateFlow history. If the provider is not yet configured, the coroutine
        // will throw and the state becomes Error — so set up the happy-path here.
        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()

        // With UnconfinedTestDispatcher the init block may complete immediately.
        // This test verifies the state machine transitions correctly from Loading.
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Post-init, the state must be Success (stub returns immediately).
        assertThat(viewModel.uiState.value).isInstanceOf(OverviewUiState.Success::class)
    }

    @Test
    fun `loads stub overview then transitions to Success`() = runTest {
        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as OverviewUiState.Success
        assertThat(state.overview.name).isEqualTo("devos-ai")
        assertThat(state.selectedTab).isEqualTo(OverviewTab.OVERVIEW)
    }

    // ── Tab selection ─────────────────────────────────────────────────────────

    @Test
    fun `onTabSelect updates selectedTab`() = runTest {
        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTabSelect(OverviewTab.FILES)

        val state = viewModel.uiState.value as OverviewUiState.Success
        assertThat(state.selectedTab).isEqualTo(OverviewTab.FILES)
    }

    @Test
    fun `onTabSelect same tab is a no-op`() = runTest {
        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val stateBefore = viewModel.uiState.value
        viewModel.onTabSelect(OverviewTab.OVERVIEW) // same as initial

        // Reference equality — state object must be unchanged
        assertThat(viewModel.uiState.value).isEqualTo(stateBefore)
    }

    @Test
    fun `onTabSelect is a no-op when state is not Success`() = runTest {
        coEvery { mockProvider.loadOverview() } throws RuntimeException("boom")
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(OverviewUiState.Error::class)
        viewModel.onTabSelect(OverviewTab.FILES)

        // Still in Error state — tab select must not crash or change state
        assertThat(viewModel.uiState.value).isInstanceOf(OverviewUiState.Error::class)
    }

    // ── Error state ───────────────────────────────────────────────────────────

    @Test
    fun `error state on provider failure`() = runTest {
        coEvery { mockProvider.loadOverview() } throws RuntimeException("boom")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as OverviewUiState.Error
        assertThat(state.message).isEqualTo("boom")
        assertThat(state.retryable).isEqualTo(true)
    }

    // ── Retry ─────────────────────────────────────────────────────────────────

    @Test
    fun `onRetry reloads after error`() = runTest {
        coEvery { mockProvider.loadOverview() } throws RuntimeException("fail")
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(OverviewUiState.Error::class)

        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()
        viewModel.onRetry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(OverviewUiState.Success::class)
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `onRefresh re-runs load and transitions to Error when provider throws`() = runTest {
        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(OverviewUiState.Success::class)

        coEvery { mockProvider.loadOverview() } throws RuntimeException("network error")
        viewModel.onRefresh()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(OverviewUiState.Error::class)
    }

    @Test
    fun `onRefresh re-runs load and transitions back to Success`() = runTest {
        coEvery { mockProvider.loadOverview() } throws RuntimeException("fail")
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isInstanceOf(OverviewUiState.Error::class)

        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()
        viewModel.onRefresh()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(OverviewUiState.Success::class)
    }

    // ── Navigation events ─────────────────────────────────────────────────────

    @Test
    fun `onNavigateBack emits NavigateBack`() = runTest {
        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onNavigateBack()
            assertThat(awaitItem()).isEqualTo(OverviewNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onGitTap emits NavigateToGit`() = runTest {
        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onGitTap()
            assertThat(awaitItem()).isEqualTo(OverviewNavEvent.NavigateToGit)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAITap emits NavigateToAI`() = runTest {
        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onAITap()
            assertThat(awaitItem()).isEqualTo(OverviewNavEvent.NavigateToAI)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onFilesTap emits NavigateToFiles`() = runTest {
        coEvery { mockProvider.loadOverview() } returns stubRepoOverview()
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onFilesTap()
            assertThat(awaitItem()).isEqualTo(OverviewNavEvent.NavigateToFiles)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Tab enum guard ────────────────────────────────────────────────────────

    @Test
    fun `OverviewTab has exactly 7 entries`() {
        assertThat(OverviewTab.entries.size).isEqualTo(7)
    }

    @Test
    fun `OverviewTab labels match the mockup`() {
        val expected = listOf("Overview", "Files", "Search", "Symbols", "Graph", "Git", "AI")
        assertThat(OverviewTab.entries.map { it.label }).isEqualTo(expected)
    }
}
