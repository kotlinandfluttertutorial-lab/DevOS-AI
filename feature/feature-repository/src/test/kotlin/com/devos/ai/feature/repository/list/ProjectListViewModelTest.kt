package com.devos.ai.feature.repository.list

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.devos.ai.feature.repository.model.HealthStatus
import com.devos.ai.feature.repository.model.SortOrder
import com.devos.ai.feature.repository.model.stubProjects
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
 * Unit tests for [ProjectListViewModel].
 *
 * Uses [UnconfinedTestDispatcher] so coroutines run eagerly.
 * Turbine is used for [ProjectListNavEvent] Flow assertions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ProjectListViewModel(ioDispatcher = testDispatcher)

    // ── Initial load ──────────────────────────────────────────────────────────

    @Test
    fun `loads stub projects and transitions to Success`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(ProjectListUiState.Success::class)
    }

    @Test
    fun `initial Success state has all 3 stub projects`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectListUiState.Success
        assertThat(state.repositories).hasSize(stubProjects().size)
    }

    @Test
    fun `initial sort order is NAME`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectListUiState.Success
        assertThat(state.sortOrder).isEqualTo(SortOrder.NAME)
    }

    @Test
    fun `initial filterLanguage is null (All)`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectListUiState.Success
        assertThat(state.filterLanguage).isEqualTo(null)
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    fun `onSearchQueryChange filters by name`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("DevOS")

        val state = viewModel.uiState.value as ProjectListUiState.Success
        assertThat(state.repositories).hasSize(1)
        assertThat(state.repositories[0].name).isEqualTo("DevOS AI")
    }

    @Test
    fun `onSearchQueryChange with blank query shows all projects`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("something")
        viewModel.onSearchQueryChange("")

        val state = viewModel.uiState.value as ProjectListUiState.Success
        assertThat(state.repositories).hasSize(stubProjects().size)
    }

    @Test
    fun `onSearchQueryChange with no matches keeps Success with empty list`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("xyznonexistentxyz")

        // Non-empty query → Success with empty list (not Empty state)
        val state = viewModel.uiState.value as ProjectListUiState.Success
        assertThat(state.repositories).hasSize(0)
    }

    // ── Language filter ───────────────────────────────────────────────────────

    @Test
    fun `onFilterLanguage filters to Kotlin projects`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onFilterLanguage("Kotlin")

        val state = viewModel.uiState.value as ProjectListUiState.Success
        state.repositories.forEach { repo ->
            assertThat(repo.languageTags.any { it.equals("Kotlin", ignoreCase = true) }).isTrue()
        }
    }

    @Test
    fun `onFilterLanguage null restores all projects`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onFilterLanguage("Kotlin")
        viewModel.onFilterLanguage(null)

        val state = viewModel.uiState.value as ProjectListUiState.Success
        assertThat(state.repositories).hasSize(stubProjects().size)
    }

    @Test
    fun `onFilterLanguage updates filterLanguage in state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onFilterLanguage("Android")

        val state = viewModel.uiState.value as ProjectListUiState.Success
        assertThat(state.filterLanguage).isEqualTo("Android")
    }

    // ── Sort ──────────────────────────────────────────────────────────────────

    @Test
    fun `onSortChange to HEALTH sorts descending by healthScore`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSortChange(SortOrder.HEALTH)

        val state = viewModel.uiState.value as ProjectListUiState.Success
        val scores = state.repositories.map { it.healthScore }
        assertThat(scores).isEqualTo(scores.sortedDescending())
    }

    @Test
    fun `onSortChange to NAME sorts alphabetically`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSortChange(SortOrder.NAME)

        val state = viewModel.uiState.value as ProjectListUiState.Success
        val names = state.repositories.map { it.name.lowercase() }
        assertThat(names).isEqualTo(names.sorted())
    }

    @Test
    fun `onSortChange updates sortOrder in state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSortChange(SortOrder.LAST_SYNC)

        val state = viewModel.uiState.value as ProjectListUiState.Success
        assertThat(state.sortOrder).isEqualTo(SortOrder.LAST_SYNC)
    }

    // ── Navigation events ─────────────────────────────────────────────────────

    @Test
    fun `onImportTap emits NavigateToImport`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onImportTap()
            assertThat(awaitItem()).isEqualTo(ProjectListNavEvent.NavigateToImport)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onProjectTap emits NavigateToRepo with correct id`() = runTest {
        val viewModel = createViewModel()

        viewModel.navEvent.test {
            viewModel.onProjectTap("devos-ai")
            val event = awaitItem() as ProjectListNavEvent.NavigateToRepo
            assertThat(event.repoId).isEqualTo("devos-ai")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Stub data guard ───────────────────────────────────────────────────────

    @Test
    fun `stub projects include all 3 health statuses`() {
        val projects = stubProjects()
        val statuses = projects.map { it.healthStatus }.toSet()
        assertThat(statuses.contains(HealthStatus.HEALTHY)).isTrue()
        assertThat(statuses.contains(HealthStatus.WARNING)).isTrue()
        assertThat(statuses.contains(HealthStatus.CRITICAL)).isTrue()
    }
}
