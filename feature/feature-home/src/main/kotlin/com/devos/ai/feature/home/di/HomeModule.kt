package com.devos.ai.feature.home.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/** DataStore delegate for home preferences — separate file from auth's "devos_prefs". */
private val Context.homeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "home_prefs",
)

/**
 * Provides the [DataStore] instance used by [HomeViewModel].
 *
 * Uses a `@Named("home")` qualifier to avoid conflicts with [AuthModule]'s unqualified
 * [DataStore<Preferences>] binding. Both DataStore instances live in different files on disk.
 */
@Module
@InstallIn(SingletonComponent::class)
object HomeModule {

    @Provides
    @Singleton
    @Named("home")
    fun provideHomeDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.homeDataStore
}
