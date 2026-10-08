package com.devos.ai.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.devos.ai.core.database.DevOSDatabase
import com.devos.ai.core.database.entity.RepositoryEntity
import com.devos.ai.core.database.entity.SymbolEntity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration test for [SymbolDao] using an in-memory Room database.
 *
 * Covers: insertAll, getByRepo, getByKind, getByFilePath, getById,
 *         observeByRepo (Flow), searchByName (Flow), deleteByRepo, deleteByFile.
 */
class SymbolDaoTest {

    private lateinit var db: DevOSDatabase
    private lateinit var dao: SymbolDao
    private lateinit var repoDao: RepositoryDao

    private val repoId = "repo-sym-test"

    @BeforeEach
    fun setUp() {
        db      = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DevOSDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao     = db.symbolDao()
        repoDao = db.repositoryDao()

        runTest {
            repoDao.upsert(stubRepo(repoId))
        }
    }

    @AfterEach
    fun tearDown() = db.close()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun stubRepo(id: String) = RepositoryEntity(
        id = id, name = id, owner = "owner",
        description = null, language = null,
        stars = 0, forks = 0, defaultBranch = "main",
        cloneUrl = "https://github.com/owner/$id.git",
        provider = "GITHUB", healthScore = 0f,
        lastSyncAt = null, syncStatus = "SYNCED", localPath = null,
    )

    private fun sym(
        name: String,
        kind: String = "CLASS",
        filePath: String = "src/Foo.kt",
        lineStart: Int = 1,
        visibility: String = "PUBLIC",
    ) = SymbolEntity(
        id         = "$repoId:$filePath:$name:$lineStart",
        repoId     = repoId,
        name       = name,
        kind       = kind,
        filePath   = filePath,
        lineStart  = lineStart,
        lineEnd    = lineStart + 10,
        signature  = "class $name",
        docComment = null,
        visibility = visibility,
    )

    // ── insertAll / getByRepo ─────────────────────────────────────────────────

    @Test
    fun `insertAll and getByRepo returns all rows`() = runTest {
        dao.insertAll(listOf(sym("Alpha"), sym("Beta"), sym("Gamma")))
        assertThat(dao.getByRepo(repoId)).hasSize(3)
    }

    @Test
    fun `getByRepo returns empty for unknown repo`() = runTest {
        assertThat(dao.getByRepo("no-such-repo")).isEmpty()
    }

    @Test
    fun `insertAll with REPLACE updates existing row`() = runTest {
        val original = sym("Foo").copy(signature = "old sig")
        val updated  = sym("Foo").copy(signature = "new sig")
        dao.insertAll(listOf(original))
        dao.insertAll(listOf(updated))

        val result = dao.getByRepo(repoId)
        assertThat(result).hasSize(1)
        assertThat(result.first().signature).isEqualTo("new sig")
    }

    // ── getByKind ─────────────────────────────────────────────────────────────

    @Test
    fun `getByKind filters correctly`() = runTest {
        dao.insertAll(listOf(
            sym("MyClass",     kind = "CLASS"),
            sym("myFun",       kind = "FUNCTION"),
            sym("MyInterface", kind = "INTERFACE"),
        ))
        val classes = dao.getByKind(repoId, "CLASS")
        assertThat(classes).hasSize(1)
        assertThat(classes.first().name).isEqualTo("MyClass")
    }

    @Test
    fun `getByKind returns empty when no symbols of that kind`() = runTest {
        dao.insertAll(listOf(sym("Foo", kind = "FUNCTION")))
        assertThat(dao.getByKind(repoId, "ENUM")).isEmpty()
    }

    // ── getByFilePath ─────────────────────────────────────────────────────────

    @Test
    fun `getByFilePath returns only symbols in that file`() = runTest {
        dao.insertAll(listOf(
            sym("A", filePath = "src/A.kt", lineStart = 1),
            sym("B", filePath = "src/B.kt", lineStart = 1),
            sym("C", filePath = "src/A.kt", lineStart = 10),
        ))
        val result = dao.getByFilePath(repoId, "src/A.kt")
        assertThat(result).hasSize(2)
        assertThat(result.map { it.name }.toSet()).isEqualTo(setOf("A", "C"))
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    fun `getById returns correct row`() = runTest {
        val s = sym("Unique", lineStart = 42)
        dao.insertAll(listOf(s))
        val found = dao.getById(s.id)
        assertThat(found).isNotNull()
        assertThat(found!!.name).isEqualTo("Unique")
    }

    @Test
    fun `getById returns null for unknown id`() = runTest {
        assertThat(dao.getById("nonexistent")).isNull()
    }

    // ── observeByRepo (Flow) ──────────────────────────────────────────────────

    @Test
    fun `observeByRepo emits initial empty list then new row`() = runTest {
        dao.observeByRepo(repoId).test {
            assertThat(awaitItem()).isEmpty()           // initial state

            dao.insertAll(listOf(sym("Observed")))
            assertThat(awaitItem()).hasSize(1)          // after insert

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── searchByName (Flow) ───────────────────────────────────────────────────

    @Test
    fun `searchByName finds substring match case-insensitively`() = runTest {
        dao.insertAll(listOf(
            sym("UserRepository"),
            sym("OrderRepository"),
            sym("ProductService"),
        ))
        dao.searchByName("repository", repoId).test {
            val results = awaitItem()
            assertThat(results).hasSize(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchByName with wildcard repoId searches all repos`() = runTest {
        // Add a second repo
        repoDao.upsert(stubRepo("repo-2"))
        dao.insertAll(listOf(sym("Alpha").copy(id = "repo-2:Foo.kt:Alpha:1", repoId = "repo-2")))
        dao.insertAll(listOf(sym("Alpha")))

        dao.searchByName("Alpha", "%").test {
            val results = awaitItem()
            assertThat(results.size).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── searchByNameAndKind ────────────────────────────────────────────────────

    @Test
    fun `searchByNameAndKind filters by kind`() = runTest {
        dao.insertAll(listOf(
            sym("Loader",  kind = "CLASS"),
            sym("loadData", kind = "FUNCTION"),
        ))
        dao.searchByNameAndKind("load", "FUNCTION", repoId).test {
            val results = awaitItem()
            assertThat(results).hasSize(1)
            assertThat(results.first().name).isEqualTo("loadData")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── deleteByRepo ──────────────────────────────────────────────────────────

    @Test
    fun `deleteByRepo removes all symbols for that repo`() = runTest {
        dao.insertAll(listOf(sym("A"), sym("B")))
        dao.deleteByRepo(repoId)
        assertThat(dao.getByRepo(repoId)).isEmpty()
    }

    @Test
    fun `deleteByRepo does not affect other repos`() = runTest {
        repoDao.upsert(stubRepo("other"))
        dao.insertAll(listOf(sym("Keep").copy(id = "other:Foo.kt:Keep:1", repoId = "other")))
        dao.insertAll(listOf(sym("Remove")))

        dao.deleteByRepo(repoId)

        assertThat(dao.getByRepo("other")).hasSize(1)
    }

    // ── deleteByFile ──────────────────────────────────────────────────────────

    @Test
    fun `deleteByFile removes only symbols in that file`() = runTest {
        dao.insertAll(listOf(
            sym("InA",  filePath = "src/A.kt", lineStart = 1),
            sym("InB",  filePath = "src/B.kt", lineStart = 1),
        ))
        dao.deleteByFile(repoId, "src/A.kt")

        assertThat(dao.getByRepo(repoId)).hasSize(1)
        assertThat(dao.getByRepo(repoId).first().name).isEqualTo("InB")
    }

    // ── Cascade delete ────────────────────────────────────────────────────────

    @Test
    fun `deleting parent repository cascades to symbols`() = runTest {
        dao.insertAll(listOf(sym("WillBeGone")))
        repoDao.delete(repoId)
        assertThat(dao.getByRepo(repoId)).isEmpty()
    }
}
