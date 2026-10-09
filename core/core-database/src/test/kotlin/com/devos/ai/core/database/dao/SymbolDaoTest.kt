package com.devos.ai.core.database.dao

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.devos.ai.core.database.entity.SymbolEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SymbolDao] using MockK.
 *
 * These tests verify the DAO contract (method signatures and return types) in a
 * pure JVM environment. Integration tests that exercise the actual SQLite
 * queries belong in the `androidTest` source set where a real Android
 * runtime (or emulator) is available.
 */
class SymbolDaoTest {

    private lateinit var dao: SymbolDao

    private val repoId = "repo-sym-test"

    @BeforeEach
    fun setUp() {
        dao = mockk(relaxed = true)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sym(
        name: String,
        kind: String = "CLASS",
        filePath: String = "src/Foo.kt",
        lineStart: Int = 1,
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
        visibility = "PUBLIC",
    )

    // ── getByRepo ─────────────────────────────────────────────────────────────

    @Test
    fun `getByRepo returns list from dao`() = runTest {
        val symbols = listOf(sym("Alpha"), sym("Beta"))
        coEvery { dao.getByRepo(repoId) } returns symbols

        val result = dao.getByRepo(repoId)

        assertThat(result).hasSize(2)
        coVerify(exactly = 1) { dao.getByRepo(repoId) }
    }

    @Test
    fun `getByRepo returns empty for unknown repo`() = runTest {
        coEvery { dao.getByRepo("no-such-repo") } returns emptyList()

        assertThat(dao.getByRepo("no-such-repo")).isEmpty()
    }

    // ── getByKind ─────────────────────────────────────────────────────────────

    @Test
    fun `getByKind returns filtered list`() = runTest {
        val classes = listOf(sym("MyClass", kind = "CLASS"))
        coEvery { dao.getByKind(repoId, "CLASS") } returns classes

        val result = dao.getByKind(repoId, "CLASS")

        assertThat(result).hasSize(1)
        assertThat(result.first().name).isEqualTo("MyClass")
    }

    @Test
    fun `getByKind returns empty when no symbols of that kind`() = runTest {
        coEvery { dao.getByKind(repoId, "ENUM") } returns emptyList()

        assertThat(dao.getByKind(repoId, "ENUM")).isEmpty()
    }

    // ── getByFilePath ─────────────────────────────────────────────────────────

    @Test
    fun `getByFilePath returns only symbols in that file`() = runTest {
        val inFile = listOf(
            sym("A", filePath = "src/A.kt", lineStart = 1),
            sym("C", filePath = "src/A.kt", lineStart = 10),
        )
        coEvery { dao.getByFilePath(repoId, "src/A.kt") } returns inFile

        val result = dao.getByFilePath(repoId, "src/A.kt")

        assertThat(result).hasSize(2)
        assertThat(result.map { it.name }.toSet()).isEqualTo(setOf("A", "C"))
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    fun `getById returns correct entity`() = runTest {
        val s = sym("Unique", lineStart = 42)
        coEvery { dao.getById(s.id) } returns s

        val found = dao.getById(s.id)

        assertThat(found).isNotNull()
        assertThat(found!!.name).isEqualTo("Unique")
    }

    @Test
    fun `getById returns null for unknown id`() = runTest {
        coEvery { dao.getById("nonexistent") } returns null

        assertThat(dao.getById("nonexistent")).isNull()
    }

    // ── observeByRepo (Flow) ──────────────────────────────────────────────────

    @Test
    fun `observeByRepo emits list from flow`() = runTest {
        val symbols = listOf(sym("Observed"))
        coEvery { dao.observeByRepo(repoId) } returns flowOf(symbols)

        dao.observeByRepo(repoId).test {
            assertThat(awaitItem()).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── searchByName (Flow) ───────────────────────────────────────────────────

    @Test
    fun `searchByName returns matching symbols`() = runTest {
        val results = listOf(sym("UserRepository"), sym("OrderRepository"))
        coEvery { dao.searchByName("repository", repoId) } returns flowOf(results)

        dao.searchByName("repository", repoId).test {
            assertThat(awaitItem()).hasSize(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── searchByNameAndKind ────────────────────────────────────────────────────

    @Test
    fun `searchByNameAndKind filters by kind`() = runTest {
        val results = listOf(sym("loadData", kind = "FUNCTION"))
        coEvery { dao.searchByNameAndKind("load", "FUNCTION", repoId) } returns flowOf(results)

        dao.searchByNameAndKind("load", "FUNCTION", repoId).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().name).isEqualTo("loadData")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── insertAll ─────────────────────────────────────────────────────────────

    @Test
    fun `insertAll is called with the correct entities`() = runTest {
        val symbols = listOf(sym("Alpha"), sym("Beta"))

        dao.insertAll(symbols)

        coVerify(exactly = 1) { dao.insertAll(symbols) }
    }

    // ── deleteByRepo ──────────────────────────────────────────────────────────

    @Test
    fun `deleteByRepo is called with the correct repoId`() = runTest {
        dao.deleteByRepo(repoId)

        coVerify(exactly = 1) { dao.deleteByRepo(repoId) }
    }

    // ── deleteByFile ──────────────────────────────────────────────────────────

    @Test
    fun `deleteByFile is called with correct repoId and path`() = runTest {
        dao.deleteByFile(repoId, "src/A.kt")

        coVerify(exactly = 1) { dao.deleteByFile(repoId, "src/A.kt") }
    }
}
