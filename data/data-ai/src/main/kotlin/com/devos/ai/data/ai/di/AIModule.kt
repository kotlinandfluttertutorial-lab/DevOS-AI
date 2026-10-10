package com.devos.ai.data.ai.di

import com.devos.ai.data.ai.repository.RAGRepositoryImpl
import com.devos.ai.domain.ai.repository.RAGRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding RAG-related repository interfaces.
 *
 * [AIRepository] is bound in [AIProviderModule] because it depends on
 * the AI provider multibinding that is also defined there.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @Singleton
    abstract fun bindRAGRepository(impl: RAGRepositoryImpl): RAGRepository
}
