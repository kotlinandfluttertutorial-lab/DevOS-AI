package com.devos.ai.network.di

import com.devos.ai.network.DevOSApiService
import com.devos.ai.network.SseStreamClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module that provides the DevOS backend [DevOSApiService] and related
 * networking infrastructure.
 *
 * ## Base URL injection
 * [BuildConfig] is defined in the `app` module and is NOT accessible from
 * `core-network`.  The `app` module (via [AppModule]) provides a
 * `@Named("baseUrl")` [String] that is injected here.
 *
 * ## OkHttpClient
 * A dedicated `@Named("devos_http_client")` client is provided so it does not
 * conflict with the `@Named("ai_http_client")` used by the direct AI provider
 * streaming clients.
 *
 * ## Security
 * No auth header is added in the interceptor — the backend is currently public.
 * VCS tokens are forwarded per-call via Retrofit [@Header] parameters.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @Named("devos_http_client")
    fun provideDevOSHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            Timber.tag("DevOS-HTTP").d(message)
        }.apply {
            // Log full body only in debug builds; level is set at runtime
            level = HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideDevOSApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("devos_http_client") okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): DevOSApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DevOSApiService::class.java)

    @Provides
    @Singleton
    fun provideSseStreamClient(
        @Named("devos_http_client") okHttpClient: OkHttpClient,
    ): SseStreamClient = SseStreamClient(okHttpClient)
}
