package com.devos.ai.feature.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.devos.ai.feature.home.search.SearchNavEvent
import com.devos.ai.feature.home.search.SearchResult
import com.devos.ai.feature.home.search.SearchResultType
import com.devos.ai.feature.home.search.SearchUiState
import com.devos.ai.feature.home.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SearchViewModel].
 *
 * Covers: idle state, query change, debounce, scope filtering, and result tap nav event.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SearchViewModel(ioDispatcher = testDispatcher)

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state is Idle`() = runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value).isInstanceOf(SearchUiState.Idle::class)
    }

    // ── Query changes ─────────────────────────────────────────────────────────

    @Test
    fun `empty query transitions back to Idle`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("hello")
        viewModel.onQueryChange("")
        assertThat(viewModel.uiState.value).isInstanceOf(SearchUiState.Idle::class)
    }

    @Test
    fun `non-empty query transitions to Searching then Success`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("HomeViewModel")
        advanceTimeBy(400)
        assertThat(viewModel.uiState.value).isInstanceOf(SearchUiState.Success::class)
    }

    @Test
    fun `Success state contains code results for generic query`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("ViewModel")
        advanceTimeBy(400)
        val state = viewModel.uiState.value as SearchUiState.Success
        assertThat(state.results[SearchResultType.CODE]?.isEmpty()).isEqualTo(false)
    }

    @Test
    fun `Success state contains semantic match`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("navigation")
        advanceTimeBy(400)
        val state = viewModel.uiState.value as SearchUiState.Success
        assertThat(state.semanticMatch != null).isEqualTo(true)
    }

    // ── Scope filtering ───────────────────────────────────────────────────────

    @Test
    fun `onScopeChange to code returns only code results`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("ViewModel")
        advanceTimeBy(400)
        viewModel.onScopeChange("code")
        advanceTimeBy(400)
        val state = viewModel.uiState.value as SearchUiState.Success
        assertThat(state.scope).isEqualTo("code")
        assertThat(state.results.keys.all { it == SearchResultType.CODE }).isEqualTo(true)
    }

    @Test
    fun `onScopeChange to issues returns only issue results`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("fix")
        advanceTimeBy(400)
        viewModel.onScopeChange("issues")
        advanceTimeBy(400)
        val state = viewModel.uiState.value as SearchUiState.Success
        assertThat(state.results.keys.all { it == SearchResultType.ISSUE }).isEqualTo(true)
    }

    @Test
    fun `onScopeChange to learn returns only learning results`() = runTest {
        val viewModel = createViewModel()
        viewModel.onQueryChange("coroutines")
        advanceTimeBy(400)
        viewModel.onScopeChange("learn")
        advanceTimeBy(400)
        val state = viewModel.uiState.value as SearchUiState.Success
        assertThat(state.results.keys.all { it == SearchResultType.LEARNING }).isEqualTo(true)
    }

    // ── Result tap nav event ──────────────────────────────────────────────────

    @Test
    fun `onResultTap emits NavigateToRoute nav event`() = runTest {
        val viewModel = createViewModel()
        val stubResult = SearchResult(
            id = "c1",
            type = SearchResultType.CODE,
            title = "HomeViewModel.kt",
            subtitle = "feature/feature-home",
            route = "home",
        )

        viewModel.navEvent.test {
            viewModel.onResultTap(stubResult)
            val event = awaitItem()
            assertThat(event).isInstanceOf(SearchNavEvent.NavigateToRoute::class)
            assertThat((event as SearchNavEvent.NavigateToRoute).route).isEqualTo("home")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onBack emits NavigateBack nav event`() = runTest {
        val viewModel = createViewModel()
        viewModel.navEvent.test {
            viewModel.onBack()
            assertThat(awaitItem()).isEqualTo(SearchNavEvent.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
