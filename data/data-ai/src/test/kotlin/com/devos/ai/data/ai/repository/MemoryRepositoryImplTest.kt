package com.devos.ai.data.ai.repository

import app.cash.turbine.test
import com.devos.ai.core.database.dao.MemoryEntryDao
import com.devos.ai.core.database.entity.MemoryEntryEntity
import com.devos.ai.domain.ai.model.MemoryCategory
import com.devos.ai.domain.ai.model.MemoryEntry
import com.devos.ai.domain.ai.model.MemorySource
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MemoryRepositoryImpl].
 *
 * DEVOS-056 / DA-67
 */
class MemoryRepositoryImplTest {

    private lateinit var dao: MemoryEntryDao
    private lateinit var repository: MemoryRepositoryImpl

    @BeforeEach
    fun setUp() {
        dao        = mockk()
        repository = MemoryRepositoryImpl(dao)
    }

    // ── observeEntries ────────────────────────────────────────────────────────

    @Test
    fun `observeEntries maps entity list to domain model list`() = runTest {
        val entity = buildEntity(id = "e1", content = "prefer MockK", category = "CODE_PREFERENCE")
        every { dao.observeAllByUser("user1") } returns flowOf(listOf(entity))

        repository.observeEntries("user1").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("e1", items[0].id)
            assertEquals("prefer MockK", items[0].content)
            assertEquals(MemoryCategory.CODE_PREFERENCE, items[0].category)
            assertEquals(MemorySource.AI_CHAT, items[0].source)
            awaitComplete()
        }
    }

    @Test
    fun `observeEntries emits empty list when DAO emits empty`() = runTest {
        every { dao.observeAllByUser("user1") } returns flowOf(emptyList())

        repository.observeEntries("user1").test {
            assertEquals(emptyList<MemoryEntry>(), awaitItem())
            awaitComplete()
        }
    }

    // ── saveEntry ─────────────────────────────────────────────────────────────

    @Test
    fun `saveEntry calls dao insert and returns success`() = runTest {
        coJustRun { dao.insert(any()) }

        val entry  = buildDomainEntry(id = "e1")
        val result = repository.saveEntry(entry)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dao.insert(any()) }
    }

    @Test
    fun `saveEntry returns failure when dao throws`() = runTest {
        coEvery { dao.insert(any()) } throws RuntimeException("DB error")

        val result = repository.saveEntry(buildDomainEntry(id = "e2"))

        assertTrue(result.isFailure)
    }

    // ── deleteEntry ───────────────────────────────────────────────────────────

    @Test
    fun `deleteEntry calls dao delete and returns success`() = runTest {
        coJustRun { dao.delete(any()) }

        val result = repository.deleteEntry("e1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dao.delete("e1") }
    }

    @Test
    fun `deleteEntry returns failure when dao throws`() = runTest {
        coEvery { dao.delete(any()) } throws RuntimeException("DB error")

        val result = repository.deleteEntry("e1")

        assertTrue(result.isFailure)
    }

    // ── clearAll ──────────────────────────────────────────────────────────────

    @Test
    fun `clearAll calls dao deleteAll with null projectId and returns success`() = runTest {
        coJustRun { dao.deleteAll(any(), any()) }

        val result = repository.clearAll("user1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dao.deleteAll("user1", null) }
    }

    // ── getRelevantMemory ─────────────────────────────────────────────────────

    @Test
    fun `getRelevantMemory returns entries containing query keyword`() = runTest {
        val matching  = buildEntity(id = "e1", content = "prefer MockK over Mockito")
        val nonMatch  = buildEntity(id = "e2", content = "decided to use MVVM")
        every { dao.observeAllByUser("user1") } returns flowOf(listOf(matching, nonMatch))

        repository.getRelevantMemory("user1", "MockK").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("e1", items[0].id)
            awaitComplete()
        }
    }

    @Test
    fun `getRelevantMemory with empty query returns all entries`() = runTest {
        val e1 = buildEntity(id = "e1", content = "prefer coroutines")
        val e2 = buildEntity(id = "e2", content = "decided MVVM")
        every { dao.observeAllByUser("user1") } returns flowOf(listOf(e1, e2))

        repository.getRelevantMemory("user1", "").test {
            val items = awaitItem()
            assertEquals(2, items.size)
            awaitComplete()
        }
    }

    @Test
    fun `getRelevantMemory is case-insensitive`() = runTest {
        val entity = buildEntity(id = "e1", content = "prefer MOCKK for testing")
        every { dao.observeAllByUser("user1") } returns flowOf(listOf(entity))

        repository.getRelevantMemory("user1", "mockk").test {
            val items = awaitItem()
            assertEquals(1, items.size)
            awaitComplete()
        }
    }

    // ── Unknown enum fallback ─────────────────────────────────────────────────

    @Test
    fun `unknown category falls back to GLOBAL without crashing`() = runTest {
        val entity = buildEntity(id = "e1", category = "UNKNOWN_FUTURE_CATEGORY")
        every { dao.observeAllByUser("user1") } returns flowOf(listOf(entity))

        repository.observeEntries("user1").test {
            val items = awaitItem()
            assertEquals(MemoryCategory.GLOBAL, items[0].category)
            awaitComplete()
        }
    }

    @Test
    fun `unknown source falls back to AI_CHAT without crashing`() = runTest {
        val entity = buildEntity(id = "e1", source = "UNKNOWN_SOURCE")
        every { dao.observeAllByUser("user1") } returns flowOf(listOf(entity))

        repository.observeEntries("user1").test {
            val items = awaitItem()
            assertEquals(MemorySource.AI_CHAT, items[0].source)
            awaitComplete()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildEntity(
        id: String       = "test-id",
        userId: String   = "user1",
        content: String  = "some content",
        category: String = "CODE_PREFERENCE",
        source: String   = "AI_CHAT",
    ) = MemoryEntryEntity(
        id             = id,
        userId         = userId,
        projectId      = null,
        content        = content,
        category       = category,
        source         = source,
        createdAt      = 1_000_000L,
        lastAccessedAt = 1_000_000L,
        expiresAt      = null,
        isPinned       = false,
    )

    private fun buildDomainEntry(id: String = "test-id") = MemoryEntry(
        id        = id,
        userId    = "user1",
        content   = "prefer Kotlin DSL",
        category  = MemoryCategory.CODE_PREFERENCE,
        source    = MemorySource.AI_CHAT,
        createdAt = System.currentTimeMillis(),
        isPinned  = false,
    )
}
