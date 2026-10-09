package com.devos.ai.feature.repository.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.core.common.di.IoDispatcher
import com.devos.ai.feature.repository.model.ProjectSummary
import com.devos.ai.feature.repository.model.SortOrder
import com.devos.ai.feature.repository.model.stubProjects
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
 * ViewModel for the Project / Repository List screen (DEVOS-017).
 *
 * Loads a stub list of projects, applies search / filter / sort, and exposes
 * [uiState] as a [StateFlow]. Navigation requests are emitted as one-shot
 * [ProjectListNavEvent]s via [navEvent].
 *
 * This ViewModel never imports [androidx.navigation.NavController].
 * DEVOS-015 will replace the stub load with a real domain use case.
 */
@HiltViewModel
class ProjectListViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectListUiState>(ProjectListUiState.Loading)
    val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ProjectListNavEvent>()
    val navEvent: SharedFlow<ProjectListNavEvent> = _navEvent.asSharedFlow()

    /** All projects loaded (full unfiltered list). */
    private var allProjects: List<ProjectSummary> = emptyList()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        _uiState.value = ProjectListUiState.Loading
        viewModelScope.launch {
            try {
                val projects = withContext(ioDispatcher) { stubProjects() }
                allProjects = projects
                applyFilters(searchQuery = "", filterLanguage = null, sortOrder = SortOrder.NAME)
            } catch (t: Throwable) {
                _uiState.value = ProjectListUiState.Error(
                    message = t.message ?: "Failed to load projects",
                    retryable = true,
                )
            }
        }
    }

    fun onRetry() = loadProjects()

    /** Update the live search query and re-filter. */
    fun onSearchQueryChange(query: String) {
        val current = _uiState.value as? ProjectListUiState.Success ?: return
        applyFilters(
            searchQuery = query,
            filterLanguage = current.filterLanguage,
            sortOrder = current.sortOrder,
        )
    }

    /** Select a language filter chip (null = All). */
    fun onFilterLanguage(language: String?) {
        val current = _uiState.value as? ProjectListUiState.Success ?: return
        applyFilters(
            searchQuery = current.searchQuery,
            filterLanguage = language,
            sortOrder = current.sortOrder,
        )
    }

    /** Change the sort order. */
    fun onSortChange(sortOrder: SortOrder) {
        val current = _uiState.value as? ProjectListUiState.Success ?: return
        applyFilters(
            searchQuery = current.searchQuery,
            filterLanguage = current.filterLanguage,
            sortOrder = sortOrder,
        )
    }

    fun onImportTap() {
        viewModelScope.launch { _navEvent.emit(ProjectListNavEvent.NavigateToImport) }
    }

    fun onProjectTap(repoId: String) {
        viewModelScope.launch { _navEvent.emit(ProjectListNavEvent.NavigateToRepo(repoId)) }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun applyFilters(
        searchQuery: String,
        filterLanguage: String?,
        sortOrder: SortOrder,
    ) {
        val filtered = allProjects
            .filter { project ->
                // Search filter: match on name or fullPath, case-insensitive
                val matchesSearch = searchQuery.isBlank() ||
                    project.name.contains(searchQuery, ignoreCase = true) ||
                    project.fullPath.contains(searchQuery, ignoreCase = true)
                // Language filter
                val matchesLanguage = filterLanguage == null ||
                    project.languageTags.any { it.equals(filterLanguage, ignoreCase = true) }
                matchesSearch && matchesLanguage
            }
            .let { list ->
                when (sortOrder) {
                    SortOrder.NAME      -> list.sortedBy { it.name.lowercase() }
                    SortOrder.LAST_SYNC -> list.sortedBy { it.lastSync }
                    SortOrder.HEALTH    -> list.sortedByDescending { it.healthScore }
                }
            }

        _uiState.value = if (filtered.isEmpty() && searchQuery.isBlank() && filterLanguage == null) {
            ProjectListUiState.Empty
        } else {
            ProjectListUiState.Success(
                repositories = filtered,
                sortOrder = sortOrder,
                filterLanguage = filterLanguage,
                searchQuery = searchQuery,
            )
        }
    }
}
