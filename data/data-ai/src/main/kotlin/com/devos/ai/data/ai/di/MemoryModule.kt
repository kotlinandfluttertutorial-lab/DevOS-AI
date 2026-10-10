package com.devos.ai.data.ai.di

import com.devos.ai.data.ai.repository.MemoryRepositoryImpl
import com.devos.ai.domain.ai.repository.MemoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [MemoryRepository] to its Room-backed implementation.
 *
 * DEVOS-056 / DA-67
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MemoryModule {

    @Binds
    @Singleton
    abstract fun bindMemoryRepository(impl: MemoryRepositoryImpl): MemoryRepository
}
