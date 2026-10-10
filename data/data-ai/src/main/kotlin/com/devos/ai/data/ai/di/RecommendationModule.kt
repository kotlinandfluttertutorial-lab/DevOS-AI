package com.devos.ai.data.ai.di

import com.devos.ai.domain.ai.usecase.LearningSignalProvider
import com.devos.ai.domain.ai.usecase.SecuritySignalProvider
import com.devos.ai.domain.ai.usecase.TestCoverageSignalProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides signal providers for [GetAIRecommendationsUseCase].
 *
 * The use case itself uses constructor injection, so it does not need a binding here.
 * Signal providers are singletons since they are stateless.
 *
 * DEVOS-058 / DA-70
 */
@Module
@InstallIn(SingletonComponent::class)
object RecommendationModule {

    @Provides
    @Singleton
    fun provideSecuritySignalProvider(): SecuritySignalProvider = SecuritySignalProvider()

    @Provides
    @Singleton
    fun provideTestCoverageSignalProvider(): TestCoverageSignalProvider =
        TestCoverageSignalProvider()

    @Provides
    @Singleton
    fun provideLearningSignalProvider(): LearningSignalProvider = LearningSignalProvider()
}
