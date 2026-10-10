package com.devos.ai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devos.ai.core.database.dao.ChunkDao
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.dao.SymbolDao
import com.devos.ai.core.database.entity.CodeChunkEntity
import com.devos.ai.core.database.entity.FileEntity
import com.devos.ai.core.database.entity.RepositoryEntity
import com.devos.ai.core.database.entity.SymbolEntity

/**
 * Single Room database for DevOS AI.
 *
 * ## Schema versioning rules
 * - Bump [version] every time entities change.
 * - Add an explicit [Migration] to [ALL_MIGRATIONS] — **never** use
 *   `fallbackToDestructiveMigration()`.
 * - Room writes schema JSON to `core-database/schemas/` (configured in build.gradle.kts).
 *
 * ## Version history
 * | Version | Ticket    | Change |
 * |---------|-----------|--------|
 * | 1       | DEVOS-015 | Initial schema: repositories, repository_files, symbols |
 * | 2       | DEVOS-031 | Added code_chunks table for RAG pipeline |
 */
@Database(
    entities = [
        RepositoryEntity::class,
        FileEntity::class,
        SymbolEntity::class,
        CodeChunkEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class DevOSDatabase : RoomDatabase() {

    abstract fun repositoryDao(): RepositoryDao
    abstract fun fileDao(): FileDao
    abstract fun symbolDao(): SymbolDao
    abstract fun chunkDao(): ChunkDao

    companion object {
        const val DATABASE_NAME = "devos.db"

        // ── Migration 1 → 2 ───────────────────────────────────────────────────
        // Adds the code_chunks table for the RAG pipeline (DEVOS-031).

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `code_chunks` (
                        `id`            TEXT    NOT NULL,
                        `repoId`        TEXT    NOT NULL,
                        `filePath`      TEXT    NOT NULL,
                        `lineStart`     INTEGER NOT NULL,
                        `lineEnd`       INTEGER NOT NULL,
                        `content`       TEXT    NOT NULL,
                        `language`      TEXT    NOT NULL,
                        `embeddingJson` TEXT,
                        `indexedAt`     INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`repoId`) REFERENCES `repositories`(`id`)
                            ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_code_chunks_repoId` ON `code_chunks` (`repoId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_code_chunks_filePath` ON `code_chunks` (`filePath`)",
                )
            }
        }

        /**
         * Ordered list of all migrations. Pass to
         * `Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS)`.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
        )
    }
}
