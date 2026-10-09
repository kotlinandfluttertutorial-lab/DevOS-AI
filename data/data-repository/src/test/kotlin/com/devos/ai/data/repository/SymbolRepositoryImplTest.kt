package com.devos.ai.data.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.devos.ai.core.database.dao.SymbolDao
import com.devos.ai.core.database.entity.SymbolEntity
import com.devos.ai.domain.repository.model.SymbolKind
import com.devos.ai.domain.repository.model.SymbolVisibility
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SymbolRepositoryImpl].
 *
 * [SymbolDao] is mocked with MockK so no Room runtime is required.
 *
 * Covers:
 * - [SymbolRepositoryImpl.observeSymbols] maps DAO Flow to domain Flow
 * - [SymbolRepositoryImpl.searchSymbols] routes to correct DAO query per kind set size
 * - [SymbolRepositoryImpl.getSymbolsForFile] delegates to [SymbolDao.getByFilePath]
 * - [SymbolRepositoryImpl.getSymbol] delegates to [SymbolDao.getById]
 * - [SymbolRepositoryImpl.deleteByRepo] calls [SymbolDao.deleteByRepo]
 * - toDomain() maps kind/visibility enums correctly; falls back on unknown values
 */
class SymbolRepositoryImplTest {

    private lateinit var symbolDao: SymbolDao
    private lateinit var impl: SymbolRepositoryImpl

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        symbolDao = mockk(relaxed = true)
        impl      = SymbolRepositoryImpl(symbolDao)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun stubEntity(
        id: String = "repo1:Foo.kt:MyClass:1",
        repoId: String = "repo1",
        name: String = "MyClass",
        kind: String = "CLASS",
        filePath: String = "Foo.kt",
        lineStart: Int = 1,
        visibility: String = "PUBLIC",
    ) = SymbolEntity(
        id         = id,
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

    // ── observeSymbols ────────────────────────────────────────────────────────

    @Test
    fun `observeSymbols maps entity list to domain list`() = runTest {
        val entities = listOf(stubEntity(name = "Alpha"), stubEntity(name = "Beta",
            id = "repo1:Foo.kt:Beta:20", lineStart = 20))
        every { symbolDao.observeByRepo("repo1") } returns flowOf(entities)

        impl.observeSymbols("repo1").test {
            val symbols = awaitItem()
            assertThat(symbols).hasSize(2)
            assertThat(symbols.map { it.name }.toSet()).isEqualTo(setOf("Alpha", "Beta"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSymbols maps kind correctly`() = runTest {
        every { symbolDao.observeByRepo("repo1") } returns
            flowOf(listOf(stubEntity(kind = "INTERFACE")))

        impl.observeSymbols("repo1").test {
            assertThat(awaitItem().first().kind).isEqualTo(SymbolKind.INTERFACE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSymbols maps unknown kind to FUNCTION fallback`() = runTest {
        every { symbolDao.observeByRepo("repo1") } returns
            flowOf(listOf(stubEntity(kind = "UNKNOWN_KIND")))

        impl.observeSymbols("repo1").test {
            assertThat(awaitItem().first().kind).isEqualTo(SymbolKind.FUNCTION)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSymbols maps visibility correctly`() = runTest {
        every { symbolDao.observeByRepo("repo1") } returns
            flowOf(listOf(stubEntity(visibility = "INTERNAL")))

        impl.observeSymbols("repo1").test {
            assertThat(awaitItem().first().visibility).isEqualTo(SymbolVisibility.INTERNAL)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── searchSymbols — no kind filter ────────────────────────────────────────

    @Test
    fun `searchSymbols with empty kinds uses searchByName`() = runTest {
        every { symbolDao.searchByName("ViewModel", "repo1") } returns
            flowOf(listOf(stubEntity(name = "MyViewModel")))

        impl.searchSymbols("ViewModel", repoId = "repo1", kinds = emptySet()).test {
            assertThat(awaitItem()).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchSymbols with null repoId uses wildcard`() = runTest {
        every { symbolDao.searchByName("Repo", "%") } returns flowOf(emptyList())

        impl.searchSymbols("Repo", repoId = null).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { symbolDao.searchByNameAndKind(any(), any(), any()) }
    }

    // ── searchSymbols — single kind ───────────────────────────────────────────

    @Test
    fun `searchSymbols with single kind uses searchByNameAndKind`() = runTest {
        every {
            symbolDao.searchByNameAndKind("send", "FUNCTION", "repo1")
        } returns flowOf(listOf(stubEntity(name = "sendMessage", kind = "FUNCTION")))

        impl.searchSymbols("send", repoId = "repo1", kinds = setOf(SymbolKind.FUNCTION)).test {
            val results = awaitItem()
            assertThat(results).hasSize(1)
            assertThat(results.first().name).isEqualTo("sendMessage")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── searchSymbols — multiple kinds (in-memory filter) ─────────────────────

    @Test
    fun `searchSymbols with multiple kinds filters in memory`() = runTest {
        val entities = listOf(
            stubEntity(name = "MyClass",  kind = "CLASS",     id = "r:F:MyClass:1",  lineStart = 1),
            stubEntity(name = "myFun",    kind = "FUNCTION",  id = "r:F:myFun:10",   lineStart = 10),
            stubEntity(name = "MyObject", kind = "OBJECT",    id = "r:F:MyObject:20", lineStart = 20),
        )
        every { symbolDao.searchByName("my", "repo1") } returns flowOf(entities)

        impl.searchSymbols(
            "my",
            repoId = "repo1",
            kinds  = setOf(SymbolKind.CLASS, SymbolKind.OBJECT),
        ).test {
            val results = awaitItem()
            assertThat(results).hasSize(2)
            assertThat(results.none { it.kind == SymbolKind.FUNCTION }).isEqualTo(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getSymbolsForFile ─────────────────────────────────────────────────────

    @Test
    fun `getSymbolsForFile delegates to DAO and maps results`() = runTest {
        coEvery { symbolDao.getByFilePath("repo1", "src/Foo.kt") } returns
            listOf(stubEntity(name = "Foo"), stubEntity(name = "FooHelper",
                id = "repo1:Foo.kt:FooHelper:20", lineStart = 20))

        val result = impl.getSymbolsForFile("repo1", "src/Foo.kt")
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getSymbolsForFile returns empty list for unknown file`() = runTest {
        coEvery { symbolDao.getByFilePath(any(), any()) } returns emptyList()
        assertThat(impl.getSymbolsForFile("repo1", "nonexistent.kt")).isEmpty()
    }

    // ── getSymbol ─────────────────────────────────────────────────────────────

    @Test
    fun `getSymbol returns domain object when found`() = runTest {
        val entity = stubEntity(id = "repo1:Foo.kt:Target:5", lineStart = 5)
        coEvery { symbolDao.getById("repo1:Foo.kt:Target:5") } returns entity

        val result = impl.getSymbol("repo1:Foo.kt:Target:5")
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("MyClass")
    }

    @Test
    fun `getSymbol returns null when not found`() = runTest {
        coEvery { symbolDao.getById(any()) } returns null
        assertThat(impl.getSymbol("missing-id")).isNull()
    }

    // ── deleteByRepo ──────────────────────────────────────────────────────────

    @Test
    fun `deleteByRepo delegates to DAO`() = runTest {
        impl.deleteByRepo("repo1")
        coVerify(exactly = 1) { symbolDao.deleteByRepo("repo1") }
    }
}
