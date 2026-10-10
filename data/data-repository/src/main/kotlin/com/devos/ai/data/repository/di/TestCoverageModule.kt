package com.devos.ai.data.repository.di

import com.devos.ai.data.repository.testing.JaCoCoParser
import com.devos.ai.data.repository.testing.TestCoverageRepositoryImpl
import com.devos.ai.domain.repository.TestCoverageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [TestCoverageRepository] to its implementation and
 * provides [JaCoCoParser] as a scoped dependency.
 *
 * Feature modules must inject [TestCoverageRepository] only; they must NEVER
 * import [TestCoverageRepositoryImpl] or any other class from `data-repository`.
 *
 * DEVOS-049 / DA-60
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TestCoverageModule {

    @Binds
    @Singleton
    abstract fun bindTestCoverageRepository(
        impl: TestCoverageRepositoryImpl,
    ): TestCoverageRepository

    companion object {
        @Provides
        @Singleton
        fun provideJaCoCoParser(): JaCoCoParser = JaCoCoParser()
    }
}
