package com.devos.ai.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.devos.ai.core.database.DevOSDatabase
import com.devos.ai.core.database.entity.FileEntity
import com.devos.ai.core.database.entity.RepositoryEntity
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for [FileDao] using an in-memory [DevOSDatabase].
 *
 * [BundledSQLiteDriver] ships its own SQLite binary so it never calls
 * [Context.getCacheDir] or any other filesystem method — the context mock
 * requires no stubs.
 */
class FileDaoTest {

    private lateinit var db: DevOSDatabase
    private lateinit var fileDao: FileDao
    private lateinit var repoDao: RepositoryDao

    @BeforeEach
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            mockk<Context>(relaxed = true),
            DevOSDatabase::class.java,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        fileDao = db.fileDao()
        repoDao = db.repositoryDao()
    }

    @AfterEach
    fun tearDown() = db.close()

    // ── Helpers ────────────────────────────────────────────────────────────────

    private suspend fun insertRepo(repoId: String = "repo-1") {
        repoDao.upsert(
            RepositoryEntity(
                id            = repoId,
                name          = "Repo",
                owner         = "owner",
                description   = null,
                language      = null,
                stars         = 0,
                forks         = 0,
                defaultBranch = "main",
                cloneUrl      = "https://github.com/owner/repo.git",
                provider      = "GITHUB",
                healthScore   = 0f,
                lastSyncAt    = null,
                syncStatus    = "IDLE",
                localPath     = null,
            ),
        )
    }

    private fun makeFile(
        repoId: String = "repo-1",
        path: String = "src/Main.kt",
        hash: String = "abc123",
    ) = FileEntity(
        id           = "$repoId:$path",
        repoId       = repoId,
        path         = path,
        name         = path.substringAfterLast('/'),
        extension    = path.substringAfterLast('.', ""),
        sizeBytes    = 1024L,
        lastModified = 1_000_000L,
        contentHash  = hash,
    )

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    fun `insertAll stores files and getByRepo returns them`() = runTest {
        insertRepo()
        val files = listOf(
            makeFile(path = "src/A.kt"),
            makeFile(path = "src/B.kt"),
        )
        fileDao.insertAll(files)

        val result = fileDao.getByRepo("repo-1")
        assertEquals(2, result.size)
        assertTrue(result.any { it.path == "src/A.kt" })
        assertTrue(result.any { it.path == "src/B.kt" })
    }

    @Test
    fun `insertAll with REPLACE updates existing row on id conflict`() = runTest {
        insertRepo()
        fileDao.insertAll(listOf(makeFile(path = "src/A.kt", hash = "old")))
        fileDao.insertAll(listOf(makeFile(path = "src/A.kt", hash = "new")))

        val result = fileDao.getByRepo("repo-1")
        assertEquals(1, result.size)
        assertEquals("new", result.first().contentHash)
    }

    @Test
    fun `getByRepo returns empty list when no files exist`() = runTest {
        insertRepo()
        assertTrue(fileDao.getByRepo("repo-1").isEmpty())
    }

    @Test
    fun `getByRepo only returns files for the given repoId`() = runTest {
        insertRepo("repo-1")
        insertRepo("repo-2")
        fileDao.insertAll(listOf(makeFile(repoId = "repo-1", path = "A.kt")))
        fileDao.insertAll(listOf(makeFile(repoId = "repo-2", path = "B.kt")))

        val result = fileDao.getByRepo("repo-1")
        assertEquals(1, result.size)
        assertEquals("A.kt", result.first().path)
    }

    @Test
    fun `deleteByRepo removes all files for a repo`() = runTest {
        insertRepo()
        fileDao.insertAll(
            listOf(
                makeFile(path = "src/A.kt"),
                makeFile(path = "src/B.kt"),
            ),
        )
        fileDao.deleteByRepo("repo-1")

        assertTrue(fileDao.getByRepo("repo-1").isEmpty())
    }

    @Test
    fun `deleteByPath removes only the specified file`() = runTest {
        insertRepo()
        fileDao.insertAll(
            listOf(
                makeFile(path = "src/A.kt"),
                makeFile(path = "src/B.kt"),
            ),
        )
        fileDao.deleteByPath("repo-1", "src/A.kt")

        val result = fileDao.getByRepo("repo-1")
        assertEquals(1, result.size)
        assertFalse(result.any { it.path == "src/A.kt" })
        assertTrue(result.any { it.path == "src/B.kt" })
    }

    @Test
    fun `cascade delete removes files when parent repo is deleted`() = runTest {
        insertRepo()
        fileDao.insertAll(listOf(makeFile(path = "src/A.kt")))
        repoDao.delete("repo-1")

        assertTrue(fileDao.getByRepo("repo-1").isEmpty())
    }
}
