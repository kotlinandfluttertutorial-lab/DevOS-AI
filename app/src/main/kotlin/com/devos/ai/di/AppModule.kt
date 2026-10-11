package com.devos.ai.di

import com.devos.ai.BuildConfig
import com.devos.ai.core.common.di.DefaultDispatcher
import com.devos.ai.core.common.di.IoDispatcher
import com.devos.ai.core.common.di.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /**
     * Provides the active backend base URL to all modules that cannot import
     * [BuildConfig] directly (e.g. core-network, data-*).
     *
     * The value is set per product flavor in [app/build.gradle.kts]:
     *  - local      → http://10.0.2.2:8000/v1/
     *  - staging    → https://api-staging.devos.ai/v1/
     *  - production → https://api.devos.ai/v1/
     */
    @Provides
    @Singleton
    @Named("baseUrl")
    fun provideBaseUrl(): String = BuildConfig.BASE_URL
}
