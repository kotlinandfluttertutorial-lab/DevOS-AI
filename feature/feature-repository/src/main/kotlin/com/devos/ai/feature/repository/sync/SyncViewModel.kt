package com.devos.ai.feature.repository.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.repository.model.StepState
import com.devos.ai.feature.repository.model.SyncStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Repository Sync progress screen.
 *
 * Starts immediately in the [SyncUiState.Syncing] state with hardcoded stub steps
 * that match the mockup. DEVOS-015 will replace this with real WorkManager progress.
 *
 * Navigation events are emitted via [navEvent] — this ViewModel never imports NavController.
 */
@HiltViewModel
class SyncViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<SyncUiState>(
        SyncUiState.Syncing(
            steps = listOf(
                SyncStep(
                    name = "Fetch remote",
                    subtitle = "847 files · 2.3s",
                    state = StepState.COMPLETE,
                ),
                SyncStep(
                    name = "Index files",
                    subtitle = "247 / 847 files indexed",
                    state = StepState.IN_PROGRESS,
                ),
                SyncStep(
                    name = "Build symbol table",
                    subtitle = "Pending",
                    state = StepState.PENDING,
                ),
                SyncStep(
                    name = "Build code graph",
                    subtitle = "Pending",
                    state = StepState.PENDING,
                ),
                SyncStep(
                    name = "Update RAG embeddings",
                    subtitle = "Pending",
                    state = StepState.PENDING,
                ),
            ),
            currentStepIndex = 1,
            overallProgress = 0.29f,
        ),
    )
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<SyncNavEvent>()
    val navEvent: SharedFlow<SyncNavEvent> = _navEvent.asSharedFlow()

    /**
     * Cancel the current sync and navigate back.
     * DEVOS-015 will wire this to CancelSyncUseCase / WorkManager cancellation.
     */
    fun cancel() {
        viewModelScope.launch {
            _navEvent.emit(SyncNavEvent.NavigateBack)
        }
    }

    /**
     * Navigate back from the top-bar back arrow.
     *
     * Routes through a nav event so the back path is testable and consistent
     * with the Import screen — this ViewModel never imports NavController.
     * Distinct from [cancel] which represents the explicit "Cancel Sync" intent
     * (DEVOS-015 will also stop the indexing work there).
     */
    fun navigateBack() {
        viewModelScope.launch {
            _navEvent.emit(SyncNavEvent.NavigateBack)
        }
    }
}
