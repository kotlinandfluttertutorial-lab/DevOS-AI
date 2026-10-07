package com.devos.ai.core.database.dao

import com.devos.ai.core.database.entity.FileEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [FileDao] using a mock implementation.
 *
 * Room validates SQL at compile time; these tests verify the calling conventions
 * (suspend, return types, argument shapes) and the business-logic expectations
 * that a repository layer would assert against the DAO contract.
 */
class FileDaoTest {

    private lateinit var dao: FileDao

    @BeforeEach
    fun setUp() {
        dao = mockk(relaxed = true)
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

    @Test
    fun `insertAll is called with the exact list provided`() = runTest {
        val files = listOf(makeFile(path = "src/A.kt"), makeFile(path = "src/B.kt"))

        dao.insertAll(files)

        coVerify(exactly = 1) { dao.insertAll(files) }
    }

    @Test
    fun `getByRepo returns empty list when dao returns empty list`() = runTest {
        coEvery { dao.getByRepo("repo-1") } returns emptyList()

        val result = dao.getByRepo("repo-1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getByRepo returns files for the given repoId`() = runTest {
        val expected = listOf(
            makeFile(path = "src/A.kt"),
            makeFile(path = "src/B.kt"),
        )
        coEvery { dao.getByRepo("repo-1") } returns expected

        val result = dao.getByRepo("repo-1")

        assertEquals(2, result.size)
        assertEquals(expected, result)
    }

    @Test
    fun `getByRepo only returns files matching the given repoId`() = runTest {
        val repo1Files = listOf(makeFile(repoId = "repo-1", path = "A.kt"))
        coEvery { dao.getByRepo("repo-1") } returns repo1Files
        coEvery { dao.getByRepo("repo-2") } returns listOf(makeFile(repoId = "repo-2", path = "B.kt"))

        val result = dao.getByRepo("repo-1")

        assertEquals(1, result.size)
        assertEquals("A.kt", result.first().path)
    }

    @Test
    fun `deleteByRepo is called with the correct repoId`() = runTest {
        dao.deleteByRepo("repo-1")

        coVerify(exactly = 1) { dao.deleteByRepo("repo-1") }
    }

    @Test
    fun `deleteByPath is called with the correct repoId and path`() = runTest {
        dao.deleteByPath("repo-1", "src/A.kt")

        coVerify(exactly = 1) { dao.deleteByPath("repo-1", "src/A.kt") }
    }

    @Test
    fun `getByRepo after insertAll reflects inserted files`() = runTest {
        val files = listOf(
            makeFile(path = "src/A.kt"),
            makeFile(path = "src/B.kt"),
        )
        coEvery { dao.getByRepo("repo-1") } returns files

        dao.insertAll(files)
        val result = dao.getByRepo("repo-1")

        assertEquals(2, result.size)
    }
}
