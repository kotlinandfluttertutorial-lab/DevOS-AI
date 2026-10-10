package com.devos.ai.domain.ai.repository

import com.devos.ai.domain.ai.model.AIModel
import com.devos.ai.domain.ai.model.AIProvider
import com.devos.ai.domain.ai.model.ProviderConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for AI provider configuration.
 *
 * API keys are always stored via [SecureTokenRepository] — never in plain text.
 * This interface is bound in [AIProviderModule] and injected into screens via
 * [AISettingsViewModel] (DEVOS-033) and [ProviderSettingsViewModel] (DEVOS-034).
 *
 * Pure Kotlin — zero Android imports.
 */
interface AIProviderRepository {

    // ── Config ────────────────────────────────────────────────────────────────

    /** Emits the active provider config; re-emits when it changes. */
    fun observeActiveConfig(): Flow<ProviderConfig>

    /** Returns the currently active provider config. */
    suspend fun getActiveConfig(): ProviderConfig

    /** Sets [provider] as the active provider and saves the choice. */
    suspend fun setActiveProvider(provider: AIProvider)

    /** Updates the selected model for [provider]. */
    suspend fun setSelectedModel(provider: AIProvider, model: AIModel)

    /** Updates temperature and maxTokens for [provider]. */
    suspend fun updateGenerationParams(
        provider: AIProvider,
        temperature: Float,
        maxTokens: Int,
    )

    // ── API Keys ──────────────────────────────────────────────────────────────

    /**
     * Saves [apiKeyOrUrl] for [provider] in EncryptedSharedPreferences.
     *
     * For Ollama this is the base URL (e.g. "http://192.168.1.10:11434").
     * The raw value is NEVER logged — mask as `value.take(4)+"****"`.
     */
    suspend fun saveApiKey(provider: AIProvider, apiKeyOrUrl: String)

    /**
     * Returns true when a non-empty API key / URL is stored for [provider].
     */
    suspend fun hasApiKey(provider: AIProvider): Boolean

    /** Clears the stored API key for [provider]. */
    suspend fun clearApiKey(provider: AIProvider)

    // ── Models ────────────────────────────────────────────────────────────────

    /** Returns available models for [provider] (from defaults + optional API fetch). */
    suspend fun getModels(provider: AIProvider): List<AIModel>

    // ── Connectivity test ─────────────────────────────────────────────────────

    /**
     * Sends a minimal ping request to [provider] to verify the stored key works.
     *
     * @return `Result.success(Unit)` on 200, `Result.failure` with a user-facing
     *         message on auth error or network failure.
     */
    suspend fun testConnection(provider: AIProvider): Result<Unit>
}
