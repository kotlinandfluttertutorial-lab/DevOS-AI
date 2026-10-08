package com.devos.ai.data.repository.di

import com.devos.ai.data.repository.RepositoryRepositoryImpl
import com.devos.ai.domain.repository.RepositoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the [RepositoryRepository] domain interface to its
 * [RepositoryRepositoryImpl] data-layer implementation.
 *
 * Installed in [SingletonComponent] so the repository — and its WorkManager
 * reference — lives for the full application lifetime.
 *
 * Feature modules must inject [RepositoryRepository] only; they must NEVER
 * import [RepositoryRepositoryImpl] or any other class from `data-repository`.
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
