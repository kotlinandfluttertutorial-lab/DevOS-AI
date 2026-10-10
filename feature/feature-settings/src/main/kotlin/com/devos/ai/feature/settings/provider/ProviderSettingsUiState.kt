package com.devos.ai.feature.settings.provider

import com.devos.ai.domain.ai.model.AIProvider

/**
 * UI state for the Provider Settings screen (DEVOS-034).
 *
 * Follows the DevOS 4-state pattern: Loading | Success | Empty | Error.
 */
sealed interface ProviderSettingsUiState {
    data object Loading : ProviderSettingsUiState
    data class Success(val providers: List<ProviderCardState>) : ProviderSettingsUiState
    data object Empty : ProviderSettingsUiState
    data class Error(val message: String, val retryable: Boolean) : ProviderSettingsUiState
}

/**
 * Display state for a single provider card.
 *
 * @param provider       The AI provider this card represents
 * @param isConfigured   True when a non-empty API key / URL is stored
 * @param maskedKey      Masked representation, e.g. "sk-ant-api03-••••••••"  (null if not configured)
 * @param selectedModel  Currently selected model display name, e.g. "claude-sonnet-4.5"
 * @param capabilities   Short capability tag, e.g. "Chat + Code" (shown as subtitle)
 * @param isTestingConnection True while a testConnection call is in flight
 * @param connectionError Error message from the last failed testConnection (null = none)
 * @param isEditingKey   True when the key-input bottom sheet / dialog is visible
 */
data class ProviderCardState(
    val provider: AIProvider,
    val isConfigured: Boolean,
    val maskedKey: String?,
    val selectedModel: String,
    val capabilities: String,
    val isTestingConnection: Boolean = false,
    val connectionError: String? = null,
    val isEditingKey: Boolean = false,
)

/** One-shot navigation events emitted by [ProviderSettingsViewModel]. */
sealed class ProviderSettingsNavEvent {
    data object NavigateBack : ProviderSettingsNavEvent()
}
