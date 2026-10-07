package com.devos.ai.feature.auth.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.devos.ai.feature.auth.repository.AuthRepository
import com.devos.ai.feature.auth.repository.AuthRepositoryImpl
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
 * Hilt module for the auth feature.
 *
 * Provides:
 *  - [DataStore<Preferences>] for onboarding/auth flags.
 *  - Binds [AuthRepository] to [AuthRepositoryImpl] (stub awaiting DEVOS-041).
 *
 * Note: [SecureTokenRepository] is bound in [com.devos.ai.core.security.SecurityModule]
 * (core-security module) to keep the binding at the correct layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {

        @Provides
        @Singleton
        fun provideDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.authDataStore
    }
}
