package com.devos.ai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.dao.SymbolDao
import com.devos.ai.core.database.entity.FileEntity
import com.devos.ai.core.database.entity.RepositoryEntity
import com.devos.ai.core.database.entity.SymbolEntity

/**
 * Central Room database for DevOS AI.
 *
 * **Version history:**
 * | Version | Ticket    | Change                                               |
 * |---------|-----------|------------------------------------------------------|
 * | 1       | DEVOS-015 | Initial schema: repositories, repository_files, symbols |
 *
 * Rules:
 * - Never use `fallbackToDestructiveMigration()` in production builds.
 * - Every schema change MUST add an explicit [Migration] to [ALL_MIGRATIONS]
 *   and bump [version].
 * - Schema JSON files are auto-exported to `core-database/schemas/` via the
 *   Room Gradle plugin (configured in build.gradle.kts).
 */
@Database(
    entities = [
        RepositoryEntity::class,
        FileEntity::class,
        SymbolEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DevOSDatabase : RoomDatabase() {

    abstract fun repositoryDao(): RepositoryDao
    abstract fun fileDao(): FileDao
    abstract fun symbolDao(): SymbolDao

    companion object {
        const val DATABASE_NAME = "devos.db"

        /**
         * Ordered list of all explicit migrations.
         *
         * Pass this to `Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS)`.
         *
         * Version 1 is the initial schema — no migration needed from 0→1
         * (Room creates the tables automatically on a fresh install).
         * Add future migrations here as:
         *
         * ```kotlin
         * val MIGRATION_1_2 = object : Migration(1, 2) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE repositories ADD COLUMN isStarred INTEGER NOT NULL DEFAULT 0")
         *     }
         * }
         * ```
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            // No migrations yet — version 1 is the baseline schema.
        )
    }
}
