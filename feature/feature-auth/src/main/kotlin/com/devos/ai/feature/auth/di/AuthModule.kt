package com.devos.ai.feature.auth.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.devos.ai.core.security.SecureTokenRepository
import com.devos.ai.core.security.SecureTokenRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** DataStore delegate — one instance per app process. */
private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "devos_prefs",
)

/**
 * Hilt module that provides DataStore<Preferences> and binds SecureTokenRepository
 * for the auth feature.
 *
 * Converted to abstract class (required for @Binds); @Provides methods moved to
 * a companion object so they can coexist with @Binds methods in the same module.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindSecureTokenRepository(
        impl: SecureTokenRepositoryImpl,
    ): SecureTokenRepository

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.authDataStore
    }
}
