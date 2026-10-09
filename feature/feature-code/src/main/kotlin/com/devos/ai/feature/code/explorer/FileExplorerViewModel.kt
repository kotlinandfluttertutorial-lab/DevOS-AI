package com.devos.ai.feature.code.explorer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.code.model.FileItem
import com.devos.ai.feature.code.model.FileItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState ──────────────────────────────────────────────────────────────────

sealed interface FileExplorerUiState {
    data object Loading : FileExplorerUiState
    data class Success(
        val items: List<FileItem>,
        val breadcrumb: List<String>,
        val currentPath: String,
        val activeFilter: FileTypeFilter,
    ) : FileExplorerUiState
    data object Empty : FileExplorerUiState
    data class Error(val message: String, val retryable: Boolean) : FileExplorerUiState
}

enum class FileTypeFilter(val label: String) {
    ALL("All"),
    KOTLIN("Kotlin"),
    XML("XML"),
    GRADLE("Gradle"),
}

// ── Nav events ────────────────────────────────────────────────────────────────

sealed interface FileExplorerNavEvent {
    data class NavigateToCodeViewer(val repoId: String, val filePath: String) : FileExplorerNavEvent
    data object NavigateBack : FileExplorerNavEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val repoId: String = savedStateHandle["repoId"] ?: ""

    private val _uiState = MutableStateFlow<FileExplorerUiState>(FileExplorerUiState.Loading)
    val uiState: StateFlow<FileExplorerUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<FileExplorerNavEvent>()
    val navEvent: SharedFlow<FileExplorerNavEvent> = _navEvent.asSharedFlow()

    // Breadcrumb path stack
    private val pathStack = mutableListOf<String>()

    init {
        loadDirectory(path = "")
    }

    fun onFilterChange(filter: FileTypeFilter) {
        val current = _uiState.value as? FileExplorerUiState.Success ?: return
        val filtered = filterItems(stubFilesFor(current.currentPath), filter)
        _uiState.value = current.copy(items = filtered, activeFilter = filter)
    }

    fun onFolderTap(item: FileItem) {
        pathStack.add(item.name)
        loadDirectory(path = item.path)
    }

    fun onFileTap(item: FileItem) {
        viewModelScope.launch {
            _navEvent.emit(FileExplorerNavEvent.NavigateToCodeViewer(repoId, item.path))
        }
    }

    fun onBreadcrumbTap(index: Int) {
        // Pop stack to the selected index
        while (pathStack.size > index + 1) {
            pathStack.removeLastOrNull()
        }
        val path = pathStack.lastOrNull() ?: ""
        loadDirectory(path = path)
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            _navEvent.emit(FileExplorerNavEvent.NavigateBack)
        }
    }

    fun retry() = loadDirectory(path = ((_uiState.value as? FileExplorerUiState.Success)?.currentPath ?: ""))

    private fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.value = FileExplorerUiState.Loading

            val items = stubFilesFor(path)
            val breadcrumb = buildBreadcrumb(path)

            if (items.isEmpty()) {
                _uiState.value = FileExplorerUiState.Empty
            } else {
                _uiState.value = FileExplorerUiState.Success(
                    items = items,
                    breadcrumb = breadcrumb,
                    currentPath = path,
                    activeFilter = FileTypeFilter.ALL,
                )
            }
        }
    }

    private fun buildBreadcrumb(path: String): List<String> {
        val parts = mutableListOf("DevOS-AI")
        if (path.isNotBlank()) {
            parts.addAll(path.split("/").filter { it.isNotBlank() })
        }
        return parts
    }

    private fun filterItems(items: List<FileItem>, filter: FileTypeFilter): List<FileItem> = when (filter) {
        FileTypeFilter.ALL -> items
        FileTypeFilter.KOTLIN -> items.filter { it.type == FileItemType.KOTLIN || it.type == FileItemType.FOLDER }
        FileTypeFilter.XML -> items.filter { it.type == FileItemType.XML || it.type == FileItemType.FOLDER }
        FileTypeFilter.GRADLE -> items.filter { it.type == FileItemType.GRADLE || it.type == FileItemType.FOLDER }
    }

    // ── Stub data ────────────────────────────────────────────────────────────

    private fun stubFilesFor(path: String): List<FileItem> = if (path.isBlank()) {
        listOf(
            FileItem("1", "app", "app", FileItemType.FOLDER, fileCount = 12),
            FileItem("2", "core", "core", FileItemType.FOLDER, fileCount = 8),
            FileItem("3", "designsystem", "designsystem", FileItemType.FOLDER, fileCount = 24),
            FileItem("4", "feature", "feature", FileItemType.FOLDER, fileCount = 64),
            FileItem("5", "domain", "domain", FileItemType.FOLDER, fileCount = 16),
            FileItem("6", "data", "data", FileItemType.FOLDER, fileCount = 20),
            FileItem("7", "build.gradle.kts", "build.gradle.kts", FileItemType.GRADLE, "2.1 KB", "2h ago"),
            FileItem("8", "settings.gradle.kts", "settings.gradle.kts", FileItemType.GRADLE, "1.4 KB", "2h ago"),
        )
    } else {
        listOf(
            FileItem("a1", "MainActivity.kt", "$path/MainActivity.kt", FileItemType.KOTLIN, "4.2 KB", "1h ago"),
            FileItem("a2", "DevOSNavGraph.kt", "$path/DevOSNavGraph.kt", FileItemType.KOTLIN, "8.6 KB", "30m ago"),
            FileItem("a3", "DevOSApplication.kt", "$path/DevOSApplication.kt", FileItemType.KOTLIN, "1.2 KB", "2h ago"),
            FileItem("a4", "AndroidManifest.xml", "$path/AndroidManifest.xml", FileItemType.XML, "3.1 KB", "1d ago"),
            FileItem("a5", "activity_main.xml", "$path/activity_main.xml", FileItemType.XML, "0.8 KB", "3d ago"),
            FileItem("a6", "build.gradle.kts", "$path/build.gradle.kts", FileItemType.GRADLE, "2.4 KB", "2h ago"),
            FileItem("a7", "RepositoryViewModel.kt", "$path/RepositoryViewModel.kt", FileItemType.KOTLIN, "6.3 KB", "45m ago"),
            FileItem("a8", "strings.xml", "$path/strings.xml", FileItemType.XML, "1.8 KB", "2d ago"),
        )
    }
}
