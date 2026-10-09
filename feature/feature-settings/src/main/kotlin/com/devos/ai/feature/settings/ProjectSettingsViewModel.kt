package com.devos.ai.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Project Settings screen. */
data class ProjectSettingsUiState(
    val name: String = "DevOS AI",
    val description: String = "AI-powered Android developer command center",
    val githubRepo: String = "github.com/dev/devos-ai",
    val branch: String = "main",
    val autoInclude: Boolean = true,
    val showDeleteDialog: Boolean = false,
)

/** One-shot navigation events emitted by [ProjectSettingsViewModel]. */
sealed class ProjectSettingsNavEvent {
    data object NavigateBack : ProjectSettingsNavEvent()
}

/**
 * ViewModel for [ProjectSettingsScreen].
 *
 * Loads stub project data from SavedStateHandle. All update operations mutate local state.
 * Delete is guarded by a confirmation dialog — on confirm, NavigateBack is emitted.
 *
 * NavController is never imported here.
 *
 * DEVOS-063 / DA-75
 */
@HiltViewModel
class ProjectSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    @Suppress("UNUSED")
    private val projectId: String = savedStateHandle.get<String>("projectId") ?: ""

    private val _uiState = MutableStateFlow(ProjectSettingsUiState())
    val uiState: StateFlow<ProjectSettingsUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ProjectSettingsNavEvent>()
    val navEvent: SharedFlow<ProjectSettingsNavEvent> = _navEvent.asSharedFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateBranch(branch: String) {
        _uiState.value = _uiState.value.copy(branch = branch)
    }

    fun updateAutoInclude(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoInclude = enabled)
    }

    /** Show the delete-project confirmation dialog. */
    fun showDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true)
    }

    /** Cancel the delete-project confirmation dialog. */
    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }

    /** Confirm deletion and navigate back. */
    fun confirmDelete() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
        viewModelScope.launch { _navEvent.emit(ProjectSettingsNavEvent.NavigateBack) }
    }

    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(ProjectSettingsNavEvent.NavigateBack) }
    }
}
