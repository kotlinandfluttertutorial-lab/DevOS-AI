package com.devos.ai.feature.home.dashboard

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.core.common.di.IoDispatcher
import com.devos.ai.feature.home.model.AIRecommendation
import com.devos.ai.feature.home.model.ChatSessionSummary
import com.devos.ai.feature.home.model.HealthStatus
import com.devos.ai.feature.home.model.ProjectHealth
import com.devos.ai.feature.home.model.ProjectSummary
import com.devos.ai.feature.home.model.RecommendationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
/**
 * ViewModel for the Home Dashboard screen.
 *
 * Loads stub dashboard data and handles user interactions by emitting [HomeNavEvent]s.
 * Dismissed recommendations are persisted to DataStore so they survive app restarts.
 *
 * TODO(DEVOS-058): replace stub data with real domain use cases.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @Named("home") private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    companion object {
        private val DISMISSED_KEY = stringPreferencesKey("dismissed_recommendations")
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<HomeNavEvent>()
    val navEvent: SharedFlow<HomeNavEvent> = _navEvent.asSharedFlow()

    init {
        loadDashboard()
    }

    /**
     * Loads the dashboard. Can be called again on retry.
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val dismissed = withContext(ioDispatcher) {
                    dataStore.data.map { prefs ->
                        prefs[DISMISSED_KEY]
                            ?.split(",")
                            ?.filter { it.isNotEmpty() }
                            ?.toSet()
                            ?: emptySet()
                    }.first()
                }

                // TODO(DEVOS-058): replace stub data with GetDashboardUseCase
                val projects = stubProjects()
                val filteredRecs = stubRecommendations().filter { it.id !in dismissed }

                if (projects.isEmpty()) {
                    _uiState.value = HomeUiState.Empty
                } else {
                    _uiState.value = HomeUiState.Success(
                        recentProjects = projects,
                        recommendations = filteredRecs,
                        health = stubProjectHealth(),
                        recentSessions = stubSessions(),
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    message = e.message ?: "Failed to load dashboard",
                    retryable = true,
                )
            }
        }
    }

    /**
     * Dismisses a recommendation. Removes it from the current state immediately
     * and persists the dismissed ID to DataStore so it is filtered on next load.
     */
    fun dismissRecommendation(id: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                dataStore.edit { prefs ->
                    val current = prefs[DISMISSED_KEY]
                        ?.split(",")
                        ?.filter { it.isNotEmpty() }
                        ?.toMutableSet()
                        ?: mutableSetOf()
                    current.add(id)
                    prefs[DISMISSED_KEY] = current.joinToString(",")
                }
            }
            // Update in-memory state immediately without a full reload
            val current = _uiState.value
            if (current is HomeUiState.Success) {
                _uiState.value = current.copy(
                    recommendations = current.recommendations.filter { it.id != id },
                )
            }
        }
    }

    // ── Navigation event emitters ─────────────────────────────────────────────

    fun onSearchTap() = emit(HomeNavEvent.NavigateToSearch)

    fun onProjectTap(id: String) = emit(HomeNavEvent.NavigateToProject(id))

    fun onSessionTap(sessionId: String) = emit(HomeNavEvent.NavigateToChat(sessionId))

    fun onNotificationTap() = emit(HomeNavEvent.NavigateToNotifications)

    fun onProfileTap() = emit(HomeNavEvent.NavigateToProfile)

    fun onImportTap() = emit(HomeNavEvent.NavigateToImport)

    fun onSeeAllProjectsTap() = emit(HomeNavEvent.NavigateToProjectList)

    fun onSeeAllSessionsTap() = emit(HomeNavEvent.NavigateToAllSessions)

    private fun emit(event: HomeNavEvent) {
        viewModelScope.launch { _navEvent.emit(event) }
    }

    // ── Stub data — remove after DEVOS-058 ───────────────────────────────────

    private fun stubProjects() = listOf(
        ProjectSummary("p1", "DevOS AI", "Kotlin", HealthStatus.HEALTHY),
        ProjectSummary("p2", "Compose Lib", "Kotlin", HealthStatus.WARNING),
        ProjectSummary("p3", "SDK Tools", "Kotlin", HealthStatus.CRITICAL),
    )

    private fun stubRecommendations() = listOf(
        AIRecommendation(
            id = "r1",
            type = RecommendationType.SECURITY,
            title = "Fix 2 Hardcoded Secrets",
            description = "API keys detected in BuildConfig.kt",
        ),
        AIRecommendation(
            id = "r2",
            type = RecommendationType.TESTS,
            title = "Increase Test Coverage",
            description = "Coverage is at 34%, target is 80%",
        ),
        AIRecommendation(
            id = "r3",
            type = RecommendationType.LEARNING,
            title = "New Kotlin Coroutines Course",
            description = "Recommended based on your recent code",
        ),
    )

    private fun stubProjectHealth() = ProjectHealth(
        securityCount = 2,
        securityLabel = "Critical",
        testCoverage = 34,
        architectureGrade = "B+",
        dependencyUpdates = 5,
    )

    private fun stubSessions() = listOf(
        ChatSessionSummary(
            id = "1",
            title = "How do I implement pagination?",
            projectName = "DevOS AI",
            relativeTime = "2 hours ago",
            iconEmoji = "💬",
        ),
        ChatSessionSummary(
            id = "2",
            title = "Explain this ViewModel pattern",
            projectName = "Compose Lib",
            relativeTime = "Yesterday",
            iconEmoji = "🤖",
        ),
    )
}
