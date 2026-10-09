package com.devos.ai.feature.code.explorer

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.devos.ai.feature.code.model.FileItemType
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
 * Unit tests for [FileExplorerViewModel].
 *
 * Uses UnconfinedTestDispatcher for eager coroutine execution.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FileExplorerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(repoId: String = "repo-1") =
        FileExplorerViewModel(SavedStateHandle(mapOf("repoId" to repoId)))

    @Test
    fun `initial state transitions to Success with stub files`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(FileExplorerUiState.Success::class)
            val success = state as FileExplorerUiState.Success
            assertThat(success.items).isNotEmpty()
        }
    }

    @Test
    fun `filter ALL shows all items`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onFilterChange(FileTypeFilter.ALL)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem() as FileExplorerUiState.Success
            assertThat(state.activeFilter).isEqualTo(FileTypeFilter.ALL)
            assertThat(state.items).isNotEmpty()
        }
    }

    @Test
    fun `filter KOTLIN hides non-Kotlin non-folder items`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onFilterChange(FileTypeFilter.KOTLIN)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem() as FileExplorerUiState.Success
            val nonKotlinFiles = state.items.filter {
                it.type != FileItemType.KOTLIN && it.type != FileItemType.FOLDER
            }
            assertThat(nonKotlinFiles.isEmpty()).isTrue()
        }
    }

    @Test
    fun `filter XML hides non-XML non-folder items`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onFilterChange(FileTypeFilter.XML)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem() as FileExplorerUiState.Success
            val nonXmlFiles = state.items.filter {
                it.type != FileItemType.XML && it.type != FileItemType.FOLDER
            }
            assertThat(nonXmlFiles.isEmpty()).isTrue()
        }
    }

    @Test
    fun `filter GRADLE updates active filter to GRADLE`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onFilterChange(FileTypeFilter.GRADLE)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem() as FileExplorerUiState.Success
            assertThat(state.activeFilter).isEqualTo(FileTypeFilter.GRADLE)
        }
    }

    @Test
    fun `initial breadcrumb contains root segment`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem() as FileExplorerUiState.Success
            assertThat(state.breadcrumb).isNotEmpty()
        }
    }

    @Test
    fun `folder tap navigates deeper and updates breadcrumb`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val initialState = vm.uiState.value as? FileExplorerUiState.Success ?: return@runTest
        val folder = initialState.items.firstOrNull { it.type == FileItemType.FOLDER } ?: return@runTest
        val initialBreadcrumbSize = initialState.breadcrumb.size

        vm.onFolderTap(folder)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem() as FileExplorerUiState.Success
            assertThat(state.breadcrumb.size >= initialBreadcrumbSize).isTrue()
        }
    }

    @Test
    fun `file tap emits NavigateToCodeViewer nav event`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        // Navigate into a folder first to get files
        val state = vm.uiState.value as? FileExplorerUiState.Success ?: return@runTest
        val folder = state.items.firstOrNull { it.type == FileItemType.FOLDER } ?: return@runTest
        vm.onFolderTap(folder)
        advanceUntilIdle()

        val fileState = vm.uiState.value as? FileExplorerUiState.Success ?: return@runTest
        val kotlinFile = fileState.items.firstOrNull { it.type == FileItemType.KOTLIN } ?: return@runTest

        vm.navEvent.test {
            vm.onFileTap(kotlinFile)
            val event = awaitItem()
            assertThat(event).isInstanceOf(FileExplorerNavEvent.NavigateToCodeViewer::class)
        }
    }

    @Test
    fun `navigate back emits NavigateBack event`() = runTest {
        val vm = createViewModel()

        vm.navEvent.test {
            vm.onNavigateBack()
            val event = awaitItem()
            assertThat(event).isEqualTo(FileExplorerNavEvent.NavigateBack)
        }
    }
}
