package com.devos.ai.feature.repository.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.core.common.di.IoDispatcher
import com.devos.ai.feature.repository.model.OverviewTab
import com.devos.ai.feature.repository.model.RepoOverview
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
 * ViewModel for the Repository Overview screen (DEVOS-016).
 *
 * Loads the (stub) repository overview via [RepositoryOverviewProvider] and
 * exposes [uiState] as a [StateFlow]. In-page tab selection mutates the
 * [OverviewUiState.Success] in-place; navigation requests are emitted as
 * one-shot [OverviewNavEvent]s via [navEvent].
 *
 * This ViewModel never imports [androidx.navigation.NavController].
 */
@HiltViewModel
class OverviewViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val provider: RepositoryOverviewProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OverviewUiState>(OverviewUiState.Loading)
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<OverviewNavEvent>()
    val navEvent: SharedFlow<OverviewNavEvent> = _navEvent.asSharedFlow()

    init {
        loadOverview()
    }

    /** Load (or reload) the overview. Transitions Loading → Success | Error. */
    private fun loadOverview() {
        _uiState.value = OverviewUiState.Loading
        viewModelScope.launch {
            try {
                val overview: RepoOverview = withContext(ioDispatcher) { provider.loadOverview() }
                _uiState.value = OverviewUiState.Success(overview, OverviewTab.OVERVIEW)
            } catch (t: Throwable) {
                _uiState.value = OverviewUiState.Error(
                    message = t.message ?: "Failed to load repository overview",
                    retryable = true,
                )
            }
        }
    }

    /** In-page tab switch (drives the HorizontalPager). No navigation event. */
    fun onTabSelect(tab: OverviewTab) {
        val current = _uiState.value
        if (current is OverviewUiState.Success && current.selectedTab != tab) {
            _uiState.value = current.copy(selectedTab = tab)
        }
    }

    /** Top-bar refresh icon. */
    fun onRefresh() = loadOverview()

    /** Error-state Retry button. */
    fun onRetry() = loadOverview()

    fun onNavigateBack() {
        viewModelScope.launch { _navEvent.emit(OverviewNavEvent.NavigateBack) }
    }

    fun onGitTap() {
        viewModelScope.launch { _navEvent.emit(OverviewNavEvent.NavigateToGit) }
    }

    fun onAITap() {
        viewModelScope.launch { _navEvent.emit(OverviewNavEvent.NavigateToAI) }
    }

    fun onFilesTap() {
        viewModelScope.launch { _navEvent.emit(OverviewNavEvent.NavigateToFiles) }
    }
}
