package com.devos.ai.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Forward declaration — DevOSDatabase will be implemented in :core:core-database
// This module wires it at the app level once the database is created.

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Database, OkHttp client, and other app-level singletons
    // will be provided here once core modules are implemented.
    //
    // Example:
    // @Provides @Singleton
    // fun provideDatabase(@ApplicationContext context: Context): DevOSDatabase =
    //     Room.databaseBuilder(context, DevOSDatabase::class.java, "devos.db")
    //         .addMigrations(*ALL_MIGRATIONS)
    //         .build()
}
