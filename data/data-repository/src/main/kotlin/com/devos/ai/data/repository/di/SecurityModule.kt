package com.devos.ai.data.repository.di

import com.devos.ai.data.repository.security.SecurityRepositoryImpl
import com.devos.ai.domain.repository.SecurityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the [SecurityRepository] domain interface to its
 * [SecurityRepositoryImpl] data-layer implementation.
 *
 * Feature modules must inject [SecurityRepository] only; they must NEVER
 * import [SecurityRepositoryImpl] or any other class from `data-repository`.
 *
 * DEVOS-047 / DA-58
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindSecurityRepository(
        impl: SecurityRepositoryImpl,
    ): SecurityRepository
}
