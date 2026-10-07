package com.devos.ai.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.entity.RepositoryEntity
import com.devos.ai.domain.repository.model.RepositoryProvider
import com.devos.ai.domain.repository.model.SyncStatus
import com.devos.ai.domain.repository.model.SyncStep
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [RepositoryRepositoryImpl].
 *
 * WorkManager is mocked via MockK — no Android runtime needed.
 * Flow assertions use Turbine.
 */
class RepositoryRepositoryImplTest {

    private lateinit var workManager: WorkManager
    private lateinit var repositoryDao: RepositoryDao
    private lateinit var fileDao: FileDao
    private lateinit var impl: RepositoryRepositoryImpl

    @BeforeEach
    fun setUp() {
        workManager   = mockk(relaxed = true)
        repositoryDao = mockk(relaxed = true)
        fileDao       = mockk(relaxed = true)

        // WorkManager.getInstance() is static — inject via reflection-safe wrapper
        // RepositoryRepositoryImpl exposes workManager as a lazy property; we
        // replace it here by subclassing in the test.
        impl = TestableRepositoryRepositoryImpl(
            workManager   = workManager,
            repositoryDao = repositoryDao,
            fileDao       = fileDao,
        )
    }

    // ── importRepository ──────────────────────────────────────────────────────

    @Test
    fun `importRepository inserts entity with IDLE status before enqueuing work`() = runTest {
        coEvery { repositoryDao.upsert(any()) } just Runs

        val result = impl.importRepository(
            "https://github.com/acme/myapp.git",
            RepositoryProvider.GITHUB,
        )

        assertTrue(result.isSuccess)
        val repoId = result.getOrThrow()
        assertNotNull(repoId)

        val entitySlot = slot<RepositoryEntity>()
        coVerify { repositoryDao.upsert(capture(entitySlot)) }

        with(entitySlot.captured) {
            assertEquals(repoId, id)
            assertEquals("myapp", name)
            assertEquals("acme", owner)
            assertEquals(SyncStatus.IDLE.name, syncStatus)
            assertEquals("GITHUB", provider)
        }
    }

    @Test
    fun `importRepository enqueues unique WorkManager job with REPLACE policy`() = runTest {
        coEvery { repositoryDao.upsert(any()) } just Runs

        val result = impl.importRepository(
            "https://github.com/acme/myapp.git",
            RepositoryProvider.GITHUB,
        )

        val repoId = result.getOrThrow()

        verify {
            workManager.enqueueUniqueWork(
                "index_$repoId",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun `importRepository parses SSH URL correctly`() = runTest {
        coEvery { repositoryDao.upsert(any()) } just Runs

        impl.importRepository("git@github.com:octocat/hello-world.git", RepositoryProvider.GITHUB)

        val entitySlot = slot<RepositoryEntity>()
        coVerify { repositoryDao.upsert(capture(entitySlot)) }

        assertEquals("octocat", entitySlot.captured.owner)
        assertEquals("hello-world", entitySlot.captured.name)
    }

    // ── cancelSync ────────────────────────────────────────────────────────────

    @Test
    fun `cancelSync cancels unique work and resets status to IDLE`() = runTest {
        coEvery { repositoryDao.updateSyncStatus(any(), any()) } just Runs

        val result = impl.cancelSync("repo-abc")

        assertTrue(result.isSuccess)
        verify { workManager.cancelUniqueWork("index_repo-abc") }
        coVerify { repositoryDao.updateSyncStatus("repo-abc", SyncStatus.IDLE.name) }
    }

    // ── observeSyncProgress ───────────────────────────────────────────────────

    @Test
    fun `observeSyncProgress emits null when work list is empty`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow("index_repo-1") } returns
            flowOf(emptyList())

        impl.observeSyncProgress("repo-1").test {
            assertNotNull(awaitItem()) // empty list → null progress
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSyncProgress maps RUNNING WorkInfo with PARSE step to SyncProgress`() = runTest {
        val workInfo = buildWorkInfo(
            state    = WorkInfo.State.RUNNING,
            stepName = SyncStep.PARSE.name,
        )
        every { workManager.getWorkInfosForUniqueWorkFlow("index_repo-1") } returns
            flowOf(listOf(workInfo))

        impl.observeSyncProgress("repo-1").test {
            val progress = awaitItem()
            assertNotNull(progress)
            assertEquals(SyncStep.PARSE, progress?.currentStep)
            assertTrue(progress?.completedSteps?.contains(SyncStep.CLONE) == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSyncProgress emits null progress for CANCELLED state`() = runTest {
        val workInfo = buildWorkInfo(state = WorkInfo.State.CANCELLED)
        every { workManager.getWorkInfosForUniqueWorkFlow("index_repo-1") } returns
            flowOf(listOf(workInfo))

        impl.observeSyncProgress("repo-1").test {
            // CANCELLED maps to null
            val item = awaitItem()
            assertTrue(item == null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSyncProgress maps SUCCEEDED WorkInfo to DONE step`() = runTest {
        val workInfo = buildWorkInfo(state = WorkInfo.State.SUCCEEDED)
        every { workManager.getWorkInfosForUniqueWorkFlow("index_repo-1") } returns
            flowOf(listOf(workInfo))

        impl.observeSyncProgress("repo-1").test {
            val progress = awaitItem()
            assertEquals(SyncStep.DONE, progress?.currentStep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Builds a [WorkInfo] stub without needing a real WorkManager instance. */
    private fun buildWorkInfo(
        state: WorkInfo.State,
        stepName: String? = null,
        errorMessage: String? = null,
    ): WorkInfo {
        val progressData = if (stepName != null) {
            androidx.work.workDataOf(
                com.devos.ai.data.repository.worker.RepositoryIndexingWorker.KEY_STEP to stepName,
            )
        } else {
            androidx.work.Data.EMPTY
        }
        val outputData = if (errorMessage != null) {
            androidx.work.workDataOf(
                com.devos.ai.data.repository.worker.RepositoryIndexingWorker.KEY_ERROR to errorMessage,
            )
        } else {
            androidx.work.Data.EMPTY
        }
        // WorkInfo(id, state, tags, outputData, progress, runAttemptCount, generation,
        //          constraints, initialDelayMillis, periodicityInfo, nextScheduleTimeMillis,
        //          stopReason, workerClassName)
        return WorkInfo(
            /* id                    */ java.util.UUID.randomUUID(),
            /* state                 */ state,
            /* tags                  */ emptySet(),
            /* outputData            */ outputData,
            /* progress              */ progressData,
            /* runAttemptCount       */ 1,
            /* generation            */ 1,
            /* constraints           */ androidx.work.Constraints.NONE,
            /* initialDelayMillis    */ 0L,
            /* periodicityInfo       */ null,
            /* nextScheduleTimeMillis*/ Long.MAX_VALUE,
            /* stopReason            */ WorkInfo.STOP_REASON_NOT_STOPPED,
            /* workerClassName       */ null,
        )
    }
}

/**
 * Test subclass that replaces the internal [WorkManager] instance with a mock.
 *
 * [RepositoryRepositoryImpl] lazily calls [WorkManager.getInstance]; in unit
 * tests there is no Android runtime, so we override the property instead.
 */
private class TestableRepositoryRepositoryImpl(
    private val workManager: WorkManager,
    repositoryDao: RepositoryDao,
    fileDao: FileDao,
) : RepositoryRepositoryImpl(
    context       = mockk(relaxed = true),
    repositoryDao = repositoryDao,
    fileDao       = fileDao,
) {
    // Shadow the lazy property from the parent
    override fun getWorkManager(): WorkManager = workManager
}
