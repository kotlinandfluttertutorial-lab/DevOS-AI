package com.devos.ai.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.devos.ai.core.database.DevOSDatabase
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
import assertk.assertions.isNotNull
import assertk.assertions.isNull

/**
 * Integration test for [RepositoryDao] using an in-memory Room database.
 *
 * Covers: upsert, observeAll Flow emission, observeById, updateSyncStatus,
 *         updateLocalPath, and delete.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RepositoryDaoTest {

    private lateinit var db: DevOSDatabase
    private lateinit var dao: RepositoryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DevOSDatabase::class.java,
        )
            .allowMainThreadQueries()   // acceptable in unit tests only
            .build()
        dao = db.repositoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun entity(
        id: String = "repo-1",
        name: String = "MyRepo",
        owner: String = "owner",
        syncStatus: String = "IDLE",
        localPath: String? = null,
    ) = RepositoryEntity(
        id            = id,
        name          = name,
        owner         = owner,
        description   = null,
        language      = "Kotlin",
        stars         = 0,
        forks         = 0,
        defaultBranch = "main",
        cloneUrl      = "https://github.com/$owner/$name.git",
        provider      = "GITHUB",
        healthScore   = 0f,
        lastSyncAt    = null,
        syncStatus    = syncStatus,
        localPath     = localPath,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `upsert inserts a new row and observeAll emits it`() = runTest {
        dao.upsert(entity())

        dao.observeAll().test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list.first().id).isEqualTo("repo-1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsert replaces an existing row`() = runTest {
        dao.upsert(entity(name = "OldName"))
        dao.upsert(entity(name = "NewName"))   // same id → replace

        dao.observeAll().test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list.first().name).isEqualTo("NewName")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAll emits empty list when table is empty`() = runTest {
        dao.observeAll().test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAll re-emits when a second row is inserted`() = runTest {
        dao.observeAll().test {
            assertThat(awaitItem()).isEmpty()         // initial state

            dao.upsert(entity("repo-1"))
            assertThat(awaitItem()).hasSize(1)        // after first insert

            dao.upsert(entity("repo-2"))
            assertThat(awaitItem()).hasSize(2)        // after second insert

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeById returns entity by id`() = runTest {
        dao.upsert(entity("repo-42"))

        dao.observeById("repo-42").test {
            assertThat(awaitItem()).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeById emits null for unknown id`() = runTest {
        dao.observeById("nonexistent").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSyncStatus changes only the status column`() = runTest {
        dao.upsert(entity(syncStatus = "IDLE"))
        dao.updateSyncStatus("repo-1", "SYNCING")

        dao.observeById("repo-1").test {
            val updated = awaitItem()
            assertThat(updated?.syncStatus).isEqualTo("SYNCING")
            assertThat(updated?.name).isEqualTo("MyRepo")   // other columns intact
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateLocalPath stores the path`() = runTest {
        dao.upsert(entity())
        dao.updateLocalPath("repo-1", "/data/data/com.devos.ai/files/repos/repo-1")

        dao.observeById("repo-1").test {
            assertThat(awaitItem()?.localPath)
                .isEqualTo("/data/data/com.devos.ai/files/repos/repo-1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes the row`() = runTest {
        dao.upsert(entity("repo-1"))
        dao.delete("repo-1")

        dao.observeById("repo-1").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
