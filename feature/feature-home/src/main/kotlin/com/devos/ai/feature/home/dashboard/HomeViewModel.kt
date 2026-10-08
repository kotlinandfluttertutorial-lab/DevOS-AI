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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/**
 * ViewModel for the Home Dashboard screen.
 *
 * Loads stub dashboard data and handles user interactions by emitting [HomeNavEvent]s.
 * Dismissed recommendations are persisted to DataStore so they survive app restarts.
 *
 * Computes [HomeUiState.Success.greeting] and [HomeUiState.Success.dateLabel] dynamically
 * from the current wall-clock time so the UI always shows the correct time-of-day salutation
 * and formatted date matching the `#s-home` mockup.
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

        /** Formatter for the date label: "Wednesday, Oct 7" */
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH)
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<HomeNavEvent>()
    val navEvent: SharedFlow<HomeNavEvent> = _navEvent.asSharedFlow()

    init {
        loadDashboard()
    }

    /** Loads the dashboard. Can be called again on retry. */
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

                // TODO(DEVOS-058): replace with GetDashboardUseCase
                val projects     = stubProjects()
                val filteredRecs = stubRecommendations().filter { it.id !in dismissed }
                val now          = LocalDateTime.now()

                if (projects.isEmpty()) {
                    _uiState.value = HomeUiState.Empty
                } else {
                    _uiState.value = HomeUiState.Success(
                        recentProjects  = projects,
                        recommendations = filteredRecs,
                        health          = stubProjectHealth(),
                        recentSessions  = stubSessions(),
                        greeting        = buildGreeting(now),
                        dateLabel       = DATE_FORMATTER.format(now),
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    message   = e.message ?: "Failed to load dashboard",
                    retryable = true,
                )
            }
        }
    }

    /**
     * Dismisses a recommendation: removes it from current state immediately
     * and persists the ID to DataStore so it is filtered on next load.
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
            val current = _uiState.value
            if (current is HomeUiState.Success) {
                _uiState.value = current.copy(
                    recommendations = current.recommendations.filter { it.id != id },
                )
            }
        }
    }

    // ── Navigation event emitters ─────────────────────────────────────────────

    fun onSearchTap()                       = emit(HomeNavEvent.NavigateToSearch)
    fun onProjectTap(id: String)            = emit(HomeNavEvent.NavigateToProject(id))
    fun onSessionTap(sessionId: String)     = emit(HomeNavEvent.NavigateToChat(sessionId))
    fun onNotificationTap()                 = emit(HomeNavEvent.NavigateToNotifications)
    fun onProfileTap()                      = emit(HomeNavEvent.NavigateToProfile)
    fun onImportTap()                       = emit(HomeNavEvent.NavigateToImport)
    fun onSeeAllProjectsTap()               = emit(HomeNavEvent.NavigateToProjectList)
    fun onSeeAllSessionsTap()               = emit(HomeNavEvent.NavigateToAllSessions)

    private fun emit(event: HomeNavEvent) {
        viewModelScope.launch { _navEvent.emit(event) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns "Good morning / afternoon / evening, Dev 👋" based on the hour.
     * Matches the greeting shown in the `#s-home` mockup.
     */
    private fun buildGreeting(now: LocalDateTime): String {
        val salutation = when (now.hour) {
            in 5..11  -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else      -> "Good night"
        }
        return "$salutation, Dev \uD83D\uDC4B"
    }

    // ── Stub data — remove after DEVOS-058 ───────────────────────────────────

    private fun stubProjects() = listOf(
        ProjectSummary("p1", "DevOS AI",    "Kotlin", HealthStatus.HEALTHY),
        ProjectSummary("p2", "Compose Lib", "Kotlin", HealthStatus.WARNING),
        ProjectSummary("p3", "SDK Tools",   "Kotlin", HealthStatus.CRITICAL),
    )

    private fun stubRecommendations() = listOf(
        AIRecommendation(
            id          = "r1",
            type        = RecommendationType.SECURITY,
            title       = "Security: SQL Injection Risk",
            description = "Found in DatabaseHelper.kt · High severity",
        ),
        AIRecommendation(
            id          = "r2",
            type        = RecommendationType.TESTS,
            title       = "Add tests for AuthViewModel",
            description = "Coverage dropped to 34% · 3 untested functions",
        ),
        AIRecommendation(
            id          = "r3",
            type        = RecommendationType.LEARNING,
            title       = "Learn: Kotlin Coroutines",
            description = "Based on recent code patterns · 4 lessons",
        ),
    )

    private fun stubProjectHealth() = ProjectHealth(
        securityCount     = 2,
        securityLabel     = "Critical findings",
        testCoverage      = 67,
        architectureGrade = "A",
        dependencyUpdates = 3,
    )

    private fun stubSessions() = listOf(
        ChatSessionSummary(
            id           = "1",
            title        = "Explain RepositoryViewModel",
            projectName  = "DevOS AI",
            relativeTime = "2 hours ago",
            iconEmoji    = "✨",
        ),
        ChatSessionSummary(
            id           = "2",
            title        = "Debug navigation back stack issue",
            projectName  = "Compose Lib",
            relativeTime = "Yesterday",
            iconEmoji    = "\uD83D\uDC1B",
        ),
    )
}
