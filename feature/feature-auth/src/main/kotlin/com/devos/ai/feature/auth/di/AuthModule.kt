package com.devos.ai.feature.auth.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
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
 * Hilt module that provides DataStore<Preferences> for the auth feature.
 *
 * SecureTokenRepository binding will be added in DEVOS-012 once core-security
 * provides the implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.authDataStore
}
