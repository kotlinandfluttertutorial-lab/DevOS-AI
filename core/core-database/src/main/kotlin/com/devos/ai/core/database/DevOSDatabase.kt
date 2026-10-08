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
 * Single Room database for DevOS AI.
 *
 * ## Schema versioning rules
 * - Bump [version] every time entities change.
 * - Add an explicit [Migration] object to [MIGRATIONS] — never use
 *   `fallbackToDestructiveMigration()`.
 * - The Room Gradle plugin writes generated schema JSON to
 *   `core/core-database/schemas/` (configured in `build.gradle.kts`).
 *
 * ## Version history
 * | Version | Change | Ticket |
 * |---------|--------|--------|
 * | 1       | Initial schema: repositories, repository_files, symbols | DEVOS-015 |
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
        const val DATABASE_NAME = "devos_database"

        /**
         * Ordered list of all migrations.  Add new entries here whenever the
         * schema version is bumped — never leave a version gap.
         *
         * Example for a future migration from version 1 → 2:
         * ```kotlin
         * val MIGRATION_1_2 = object : Migration(1, 2) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE repositories ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
         *     }
         * }
         * ```
         */
        val MIGRATIONS: Array<Migration> = arrayOf(
            // No migrations yet — this IS version 1.
            // Add MIGRATION_1_2, MIGRATION_2_3, … here in order.
        )
    }
}
