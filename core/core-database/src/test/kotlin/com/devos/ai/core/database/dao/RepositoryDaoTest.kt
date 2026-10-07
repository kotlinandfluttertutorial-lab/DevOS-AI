package com.devos.ai.core.database.dao

import com.devos.ai.core.database.entity.RepositoryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [RepositoryDao] using a mock implementation.
 *
 * Room validates SQL at compile time; these tests verify the calling conventions
 * (suspend, Flow return types, argument shapes) and the business-logic expectations
 * that a repository layer would assert against the DAO contract.
 */
class RepositoryDaoTest {

    private lateinit var dao: RepositoryDao

    @BeforeEach
    fun setUp() {
        dao = mockk(relaxed = true)
    }

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

    @Test
    fun `upsert is called with the provided entity`() = runTest {
        val entity = makeEntity()

        dao.upsert(entity)

        coVerify(exactly = 1) { dao.upsert(entity) }
    }

    @Test
    fun `observeAll emits the list returned by the mock`() = runTest {
        val entities = listOf(makeEntity(id = "a"), makeEntity(id = "b"))
        coEvery { dao.observeAll() } returns flowOf(entities)

        val result = mutableListOf<List<RepositoryEntity>>()
        dao.observeAll().collect { result.add(it) }

        assertEquals(1, result.size)
        assertEquals(2, result.first().size)
    }

    @Test
    fun `observeAll emits empty list when no entities exist`() = runTest {
        coEvery { dao.observeAll() } returns flowOf(emptyList())

        val result = mutableListOf<List<RepositoryEntity>>()
        dao.observeAll().collect { result.add(it) }

        assertEquals(1, result.size)
        assertEquals(0, result.first().size)
    }

    @Test
    fun `observeById emits null for unknown id`() = runTest {
        coEvery { dao.observeById("unknown") } returns flowOf(null)

        val result = mutableListOf<RepositoryEntity?>()
        dao.observeById("unknown").collect { result.add(it) }

        assertEquals(1, result.size)
        assertNull(result.first())
    }

    @Test
    fun `observeById emits entity when it exists`() = runTest {
        val entity = makeEntity()
        coEvery { dao.observeById("repo-1") } returns flowOf(entity)

        val result = mutableListOf<RepositoryEntity?>()
        dao.observeById("repo-1").collect { result.add(it) }

        assertEquals(1, result.size)
        assertEquals("repo-1", result.first()?.id)
    }

    @Test
    fun `getById returns null when no entity matches`() = runTest {
        coEvery { dao.getById("missing") } returns null

        assertNull(dao.getById("missing"))
    }

    @Test
    fun `getById returns entity when it exists`() = runTest {
        val entity = makeEntity()
        coEvery { dao.getById("repo-1") } returns entity

        val result = dao.getById("repo-1")

        assertEquals("repo-1", result?.id)
        assertEquals("MyApp", result?.name)
    }

    @Test
    fun `updateSyncStatus is called with correct id and status`() = runTest {
        dao.updateSyncStatus("repo-1", "SYNCING")

        coVerify(exactly = 1) { dao.updateSyncStatus("repo-1", "SYNCING") }
    }

    @Test
    fun `delete is called with the correct id`() = runTest {
        dao.delete("repo-1")

        coVerify(exactly = 1) { dao.delete("repo-1") }
    }
}
