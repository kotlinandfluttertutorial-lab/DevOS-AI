package com.devos.ai.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import com.devos.ai.core.database.DevOSDatabase
import com.devos.ai.core.database.entity.RepositoryEntity
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration tests for [RepositoryDao] using an in-memory [DevOSDatabase].
 *
 * Uses Turbine for Flow assertions. [BundledSQLiteDriver] ships its own SQLite
 * binary so it never calls [Context.getCacheDir] or any filesystem method —
 * the context mock requires no stubs.
 */
class RepositoryDaoTest {

    private lateinit var db: DevOSDatabase
    private lateinit var dao: RepositoryDao

    @BeforeEach
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            mockk<Context>(relaxed = true),
            DevOSDatabase::class.java,
        )
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = db.repositoryDao()
    }

    @AfterEach
    fun tearDown() {
        db.close()
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun makeEntity(
        id: String = "repo-1",
        name: String = "MyApp",
        owner: String = "acme",
        syncStatus: String = "IDLE",
    ) = RepositoryEntity(
        id            = id,
        name          = name,
        owner         = owner,
        description   = "A test repo",
        language      = "Kotlin",
        stars         = 42,
        forks         = 7,
        defaultBranch = "main",
        cloneUrl      = "https://github.com/$owner/$name.git",
        provider      = "GITHUB",
        healthScore   = 0f,
        lastSyncAt    = null,
        syncStatus    = syncStatus,
        localPath     = null,
    )

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    fun `upsert inserts new entity and observeAll emits it`() = runTest {
        val entity = makeEntity()
        dao.upsert(entity)

        dao.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("repo-1", list.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsert replaces existing entity on id conflict`() = runTest {
        val original = makeEntity(name = "OldName")
        dao.upsert(original)

        val updated = makeEntity(name = "NewName")
        dao.upsert(updated)

        dao.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("NewName", list.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAll emits updated list when second entity inserted`() = runTest {
        dao.observeAll().test {
            // Initial empty emission
            assertEquals(0, awaitItem().size)

            dao.upsert(makeEntity(id = "a", name = "Alpha"))
            assertEquals(1, awaitItem().size)

            dao.upsert(makeEntity(id = "b", name = "Beta"))
            assertEquals(2, awaitItem().size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeById emits null for unknown id`() = runTest {
        dao.observeById("unknown").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeById emits entity after upsert`() = runTest {
        dao.observeById("repo-1").test {
            assertNull(awaitItem()) // not yet inserted

            dao.upsert(makeEntity())
            val entity = awaitItem()
            assertNotNull(entity)
            assertEquals("repo-1", entity?.id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSyncStatus changes syncStatus column only`() = runTest {
        dao.upsert(makeEntity(syncStatus = "IDLE"))
        dao.updateSyncStatus("repo-1", "SYNCING")

        val entity = dao.getById("repo-1")
        assertEquals("SYNCING", entity?.syncStatus)
        // Other fields untouched
        assertEquals("MyApp", entity?.name)
    }

    @Test
    fun `delete removes entity and observeAll emits empty list`() = runTest {
        dao.upsert(makeEntity())
        dao.delete("repo-1")

        dao.observeAll().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns null after delete`() = runTest {
        dao.upsert(makeEntity())
        dao.delete("repo-1")
        assertNull(dao.getById("repo-1"))
    }
}
