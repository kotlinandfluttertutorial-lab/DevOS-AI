package com.devos.ai.feature.settings

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ProjectSettingsViewModel].
 *
 * DEVOS-063 / DA-75
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(projectId: String = "test-project"): ProjectSettingsViewModel {
        val handle = SavedStateHandle(mapOf("projectId" to projectId))
        return ProjectSettingsViewModel(handle)
    }

    // ── Initial state ────────────────────────────────────────────────────────────

    @Test
    fun `initial state has default stub values`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value
        assertEquals("DevOS AI", state.name)
        assertEquals("AI-powered Android developer command center", state.description)
        assertEquals("main", state.branch)
        assertTrue(state.autoInclude)
        assertFalse(state.showDeleteDialog)
    }

    // ── Update fields ────────────────────────────────────────────────────────────

    @Test
    fun `updateName updates name in state`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateName("My Project")
        assertEquals("My Project", viewModel.uiState.value.name)
    }

    @Test
    fun `updateDescription updates description in state`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateDescription("A new description")
        assertEquals("A new description", viewModel.uiState.value.description)
    }

    @Test
    fun `updateBranch updates branch in state`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateBranch("develop")
        assertEquals("develop", viewModel.uiState.value.branch)
    }

    @Test
    fun `updateAutoInclude toggles autoInclude to false`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateAutoInclude(false)
        assertFalse(viewModel.uiState.value.autoInclude)
    }

    @Test
    fun `updateAutoInclude toggles autoInclude back to true`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateAutoInclude(false)
        viewModel.updateAutoInclude(true)
        assertTrue(viewModel.uiState.value.autoInclude)
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────────

    @Test
    fun `showDeleteConfirmation sets showDeleteDialog to true`() = runTest {
        val viewModel = createViewModel()
        viewModel.showDeleteConfirmation()
        assertTrue(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `cancelDelete sets showDeleteDialog to false`() = runTest {
        val viewModel = createViewModel()
        viewModel.showDeleteConfirmation()
        viewModel.cancelDelete()
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `confirmDelete emits NavigateBack nav event`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.navEvent.test {
            viewModel.showDeleteConfirmation()
            viewModel.confirmDelete()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(ProjectSettingsNavEvent.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirmDelete resets showDeleteDialog to false before navigating`() = runTest {
        val viewModel = createViewModel()
        viewModel.showDeleteConfirmation()
        viewModel.confirmDelete()
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    // ── Navigate back ─────────────────────────────────────────────────────────────

    @Test
    fun `navigateBack emits NavigateBack nav event`() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.navEvent.test {
            viewModel.navigateBack()
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(ProjectSettingsNavEvent.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
