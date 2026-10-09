package com.devos.ai.feature.settings.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.domain.ai.model.AIProvider
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
 * ViewModel for the Provider Settings screen (DEVOS-034 / DA-45).
 *
 * Manages the display state for each provider card:
 * - Loads saved configuration from [AIProviderRepository] (injected in future — uses stub data now)
 * - Handles key save + test connection flow
 * - Emits one-shot [ProviderSettingsNavEvent]s for navigation
 *
 * TODO(DEVOS-034 follow-up): inject AIProviderRepository to load real key status.
 * The stub data mirrors the `#s-provider-settings` mockup exactly.
 */
@HiltViewModel
class ProviderSettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<ProviderSettingsUiState>(ProviderSettingsUiState.Loading)
    val uiState: StateFlow<ProviderSettingsUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ProviderSettingsNavEvent>()
    val navEvent: SharedFlow<ProviderSettingsNavEvent> = _navEvent.asSharedFlow()

    init {
        loadProviders()
    }

    fun loadProviders() {
        viewModelScope.launch {
            _uiState.value = ProviderSettingsUiState.Loading
            // Stub data matching the #s-provider-settings mockup
            _uiState.value = ProviderSettingsUiState.Success(
                providers = stubProviders(),
            )
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch { _navEvent.emit(ProviderSettingsNavEvent.NavigateBack) }
    }

    /**
     * Called when user taps "Edit" on a configured provider.
     * Opens the key-input inline (sets [ProviderCardState.isEditingKey] = true).
     */
    fun onEditKey(provider: AIProvider) {
        updateCard(provider) { it.copy(isEditingKey = true, connectionError = null) }
    }

    /**
     * Called when user taps "Configure" on an unconfigured provider.
     */
    fun onConfigureProvider(provider: AIProvider) {
        updateCard(provider) { it.copy(isEditingKey = true) }
    }

    /**
     * Saves the typed [apiKey] for [provider] and triggers a test connection.
     * On success marks the provider as connected; on failure shows [connectionError].
     */
    fun onSaveKey(provider: AIProvider, apiKey: String) {
        if (apiKey.isBlank()) return
        updateCard(provider) { it.copy(isEditingKey = false, isTestingConnection = true) }

        viewModelScope.launch {
            // Stub: simulate test connection — succeed for non-empty keys
            kotlinx.coroutines.delay(800)
            val masked = buildMaskedKey(provider, apiKey)
            updateCard(provider) {
                it.copy(
                    isConfigured      = true,
                    maskedKey         = masked,
                    isTestingConnection = false,
                    connectionError   = null,
                )
            }
        }
    }

    /**
     * Cancels the key editing state for [provider] without saving.
     */
    fun onCancelEdit(provider: AIProvider) {
        updateCard(provider) { it.copy(isEditingKey = false, connectionError = null) }
    }

    /**
     * Triggers a test connection for [provider] using the already-saved key.
     */
    fun onTestConnection(provider: AIProvider) {
        updateCard(provider) { it.copy(isTestingConnection = true, connectionError = null) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            // Stub: always succeed
            updateCard(provider) { it.copy(isTestingConnection = false) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun updateCard(
        provider: AIProvider,
        transform: (ProviderCardState) -> ProviderCardState,
    ) {
        val current = _uiState.value as? ProviderSettingsUiState.Success ?: return
        _uiState.value = current.copy(
            providers = current.providers.map { card ->
                if (card.provider == provider) transform(card) else card
            },
        )
    }

    private fun buildMaskedKey(provider: AIProvider, key: String): String {
        val prefix = when (provider) {
            AIProvider.OPENAI    -> "sk-proj-"
            AIProvider.ANTHROPIC -> "sk-ant-api03-"
            AIProvider.GEMINI    -> "AIzaSy"
            AIProvider.OLLAMA    -> ""
        }
        val dots = "•".repeat(24)
        return if (provider == AIProvider.OLLAMA) key   // show URL as-is
        else "$prefix$dots"
    }

    // ── Stub data — matches #s-provider-settings mockup ──────────────────────

    private fun stubProviders() = listOf(
        ProviderCardState(
            provider      = AIProvider.ANTHROPIC,
            isConfigured  = true,
            maskedKey     = "sk-ant-api03-••••••••••••••••••••••••",
            selectedModel = "claude-sonnet-4.5",
            capabilities  = "All features",
        ),
        ProviderCardState(
            provider      = AIProvider.OPENAI,
            isConfigured  = true,
            maskedKey     = "sk-proj-••••••••••••••••••••••••••",
            selectedModel = "gpt-4o",
            capabilities  = "Chat + Embeddings",
        ),
        ProviderCardState(
            provider      = AIProvider.GEMINI,
            isConfigured  = false,
            maskedKey     = null,
            selectedModel = "gemini-2.0-flash",
            capabilities  = "Chat + Vision",
        ),
        ProviderCardState(
            provider      = AIProvider.OLLAMA,
            isConfigured  = false,
            maskedKey     = null,
            selectedModel = "llama3.2",
            capabilities  = "Local · No API key",
        ),
    )
}
