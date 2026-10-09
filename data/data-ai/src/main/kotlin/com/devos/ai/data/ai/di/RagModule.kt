package com.devos.ai.data.ai.di

import com.devos.ai.data.ai.rag.ChunkingWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

/**
 * Hilt module that exposes RAG infrastructure bindings.
 *
 * Provides the [ChunkingWorker] class reference as a Named binding so that
 * modules in `:data:data-repository` (which cannot directly import data-ai)
 * can enqueue the worker via WorkManager without a circular dependency.
 *
 * Usage in RepositoryRepositoryImpl:
 * ```kotlin
 * @Inject @Named("ChunkingWorkerClass")
 * lateinit var chunkingWorkerClass: Class<out ListenableWorker>
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
object RagModule {

    @Provides
    @Named("ChunkingWorkerClass")
    fun provideChunkingWorkerClass(): Class<out androidx.work.ListenableWorker> =
        ChunkingWorker::class.java
}
