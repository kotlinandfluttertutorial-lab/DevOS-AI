package com.devos.ai.feature.repository.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.core.common.di.IoDispatcher
import com.devos.ai.feature.repository.model.RepositoryPreview
import com.devos.ai.feature.repository.model.RepositoryProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Repository Import screen.
 *
 * Handles URL input, source provider selection, validation, and import trigger.
 * Navigation events are emitted via [navEvent] — this ViewModel never imports NavController.
 *
 * All fake delays simulate network calls until DEVOS-015 provides real domain use cases.
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _selectedProvider = MutableStateFlow(RepositoryProvider.GITHUB)
    val selectedProvider: StateFlow<RepositoryProvider> = _selectedProvider.asStateFlow()

    private val _branch = MutableStateFlow("main")
    val branch: StateFlow<String> = _branch.asStateFlow()

    private val _buildAiIndex = MutableStateFlow(true)
    val buildAiIndex: StateFlow<Boolean> = _buildAiIndex.asStateFlow()

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ImportNavEvent>()
    val navEvent: SharedFlow<ImportNavEvent> = _navEvent.asSharedFlow()

    /** Update the URL field and reset validation state if we had a previous result. */
    fun onUrlChange(url: String) {
        _url.value = url
        val current = _uiState.value
        if (current is ImportUiState.Preview || current is ImportUiState.Error) {
            _uiState.value = ImportUiState.Idle
        }
    }

    /** Select a repository provider (GitHub / GitLab / Local). */
    fun onProviderSelect(provider: RepositoryProvider) {
        _selectedProvider.value = provider
    }

    /** Update the target branch. */
    fun onBranchChange(branch: String) {
        _branch.value = branch
    }

    /** Toggle the "Build AI Index" option. */
    fun onBuildAiIndexToggle(enabled: Boolean) {
        _buildAiIndex.value = enabled
    }

    /**
     * Validate the current URL against the selected provider.
     *
     * Transitions: Idle/Error → Validating → Preview (success) | Error (failure).
     * Uses a fake 1 s delay to simulate a network call (DEVOS-015 will replace this).
     */
    fun validate() {
        val currentUrl = _url.value
        if (currentUrl.isBlank()) {
            _uiState.value = ImportUiState.Error("Please enter a repository URL")
            return
        }
        viewModelScope.launch {
            _uiState.value = ImportUiState.Validating
            withContext(ioDispatcher) { delay(1_000L) }
            _uiState.value = ImportUiState.Preview(
                preview = RepositoryPreview(
                    name = extractRepoName(currentUrl),
                    owner = extractOwner(currentUrl),
                    stars = 2400,
                    fileCount = 847,
                    size = "12 MB",
                    branch = _branch.value,
                    isValid = true,
                ),
            )
        }
    }

    /**
     * Kick off the repository import.
     *
     * Transitions: Preview → Importing → (navigates to Sync screen).
     * Uses a fake 500 ms delay (DEVOS-015 will replace with WorkManager enqueue).
     */
    fun import() {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Importing
            withContext(ioDispatcher) { delay(500L) }
            _navEvent.emit(ImportNavEvent.NavigateToSync("repo-001"))
        }
    }

    /** Emit a back-navigation event. */
    fun navigateBack() {
        viewModelScope.launch {
            _navEvent.emit(ImportNavEvent.NavigateBack)
        }
    }

    // ── URL parsing helpers ──────────────────────────────────────────────────

    private fun extractRepoName(url: String): String =
        url.trimEnd('/').substringAfterLast('/').ifBlank { "repository" }

    private fun extractOwner(url: String): String {
        val parts = url.trimEnd('/').split('/')
        return if (parts.size >= 2) parts[parts.size - 2] else "owner"
    }
}
