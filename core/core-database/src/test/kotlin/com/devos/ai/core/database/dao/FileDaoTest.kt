package com.devos.ai.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.devos.ai.core.database.DevOSDatabase
import com.devos.ai.core.database.entity.FileEntity
import com.devos.ai.core.database.entity.RepositoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo

/**
 * Integration test for [FileDao] using an in-memory Room database.
 *
 * Covers: insertAll, getByRepo, deleteByPath, deleteByRepo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileDaoTest {

    private lateinit var db: DevOSDatabase
    private lateinit var dao: FileDao
    private lateinit var repoDao: RepositoryDao

    private val repoId = "repo-test"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DevOSDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        dao     = db.fileDao()
        repoDao = db.repositoryDao()

        // Every FileEntity requires a parent RepositoryEntity (FK constraint).
        runTest {
            repoDao.upsert(
                RepositoryEntity(
                    id            = repoId,
                    name          = "TestRepo",
                    owner         = "owner",
                    description   = null,
                    language      = null,
                    stars         = 0,
                    forks         = 0,
                    defaultBranch = "main",
                    cloneUrl      = "https://github.com/owner/TestRepo.git",
                    provider      = "GITHUB",
                    healthScore   = 0f,
                    lastSyncAt    = null,
                    syncStatus    = "IDLE",
                    localPath     = null,
                ),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun fileEntity(path: String, hash: String = "abc123") = FileEntity(
        id           = "$repoId:$path",
        repoId       = repoId,
        path         = path,
        name         = path.substringAfterLast('/'),
        extension    = path.substringAfterLast('.', ""),
        sizeBytes    = 512L,
        lastModified = System.currentTimeMillis(),
        contentHash  = hash,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `insertAll persists all rows and getByRepo returns them`() = runTest {
        val files = listOf(
            fileEntity("src/main/Foo.kt"),
            fileEntity("src/main/Bar.kt"),
            fileEntity("README.md"),
        )
        dao.insertAll(files)

        val result = dao.getByRepo(repoId)
        assertThat(result).hasSize(3)
    }

    @Test
    fun `insertAll with REPLACE updates an existing row`() = runTest {
        dao.insertAll(listOf(fileEntity("src/Foo.kt", hash = "old-hash")))
        dao.insertAll(listOf(fileEntity("src/Foo.kt", hash = "new-hash")))

        val result = dao.getByRepo(repoId)
        assertThat(result).hasSize(1)
        assertThat(result.first().contentHash).isEqualTo("new-hash")
    }

    @Test
    fun `getByRepo returns empty list when no files indexed`() = runTest {
        val result = dao.getByRepo(repoId)
        assertThat(result).isEmpty()
    }

    @Test
    fun `deleteByPath removes only the specified file`() = runTest {
        dao.insertAll(listOf(
            fileEntity("src/Keep.kt"),
            fileEntity("src/Remove.kt"),
        ))
        dao.deleteByPath(repoId, "src/Remove.kt")

        val result = dao.getByRepo(repoId)
        assertThat(result).hasSize(1)
        assertThat(result.first().path).isEqualTo("src/Keep.kt")
    }

    @Test
    fun `deleteByRepo removes all files for the repo`() = runTest {
        dao.insertAll(listOf(
            fileEntity("src/A.kt"),
            fileEntity("src/B.kt"),
        ))
        dao.deleteByRepo(repoId)

        assertThat(dao.getByRepo(repoId)).isEmpty()
    }

    @Test
    fun `getByRepo does not return files from a different repo`() = runTest {
        // Insert a second parent repo
        repoDao.upsert(
            RepositoryEntity(
                id = "other-repo", name = "Other", owner = "x",
                description = null, language = null,
                stars = 0, forks = 0, defaultBranch = "main",
                cloneUrl = "https://github.com/x/Other.git",
                provider = "GITHUB", healthScore = 0f,
                lastSyncAt = null, syncStatus = "IDLE", localPath = null,
            ),
        )
        dao.insertAll(listOf(
            FileEntity(
                id = "other-repo:src/Other.kt", repoId = "other-repo",
                path = "src/Other.kt", name = "Other.kt", extension = "kt",
                sizeBytes = 100L, lastModified = 0L, contentHash = "xyz",
            ),
        ))
        dao.insertAll(listOf(fileEntity("src/Mine.kt")))

        val result = dao.getByRepo(repoId)
        assertThat(result).hasSize(1)
        assertThat(result.first().repoId).isEqualTo(repoId)
    }
}
