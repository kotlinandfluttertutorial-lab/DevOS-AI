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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration test for [SymbolDao] using an in-memory Room database.
 *
 * Runs on the JVM via Robolectric so [ApplicationProvider] and Room's
 * Android-specific code are available without a device or emulator.
 *
 * Covers: insertAll, getByRepo, getByKind, getByFilePath, getById,
 *         observeByRepo (Flow), searchByName (Flow), deleteByRepo, deleteByFile.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SymbolDaoTest {

    private lateinit var db: DevOSDatabase
    private lateinit var dao: SymbolDao
    private lateinit var repoDao: RepositoryDao

    private val repoId = "repo-sym-test"

    @Before
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

    @After
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
    fun insertAll_and_getByRepo_returns_all_rows() = runTest {
        dao.insertAll(listOf(sym("Alpha"), sym("Beta"), sym("Gamma")))
        assertThat(dao.getByRepo(repoId)).hasSize(3)
    }

    @Test
    fun getByRepo_returns_empty_for_unknown_repo() = runTest {
        assertThat(dao.getByRepo("no-such-repo")).isEmpty()
    }

    @Test
    fun insertAll_with_REPLACE_updates_existing_row() = runTest {
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
    fun getByKind_filters_correctly() = runTest {
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
    fun getByKind_returns_empty_when_no_symbols_of_that_kind() = runTest {
        dao.insertAll(listOf(sym("Foo", kind = "FUNCTION")))
        assertThat(dao.getByKind(repoId, "ENUM")).isEmpty()
    }

    // ── getByFilePath ─────────────────────────────────────────────────────────

    @Test
    fun getByFilePath_returns_only_symbols_in_that_file() = runTest {
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
    fun getById_returns_correct_row() = runTest {
        val s = sym("Unique", lineStart = 42)
        dao.insertAll(listOf(s))
        val found = dao.getById(s.id)
        assertThat(found).isNotNull()
        assertThat(found!!.name).isEqualTo("Unique")
    }

    @Test
    fun getById_returns_null_for_unknown_id() = runTest {
        assertThat(dao.getById("nonexistent")).isNull()
    }

    // ── observeByRepo (Flow) ──────────────────────────────────────────────────

    @Test
    fun observeByRepo_emits_initial_empty_list_then_new_row() = runTest {
        dao.observeByRepo(repoId).test {
            assertThat(awaitItem()).isEmpty()           // initial state

            dao.insertAll(listOf(sym("Observed")))
            assertThat(awaitItem()).hasSize(1)          // after insert

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── searchByName (Flow) ───────────────────────────────────────────────────

    @Test
    fun searchByName_finds_substring_match_case_insensitively() = runTest {
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
    fun searchByName_with_wildcard_repoId_searches_all_repos() = runTest {
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
    fun searchByNameAndKind_filters_by_kind() = runTest {
        dao.insertAll(listOf(
            sym("Loader",   kind = "CLASS"),
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
    fun deleteByRepo_removes_all_symbols_for_that_repo() = runTest {
        dao.insertAll(listOf(sym("A"), sym("B")))
        dao.deleteByRepo(repoId)
        assertThat(dao.getByRepo(repoId)).isEmpty()
    }

    @Test
    fun deleteByRepo_does_not_affect_other_repos() = runTest {
        repoDao.upsert(stubRepo("other"))
        dao.insertAll(listOf(sym("Keep").copy(id = "other:Foo.kt:Keep:1", repoId = "other")))
        dao.insertAll(listOf(sym("Remove")))

        dao.deleteByRepo(repoId)

        assertThat(dao.getByRepo("other")).hasSize(1)
    }

    // ── deleteByFile ──────────────────────────────────────────────────────────

    @Test
    fun deleteByFile_removes_only_symbols_in_that_file() = runTest {
        dao.insertAll(listOf(
            sym("InA", filePath = "src/A.kt", lineStart = 1),
            sym("InB", filePath = "src/B.kt", lineStart = 1),
        ))
        dao.deleteByFile(repoId, "src/A.kt")

        assertThat(dao.getByRepo(repoId)).hasSize(1)
        assertThat(dao.getByRepo(repoId).first().name).isEqualTo("InB")
    }

    // ── Cascade delete ────────────────────────────────────────────────────────

    @Test
    fun deleting_parent_repository_cascades_to_symbols() = runTest {
        dao.insertAll(listOf(sym("WillBeGone")))
        repoDao.delete(repoId)
        assertThat(dao.getByRepo(repoId)).isEmpty()
    }
}
