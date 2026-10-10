package com.devos.ai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devos.ai.core.database.dao.ChunkDao
import com.devos.ai.core.database.dao.FileCoverageDao
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.dao.MemoryEntryDao
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.dao.SecurityFindingDao
import com.devos.ai.core.database.dao.SymbolDao
import com.devos.ai.core.database.entity.CodeChunkEntity
import com.devos.ai.core.database.entity.FileCoverageEntity
import com.devos.ai.core.database.entity.FileEntity
import com.devos.ai.core.database.entity.MemoryEntryEntity
import com.devos.ai.core.database.entity.RepositoryEntity
import com.devos.ai.core.database.entity.SecurityFindingEntity
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
 * | 3       | DEVOS-047 | Added security_findings table |
 * | 4       | DEVOS-049 | Added file_coverage table |
 * | 5       | DEVOS-056 | Added memory_entries table |
 */
@Database(
    entities = [
        RepositoryEntity::class,
        FileEntity::class,
        SymbolEntity::class,
        CodeChunkEntity::class,
        SecurityFindingEntity::class,
        FileCoverageEntity::class,
        MemoryEntryEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class DevOSDatabase : RoomDatabase() {

    abstract fun repositoryDao(): RepositoryDao
    abstract fun fileDao(): FileDao
    abstract fun symbolDao(): SymbolDao
    abstract fun chunkDao(): ChunkDao
    abstract fun securityFindingDao(): SecurityFindingDao
    abstract fun fileCoverageDao(): FileCoverageDao
    abstract fun memoryEntryDao(): MemoryEntryDao

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

        // ── Migration 2 → 3 ───────────────────────────────────────────────────
        // Adds security_findings table (DEVOS-047).

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `security_findings` (
                        `id`          TEXT    NOT NULL,
                        `repoId`      TEXT    NOT NULL,
                        `severity`    TEXT    NOT NULL,
                        `ruleId`      TEXT    NOT NULL,
                        `title`       TEXT    NOT NULL,
                        `description` TEXT    NOT NULL,
                        `filePath`    TEXT    NOT NULL,
                        `lineNumber`  INTEGER NOT NULL,
                        `codeSnippet` TEXT,
                        `cveId`       TEXT,
                        `status`      TEXT    NOT NULL,
                        `detectedAt`  INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`repoId`) REFERENCES `repositories`(`id`)
                            ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_security_findings_repoId` ON `security_findings` (`repoId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_security_findings_status` ON `security_findings` (`status`)",
                )
            }
        }

        // ── Migration 3 → 4 ───────────────────────────────────────────────────
        // Adds file_coverage table (DEVOS-049).

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `file_coverage` (
                        `id`             TEXT    NOT NULL,
                        `repoId`         TEXT    NOT NULL,
                        `filePath`       TEXT    NOT NULL,
                        `lineCoverage`   REAL    NOT NULL,
                        `branchCoverage` REAL    NOT NULL,
                        `coveredLines`   INTEGER NOT NULL,
                        `totalLines`     INTEGER NOT NULL,
                        `updatedAt`      INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_file_coverage_repoId` ON `file_coverage` (`repoId`)",
                )
            }
        }

        // ── Migration 4 → 5 ───────────────────────────────────────────────────
        // Adds memory_entries table (DEVOS-056).

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `memory_entries` (
                        `id`             TEXT    NOT NULL,
                        `userId`         TEXT    NOT NULL,
                        `projectId`      TEXT,
                        `content`        TEXT    NOT NULL,
                        `category`       TEXT    NOT NULL,
                        `source`         TEXT    NOT NULL,
                        `createdAt`      INTEGER NOT NULL,
                        `lastAccessedAt` INTEGER NOT NULL,
                        `expiresAt`      INTEGER,
                        `isPinned`       INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_memory_entries_userId` ON `memory_entries` (`userId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_memory_entries_projectId` ON `memory_entries` (`projectId`)",
                )
            }
        }

        /**
         * Ordered list of all migrations. Pass to
         * `Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS)`.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
        )
    }
}
