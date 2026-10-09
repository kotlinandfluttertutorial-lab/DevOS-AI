package com.devos.ai.feature.memory

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

/** UI state for the Developer Memory screen. */
sealed interface DeveloperMemoryUiState {
    data object Loading : DeveloperMemoryUiState

    data class Success(
        val data: MemoryData,
        val query: String = "",
        val showClearDialog: Boolean = false,
    ) : DeveloperMemoryUiState

    data object Empty : DeveloperMemoryUiState

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : DeveloperMemoryUiState
}

/** One-shot navigation events emitted by [DeveloperMemoryViewModel]. */
sealed class MemoryNavEvent {
    /** Navigate back to the previous screen. */
    data object NavigateBack : MemoryNavEvent()
}

/**
 * ViewModel for [DeveloperMemoryScreen].
 *
 * Loads stub entries at init; exposes dismiss, clear, and query operations.
 * Navigation events are emitted via [SharedFlow] — NavController is never imported here.
 *
 * DEVOS-055 / DA-68
 */
@HiltViewModel
class DeveloperMemoryViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<DeveloperMemoryUiState>(DeveloperMemoryUiState.Loading)
    val uiState: StateFlow<DeveloperMemoryUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<MemoryNavEvent>()
    val navEvent: SharedFlow<MemoryNavEvent> = _navEvent.asSharedFlow()

    /** Full (unfiltered) source data; used for query filtering. */
    private var fullData: MemoryData = MemoryData(
        codePreferences = emptyList(),
        recentDecisions = emptyList(),
    )

    init {
        loadMemories()
    }

    private fun loadMemories() {
        viewModelScope.launch {
            fullData = MemoryData(
                codePreferences = listOf(
                    MemoryEntry(
                        id = "cp-1",
                        emoji = "⚙",
                        title = "Prefers Kotlin coroutines over RxJava",
                        source = "Learned from DevOS AI · 15 interactions",
                        isDismissible = true,
                    ),
                    MemoryEntry(
                        id = "cp-2",
                        emoji = "🎨",
                        title = "Uses MVVM + Clean Architecture consistently",
                        source = "Learned from code patterns · 8 projects",
                        isDismissible = true,
                    ),
                ),
                recentDecisions = listOf(
                    MemoryEntry(
                        id = "rd-1",
                        emoji = "💡",
                        title = "Decided to use Room over SQLDelight",
                        source = "DevOS AI · 3d ago",
                        isDismissible = false,
                    ),
                    MemoryEntry(
                        id = "rd-2",
                        emoji = "💡",
                        title = "Using Hilt for dependency injection across all modules",
                        source = "DevOS AI · 1 week ago",
                        isDismissible = false,
                    ),
                ),
            )
            applyCurrentQuery("")
        }
    }

    /** Dismiss a specific memory entry (only for isDismissible=true entries). */
    fun dismissEntry(id: String) {
        fullData = fullData.copy(
            codePreferences = fullData.codePreferences.filterNot { it.id == id },
            recentDecisions = fullData.recentDecisions.filterNot { it.id == id },
        )
        val current = _uiState.value
        val query = (current as? DeveloperMemoryUiState.Success)?.query ?: ""
        applyCurrentQuery(query)
    }

    /** Show the confirmation dialog before clearing all memories. */
    fun showClearAllDialog() {
        val current = _uiState.value as? DeveloperMemoryUiState.Success ?: return
        _uiState.value = current.copy(showClearDialog = true)
    }

    /** Cancel the clear-all dialog without deleting. */
    fun cancelClearAll() {
        val current = _uiState.value as? DeveloperMemoryUiState.Success ?: return
        _uiState.value = current.copy(showClearDialog = false)
    }

    /** Confirm clearing all memories. */
    fun confirmClearAll() {
        fullData = MemoryData(codePreferences = emptyList(), recentDecisions = emptyList())
        val current = _uiState.value as? DeveloperMemoryUiState.Success ?: return
        val query = current.query
        _uiState.value = current.copy(showClearDialog = false)
        applyCurrentQuery(query)
    }

    /** Update the search query and re-filter the displayed entries. */
    fun onQueryChange(query: String) {
        applyCurrentQuery(query)
    }

    /** Emit navigate-back event. */
    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(MemoryNavEvent.NavigateBack) }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun applyCurrentQuery(query: String) {
        val filtered = if (query.isBlank()) {
            fullData
        } else {
            val q = query.lowercase()
            fullData.copy(
                codePreferences = fullData.codePreferences.filter {
                    it.title.lowercase().contains(q) || it.source.lowercase().contains(q)
                },
                recentDecisions = fullData.recentDecisions.filter {
                    it.title.lowercase().contains(q) || it.source.lowercase().contains(q)
                },
            )
        }

        val allEmpty = filtered.codePreferences.isEmpty() && filtered.recentDecisions.isEmpty()
        val showClear = (_uiState.value as? DeveloperMemoryUiState.Success)?.showClearDialog ?: false

        _uiState.value = if (allEmpty && query.isBlank() &&
            fullData.codePreferences.isEmpty() && fullData.recentDecisions.isEmpty()
        ) {
            DeveloperMemoryUiState.Empty
        } else {
            DeveloperMemoryUiState.Success(
                data = filtered,
                query = query,
                showClearDialog = showClear,
            )
        }
    }
}
