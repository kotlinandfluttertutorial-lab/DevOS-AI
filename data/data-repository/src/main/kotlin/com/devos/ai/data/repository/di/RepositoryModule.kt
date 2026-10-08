package com.devos.ai.data.repository.di

import com.devos.ai.data.repository.RepositoryRepositoryImpl
import com.devos.ai.domain.repository.RepositoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [RepositoryRepository] to [RepositoryRepositoryImpl].
 *
 * Feature modules declare a dependency on [RepositoryRepository]; Hilt
 * resolves it to this implementation at compile time.  Feature modules
 * must never import [RepositoryRepositoryImpl] directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRepositoryRepository(
        impl: RepositoryRepositoryImpl,
    ): RepositoryRepository
}
