package com.devos.ai.feature.auth.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SmartToy
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.auth.usecase.CheckAuthStateUseCase
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
 * ViewModel for the 4-page Onboarding carousel.
 *
 * Pages are statically defined per DEVOS-010 spec. Navigation out of onboarding
 * (skip or complete) sets the [KEY_ONBOARDING_COMPLETE] DataStore flag then
 * emits [OnboardingNavEvent.ToLogin].
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    companion object {
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

        /** The 4 onboarding pages per DEVOS-010 specification. */
        val ONBOARDING_PAGES = listOf(
            OnboardingPage(
                icon = Icons.Outlined.FolderOpen,
                title = "Repository Intelligence",
                description = "Import any GitHub or GitLab repo. DevOS indexes everything so you can explore and search instantly.",
            ),
            OnboardingPage(
                icon = Icons.Outlined.Code,
                title = "Code Intelligence",
                description = "Browse files, search symbols, and visualize dependencies — all from your phone.",
            ),
            OnboardingPage(
                icon = Icons.Outlined.SmartToy,
                title = "AI Platform",
                description = "Ask questions grounded in your actual code. AI that knows your repository, not just generic answers.",
            ),
            OnboardingPage(
                icon = Icons.Outlined.School,
                title = "Learn While You Build",
                description = "Get personalized learning recommendations based on the patterns in your current project.",
            ),
        )
    }

    private val _uiState = MutableStateFlow<OnboardingUiState>(
        OnboardingUiState.Success(pages = ONBOARDING_PAGES, currentPage = 0),
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<OnboardingNavEvent>()
    val navEvent: SharedFlow<OnboardingNavEvent> = _navEvent.asSharedFlow()

    /** Advance to the next page (no-op if already on the last page). */
    fun nextPage() {
        val current = _uiState.value as? OnboardingUiState.Success ?: return
        if (current.currentPage < ONBOARDING_PAGES.lastIndex) {
            _uiState.value = current.copy(currentPage = current.currentPage + 1)
        }
    }

    /** Go back to the previous page (no-op if already on the first page). */
    fun previousPage() {
        val current = _uiState.value as? OnboardingUiState.Success ?: return
        if (current.currentPage > 0) {
            _uiState.value = current.copy(currentPage = current.currentPage - 1)
        }
    }

    /** Sync ViewModel page when the pager swipes to a new page. */
    fun setPage(page: Int) {
        val current = _uiState.value as? OnboardingUiState.Success ?: return
        _uiState.value = current.copy(currentPage = page)
    }

    /**
     * User tapped Skip — mark onboarding complete and navigate to Login.
     *
     * Saves [KEY_ONBOARDING_COMPLETE] = true in DataStore.
     */
    fun skipOnboarding() {
        viewModelScope.launch {
            markOnboardingComplete()
            _navEvent.emit(OnboardingNavEvent.ToLogin)
        }
    }

    /**
     * User tapped Get Started on the last page — same as [skipOnboarding].
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            markOnboardingComplete()
            _navEvent.emit(OnboardingNavEvent.ToLogin)
        }
    }

    private suspend fun markOnboardingComplete() {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] = true
        }
    }
}
