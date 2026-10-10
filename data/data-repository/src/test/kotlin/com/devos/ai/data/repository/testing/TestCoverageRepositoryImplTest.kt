package com.devos.ai.data.repository.testing

import app.cash.turbine.test
import com.devos.ai.core.database.dao.FileCoverageDao
import com.devos.ai.core.database.entity.FileCoverageEntity
import com.devos.ai.domain.repository.model.FileCoverage
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream

/**
 * Unit tests for [TestCoverageRepositoryImpl].
 *
 * [JaCoCoParser] is mocked so we can control its output.
 * [FileCoverageDao] is mocked via MockK — no Android runtime needed.
 *
 * DEVOS-049 / DA-60
 */
class TestCoverageRepositoryImplTest {

    private lateinit var fileCoverageDao: FileCoverageDao
    private lateinit var parser: JaCoCoParser
    private lateinit var impl: TestCoverageRepositoryImpl

    @BeforeEach
    fun setUp() {
        fileCoverageDao = mockk(relaxed = true)
        parser          = mockk()
        impl            = TestCoverageRepositoryImpl(fileCoverageDao, parser)
    }

    // ── parseAndStoreCoverage ─────────────────────────────────────────────────

    @Test
    fun `parseAndStoreCoverage calls parser and stores records in DAO`() = runTest {
        val fakeStream = InputStream.nullInputStream()
        val parsed = listOf(
            FileCoverage(
                filePath     = "com/example/MyClass.kt",
                lineCoverage = 0.6667f,
                coveredLines = 20,
                totalLines   = 30,
            ),
        )
        every { parser.parse(any()) } returns parsed
        coEvery { fileCoverageDao.deleteByRepo(any()) } just Runs
        coEvery { fileCoverageDao.upsertAll(any()) } just Runs

        val result = impl.parseAndStoreCoverage("repo-1", fakeStream)

        assertTrue(result.isSuccess)

        val entitiesSlot = slot<List<FileCoverageEntity>>()
        coVerify { fileCoverageDao.deleteByRepo("repo-1") }
        coVerify { fileCoverageDao.upsertAll(capture(entitiesSlot)) }

        val entities = entitiesSlot.captured
        assertEquals(1, entities.size)
        with(entities[0]) {
            assertEquals("repo-1:com/example/MyClass.kt", id)
            assertEquals("repo-1", repoId)
            assertEquals("com/example/MyClass.kt", filePath)
            assertEquals(0.6667f, lineCoverage, 0.001f)
            assertEquals(20, coveredLines)
            assertEquals(30, totalLines)
        }
    }

    @Test
    fun `parseAndStoreCoverage deletes existing rows before inserting new ones`() = runTest {
        every { parser.parse(any()) } returns emptyList()
        coEvery { fileCoverageDao.deleteByRepo(any()) } just Runs
        coEvery { fileCoverageDao.upsertAll(any()) } just Runs

        impl.parseAndStoreCoverage("repo-2", InputStream.nullInputStream())

        // deleteByRepo must be called before upsertAll
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            fileCoverageDao.deleteByRepo("repo-2")
            fileCoverageDao.upsertAll(any())
        }
    }

    @Test
    fun `parseAndStoreCoverage returns failure when parser throws`() = runTest {
        every { parser.parse(any()) } throws RuntimeException("Malformed XML")

        val result = impl.parseAndStoreCoverage("repo-3", InputStream.nullInputStream())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Malformed XML") == true)
    }

    @Test
    fun `parseAndStoreCoverage stores empty list when parser returns no entries`() = runTest {
        every { parser.parse(any()) } returns emptyList()
        coEvery { fileCoverageDao.deleteByRepo(any()) } just Runs
        coEvery { fileCoverageDao.upsertAll(any()) } just Runs

        val result = impl.parseAndStoreCoverage("repo-4", InputStream.nullInputStream())

        assertTrue(result.isSuccess)
        val slot = slot<List<FileCoverageEntity>>()
        coVerify { fileCoverageDao.upsertAll(capture(slot)) }
        assertTrue(slot.captured.isEmpty())
    }

    // ── observeCoverage ───────────────────────────────────────────────────────

    @Test
    fun `observeCoverage maps entity list to FileCoverage domain models`() = runTest {
        val entities = listOf(
            FileCoverageEntity(
                id             = "repo-1:src/Foo.kt",
                repoId         = "repo-1",
                filePath       = "src/Foo.kt",
                lineCoverage   = 0.8f,
                branchCoverage = 0f,
                coveredLines   = 40,
                totalLines     = 50,
                updatedAt      = 0L,
            ),
        )
        every { fileCoverageDao.getByRepo("repo-1") } returns flowOf(entities)

        impl.observeCoverage("repo-1").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            with(list[0]) {
                assertEquals("src/Foo.kt", filePath)
                assertEquals(0.8f, lineCoverage, 0.001f)
                assertEquals(40, coveredLines)
                assertEquals(50, totalLines)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
