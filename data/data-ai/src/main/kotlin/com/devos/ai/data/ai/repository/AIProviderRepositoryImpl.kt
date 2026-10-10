package com.devos.ai.data.ai.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.core.security.TokenKey
import com.devos.ai.data.ai.provider.AIProviderClient
import com.devos.ai.domain.ai.model.AIModel
import com.devos.ai.domain.ai.model.AIProvider
import com.devos.ai.domain.ai.model.DefaultModels
import com.devos.ai.domain.ai.model.ProviderConfig
import com.devos.ai.domain.ai.repository.AIProviderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Concrete implementation of [AIProviderRepository].
 *
 * API keys stored via [SecureTokenRepository] — never logged raw.
 * Active provider/model/params stored in a named DataStore.
 */
@Singleton
class AIProviderRepositoryImpl @Inject constructor(
    @Named("ai_prefs") private val dataStore: DataStore<Preferences>,
    private val secureTokenRepository: SecureTokenRepository,
    private val providerClients: Map<AIProvider, @JvmSuppressWildcards AIProviderClient>,
) : AIProviderRepository {

    companion object {
        private val KEY_ACTIVE_PROVIDER  = stringPreferencesKey("active_provider")
        private val KEY_TEMPERATURE      = floatPreferencesKey("temperature")
        private val KEY_MAX_TOKENS       = intPreferencesKey("max_tokens")
        private fun modelKey(p: AIProvider) = stringPreferencesKey("model_${p.name}")
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    override fun observeActiveConfig(): Flow<ProviderConfig> =
        dataStore.data.map { prefs -> buildConfig(prefs) }

    override suspend fun getActiveConfig(): ProviderConfig =
        buildConfig(dataStore.data.first())

    // ── Provider / model selection ────────────────────────────────────────────

    override suspend fun setActiveProvider(provider: AIProvider) {
        dataStore.edit { it[KEY_ACTIVE_PROVIDER] = provider.name }
    }

    override suspend fun setSelectedModel(provider: AIProvider, model: AIModel) {
        dataStore.edit { it[modelKey(provider)] = model.id }
    }

    override suspend fun updateGenerationParams(
        provider: AIProvider,
        temperature: Float,
        maxTokens: Int,
    ) {
        dataStore.edit {
            it[KEY_TEMPERATURE] = temperature
            it[KEY_MAX_TOKENS]  = maxTokens
        }
    }

    // ── API keys ──────────────────────────────────────────────────────────────

    override suspend fun saveApiKey(provider: AIProvider, apiKeyOrUrl: String) {
        // SECURITY: never log raw value
        Timber.d("Saving key for %s: %s****", provider.displayName, apiKeyOrUrl.take(4))
        secureTokenRepository.saveToken(provider.asTokenKey(), apiKeyOrUrl)
    }

    override suspend fun hasApiKey(provider: AIProvider): Boolean =
        secureTokenRepository.hasToken(provider.asTokenKey())

    override suspend fun clearApiKey(provider: AIProvider) {
        secureTokenRepository.clearToken(provider.asTokenKey())
    }

    // ── Models ────────────────────────────────────────────────────────────────

    override suspend fun getModels(provider: AIProvider): List<AIModel> {
        val prefs  = dataStore.data.first()
        val config = buildConfig(prefs, provider)
        val apiKey = secureTokenRepository.getToken(provider.asTokenKey()) ?: ""
        return providerClients[provider]?.listModels(config, apiKey)
            ?: DefaultModels.forProvider(provider)
    }

    // ── Test connection ───────────────────────────────────────────────────────

    override suspend fun testConnection(provider: AIProvider): Result<Unit> {
        val apiKey = secureTokenRepository.getToken(provider.asTokenKey())
            ?: return Result.failure(Exception("No API key saved for ${provider.displayName}"))
        val config = buildConfig(dataStore.data.first(), provider)
        return providerClients[provider]
            ?.testConnection(config, apiKey)
            ?.map { Unit }
            ?: Result.failure(Exception("No client registered for ${provider.displayName}"))
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun buildConfig(prefs: Preferences): ProviderConfig {
        val provider = prefs[KEY_ACTIVE_PROVIDER]
            ?.let { runCatching { AIProvider.valueOf(it) }.getOrNull() }
            ?: AIProvider.OPENAI
        return buildConfig(prefs, provider)
    }

    private suspend fun buildConfig(prefs: Preferences, provider: AIProvider): ProviderConfig {
        val modelId = prefs[modelKey(provider)]
        val model   = DefaultModels.forProvider(provider)
            .find { it.id == modelId } ?: DefaultModels.defaultFor(provider)
        return ProviderConfig(
            provider       = provider,
            selectedModel  = model,
            isConfigured   = secureTokenRepository.hasToken(provider.asTokenKey()),
            temperature    = prefs[KEY_TEMPERATURE] ?: 0.7f,
            maxTokens      = prefs[KEY_MAX_TOKENS]  ?: 4_096,
        )
    }

    // ── Package-internal helpers used by ReActEngine ──────────────────────────

    /** Returns the stored API key for the given provider (raw — handle with care). */
    internal suspend fun getApiKey(provider: AIProvider): String? =
        secureTokenRepository.getToken(provider.asTokenKey())

    /** Returns the registered provider client for [provider], or null. */
    internal fun getClient(provider: AIProvider): AIProviderClient? =
        providerClients[provider]
}

// ── AIProvider as TokenKey ─────────────────────────────────────────────────────

private fun AIProvider.asTokenKey(): TokenKey = object : TokenKey {
    override val prefKey = this@asTokenKey.prefKey
    override val name    = this@asTokenKey.displayName
}
