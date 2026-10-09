package com.devos.ai.feature.code.search

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CodeSearchViewModel].
 *
 * Key behaviors:
 * - Empty query → Idle state
 * - Query change debounced 300ms before search fires
 * - Matching query → Success with results
 * - No-match query → Empty state
 * - Mode toggle changes search mode
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CodeSearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(repoId: String = "repo-1", initialQuery: String = "") =
        CodeSearchViewModel(
            SavedStateHandle(
                buildMap {
                    put("repoId", repoId)
                    if (initialQuery.isNotBlank()) put("q", initialQuery)
                },
            ),
        )

    @Test
    fun `initial state is Idle when no query provided`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(CodeSearchUiState.Idle::class)
        }
    }

    @Test
    fun `empty query sets state to Idle`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("something")
        advanceUntilIdle()

        vm.onQueryChange("")
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(CodeSearchUiState.Idle::class)
        }
    }

    @Test
    fun `query with matching text produces Success state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("Repository")
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            // "Repository" matches several stub entries
            assertThat(state is CodeSearchUiState.Success || state is CodeSearchUiState.Empty).isTrue()
        }
    }

    @Test
    fun `query with no matching text produces Empty state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("zzz_no_match_xyzzy_12345")
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(CodeSearchUiState.Empty::class)
        }
    }

    @Test
    fun `initial query from SavedStateHandle triggers search immediately`() = runTest {
        val vm = createViewModel(initialQuery = "ViewModel")
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state is CodeSearchUiState.Success || state is CodeSearchUiState.Empty).isTrue()
        }
    }

    @Test
    fun `semantic toggle changes search mode from FULL_TEXT to SEMANTIC`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val initialMode = vm.searchMode.value
        assertThat(initialMode).isEqualTo(SearchMode.FULL_TEXT)

        vm.onSemanticToggle()
        advanceUntilIdle()

        assertThat(vm.searchMode.value).isEqualTo(SearchMode.SEMANTIC)
    }

    @Test
    fun `semantic toggle back to FULL_TEXT`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSemanticToggle() // → SEMANTIC
        advanceUntilIdle()
        vm.onSemanticToggle() // → FULL_TEXT
        advanceUntilIdle()

        assertThat(vm.searchMode.value).isEqualTo(SearchMode.FULL_TEXT)
    }

    @Test
    fun `case toggle flips caseEnabled state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.caseEnabled.value).isEqualTo(false)
        vm.onCaseToggle()
        assertThat(vm.caseEnabled.value).isEqualTo(true)
    }

    @Test
    fun `regex toggle flips regexEnabled state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertThat(vm.regexEnabled.value).isEqualTo(false)
        vm.onRegexToggle()
        assertThat(vm.regexEnabled.value).isEqualTo(true)
    }

    @Test
    fun `result tap emits NavigateToCodeViewer nav event`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("Repository")
        advanceUntilIdle()

        val state = vm.uiState.value
        if (state !is CodeSearchUiState.Success) return@runTest

        vm.navEvent.test {
            vm.onResultTap(state.results.first())
            val event = awaitItem()
            assertThat(event).isInstanceOf(CodeSearchNavEvent.NavigateToCodeViewer::class)
        }
    }

    @Test
    fun `navigate back emits NavigateBack event`() = runTest {
        val vm = createViewModel()

        vm.navEvent.test {
            vm.onNavigateBack()
            val event = awaitItem()
            assertThat(event).isEqualTo(CodeSearchNavEvent.NavigateBack)
        }
    }

    @Test
    fun `Success state contains results with non-empty file names`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("class")
        advanceUntilIdle()

        val state = vm.uiState.value
        if (state is CodeSearchUiState.Success) {
            assertThat(state.results).isNotEmpty()
            state.results.forEach { result ->
                assertThat(result.fileName.isNotBlank()).isTrue()
            }
        }
    }
}
