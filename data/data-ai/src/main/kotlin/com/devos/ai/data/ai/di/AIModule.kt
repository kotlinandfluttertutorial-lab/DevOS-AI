package com.devos.ai.data.ai.di

import com.devos.ai.data.ai.repository.AIRepositoryImpl
import com.devos.ai.data.ai.repository.RAGRepositoryImpl
import com.devos.ai.domain.ai.repository.AIRepository
import com.devos.ai.domain.ai.repository.RAGRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds domain repository interfaces to their data implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @Singleton
    abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository

    @Binds
    @Singleton
    abstract fun bindRAGRepository(impl: RAGRepositoryImpl): RAGRepository
}
