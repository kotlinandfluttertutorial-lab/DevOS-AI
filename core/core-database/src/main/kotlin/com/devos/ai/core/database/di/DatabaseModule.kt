package com.devos.ai.core.database.di

import android.content.Context
import androidx.room.Room
import com.devos.ai.core.database.DevOSDatabase
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.dao.SymbolDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the [DevOSDatabase] singleton and its DAOs.
 *
 * Installed in [SingletonComponent] so the database lives for the full
 * application lifetime.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DevOSDatabase =
        Room.databaseBuilder(
            context,
            DevOSDatabase::class.java,
            DevOSDatabase.DATABASE_NAME,
        )
            .addMigrations(*DevOSDatabase.ALL_MIGRATIONS)
            // Never add fallbackToDestructiveMigration() — data loss is unacceptable.
            .build()

    @Provides
    @Singleton
    fun provideRepositoryDao(db: DevOSDatabase): RepositoryDao = db.repositoryDao()

    @Provides
    @Singleton
    fun provideFileDao(db: DevOSDatabase): FileDao = db.fileDao()

    @Provides
    @Singleton
    fun provideSymbolDao(db: DevOSDatabase): SymbolDao = db.symbolDao()
}
