package com.devos.ai.data.ai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.devos.ai.data.ai.provider.AIProviderClient
import com.devos.ai.data.ai.provider.AnthropicClient
import com.devos.ai.data.ai.provider.GeminiClient
import com.devos.ai.data.ai.provider.OllamaClient
import com.devos.ai.data.ai.provider.OpenAIClient
import com.devos.ai.data.ai.repository.AIProviderRepositoryImpl
import com.devos.ai.data.ai.repository.AIRepositoryImpl
import com.devos.ai.domain.ai.model.AIProvider
import com.devos.ai.domain.ai.repository.AIProviderRepository
import com.devos.ai.domain.ai.repository.AIRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing AI provider infrastructure.
 *
 * ## OkHttpClient
 * A dedicated [OkHttpClient] with a 60 s read timeout is provided for
 * streaming AI responses — longer than the default 10 s to allow first-token latency.
 *
 * ## Provider multibinding
 * Each [AIProviderClient] is bound into a `Map<AIProvider, AIProviderClient>` so
 * [AIRepositoryImpl] can dispatch to the active provider without `when` statements.
 *
 * ## DataStore
 * A dedicated DataStore named "ai_prefs" stores active provider, model selection,
 * and generation parameters. API keys go to [SecureTokenRepository] — never here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AIProviderModule {

    // ── @Binds ────────────────────────────────────────────────────────────────

    @Binds @Singleton
    abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository

    @Binds @Singleton
    abstract fun bindAIProviderRepository(impl: AIProviderRepositoryImpl): AIProviderRepository

    // ── Provider multibinding via @Binds + @IntoMap ───────────────────────────

    @Binds @IntoMap
    @AIProviderKey(AIProvider.OPENAI)
    abstract fun bindOpenAI(client: OpenAIClient): AIProviderClient

    @Binds @IntoMap
    @AIProviderKey(AIProvider.ANTHROPIC)
    abstract fun bindAnthropic(client: AnthropicClient): AIProviderClient

    @Binds @IntoMap
    @AIProviderKey(AIProvider.GEMINI)
    abstract fun bindGemini(client: GeminiClient): AIProviderClient

    @Binds @IntoMap
    @AIProviderKey(AIProvider.OLLAMA)
    abstract fun bindOllama(client: OllamaClient): AIProviderClient

    companion object {

        @Provides @Singleton
        @Named("ai_http_client")
        fun provideAIHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)   // streaming responses can be slow
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()

        @Provides @Singleton
        @Named("ai_prefs")
        fun provideAIPrefsDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
            File(context.filesDir, "ai_settings.preferences_pb")
        }
    }
}
