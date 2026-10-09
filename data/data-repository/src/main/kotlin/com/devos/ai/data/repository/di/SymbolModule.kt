package com.devos.ai.data.repository.di

import com.devos.ai.data.repository.SymbolRepositoryImpl
import com.devos.ai.domain.repository.SymbolRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [SymbolRepository] to [SymbolRepositoryImpl].
 *
 * Feature modules declare a dependency on [SymbolRepository]; Hilt resolves
 * it to this implementation at compile time. Feature modules must never
 * import [SymbolRepositoryImpl] directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SymbolModule {

    @Binds
    @Singleton
    abstract fun bindSymbolRepository(
        impl: SymbolRepositoryImpl,
    ): SymbolRepository
}
