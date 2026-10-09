package com.devos.ai.feature.security

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

/** UI state for the Security Findings screen. */
sealed interface SecurityFindingsUiState {
    data object Loading : SecurityFindingsUiState

    data class Success(
        val findings: List<SecurityFinding>,
        val activeFilter: Severity?,
        val counts: Map<Severity, Int>,
    ) : SecurityFindingsUiState

    data object Empty : SecurityFindingsUiState

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : SecurityFindingsUiState
}

/** One-shot navigation events emitted by [SecurityFindingsViewModel]. */
sealed class SecurityNavEvent {
    data object NavigateBack : SecurityNavEvent()
    data class NavigateToAIChat(val context: String) : SecurityNavEvent()
}

/**
 * ViewModel for [SecurityFindingsScreen].
 *
 * Loads stub findings matching the #s-security mockup. Supports:
 * - Severity filter (null = All)
 * - Mark fixed (removes finding from list)
 * - Ask AI (emits nav event with finding context)
 *
 * Navigation events emitted via [SharedFlow] — NavController never imported here.
 *
 * DEVOS-046 / DA-59
 */
@HiltViewModel
class SecurityFindingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<SecurityFindingsUiState>(SecurityFindingsUiState.Loading)
    val uiState: StateFlow<SecurityFindingsUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<SecurityNavEvent>()
    val navEvent: SharedFlow<SecurityNavEvent> = _navEvent.asSharedFlow()

    /** Full unfiltered list; used for filter application. */
    private var allFindings: List<SecurityFinding> = emptyList()

    /** Active severity filter; null means "All". */
    private var activeFilter: Severity? = null

    init {
        loadFindings()
    }

    private fun loadFindings() {
        viewModelScope.launch {
            allFindings = listOf(
                SecurityFinding(
                    id = "sf-1",
                    title = "SQL Injection Risk",
                    severity = Severity.CRITICAL,
                    filePath = "DatabaseHelper.kt",
                    lineNumber = 84,
                    owaspCategory = "OWASP A03:2021 · Injection",
                    aiSuggestion = "Use parameterized queries to fix",
                ),
                SecurityFinding(
                    id = "sf-2",
                    title = "Hardcoded API Key",
                    severity = Severity.CRITICAL,
                    filePath = "Config.kt",
                    lineNumber = 12,
                    owaspCategory = "OWASP A02:2021 · Crypto Failures",
                    aiSuggestion = "Move to BuildConfig or EncryptedSharedPreferences",
                ),
                SecurityFinding(
                    id = "sf-3",
                    title = "Missing HTTPS enforcement",
                    severity = Severity.HIGH,
                    filePath = "NetworkModule.kt",
                    lineNumber = 31,
                    owaspCategory = "OWASP A02:2021 · Network",
                    aiSuggestion = "Enforce HTTPS via OkHttp CertificatePinner",
                ),
            )
            applyFilter(activeFilter)
        }
    }

    /** Change the active severity filter; null = show all. */
    fun onFilterChange(severity: Severity?) {
        activeFilter = severity
        applyFilter(severity)
    }

    /** Mark a finding as fixed — removes it from the displayed list. */
    fun markFixed(id: String) {
        allFindings = allFindings.filterNot { it.id == id }
        applyFilter(activeFilter)
    }

    /** Emit nav event to open AI Chat pre-filled with finding context. */
    fun onAskAI(id: String) {
        val finding = allFindings.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            _navEvent.emit(SecurityNavEvent.NavigateToAIChat("Security: ${finding.title} in ${finding.filePath}:${finding.lineNumber}"))
        }
    }

    /** Emit navigate-back event. */
    fun navigateBack() {
        viewModelScope.launch { _navEvent.emit(SecurityNavEvent.NavigateBack) }
    }

    /** Retry loading after an error. */
    fun retry() {
        _uiState.value = SecurityFindingsUiState.Loading
        loadFindings()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun applyFilter(filter: Severity?) {
        val filtered = if (filter == null) allFindings else allFindings.filter { it.severity == filter }
        val counts = Severity.entries.associateWith { sev -> allFindings.count { it.severity == sev } }

        _uiState.value = if (filtered.isEmpty() && allFindings.isEmpty()) {
            SecurityFindingsUiState.Empty
        } else {
            SecurityFindingsUiState.Success(
                findings = filtered,
                activeFilter = filter,
                counts = counts,
            )
        }
    }
}
