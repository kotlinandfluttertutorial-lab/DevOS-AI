package com.devos.ai.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.devos.ai.core.database.dao.RepositoryDao
import com.devos.ai.core.database.entity.RepositoryEntity
import com.devos.ai.data.repository.worker.RepositoryIndexingWorker
import com.devos.ai.domain.repository.model.RepositoryProvider
import com.devos.ai.domain.repository.model.SyncProgress
import com.devos.ai.domain.repository.model.SyncStatus
import com.devos.ai.domain.repository.model.SyncStep
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import androidx.work.Data

/**
 * Unit tests for [RepositoryRepositoryImpl].
 *
 * WorkManager is mocked via MockK static mocking so we never touch an
 * Android runtime.  RepositoryDao is mocked to isolate the impl logic.
 *
 * Covers:
 * - importRepository enqueues a unique WorkManager job with correct input data
 * - cancelSync calls cancelUniqueWork with the expected tag + resets status
 * - observeSyncProgress maps WorkInfo state → SyncProgress correctly
 */
class RepositoryRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var repositoryDao: RepositoryDao
    private lateinit var workManager: WorkManager
    private lateinit var impl: RepositoryRepositoryImpl

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        context      = mockk(relaxed = true)
        repositoryDao = mockk(relaxed = true)
        workManager  = mockk(relaxed = true)

        // Static mock so WorkManager.getInstance(context) returns our mock.
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager

        impl = RepositoryRepositoryImpl(context, repositoryDao)
    }

    // -------------------------------------------------------------------------
    // importRepository
    // -------------------------------------------------------------------------

    @Test
    fun `importRepository upserts entity and enqueues unique work`() = runTest {
        coEvery { repositoryDao.upsert(any()) } just Runs

        val workNameSlot = slot<String>()
        val policySlot   = slot<ExistingWorkPolicy>()
        val requestSlot  = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(
                capture(workNameSlot),
                capture(policySlot),
                capture(requestSlot),
            )
        } returns mockk(relaxed = true)

        val result = impl.importRepository(
            cloneUrl = "https://github.com/owner/repo.git",
            provider = RepositoryProvider.GITHUB,
        )

        assertThat(result.isSuccess).isEqualTo(true)
        val repoId = result.getOrNull()
        assertThat(repoId).isNotNull()
        assertThat(repoId!!).isNotEmpty()

        // DB write happened
        coVerify(exactly = 1) { repositoryDao.upsert(any()) }

        // WorkManager received a uniqueWork enqueue
        assertThat(workNameSlot.captured).isEqualTo("index_$repoId")
        assertThat(policySlot.captured).isEqualTo(ExistingWorkPolicy.REPLACE)

        // Input data contains required keys
        val inputData = requestSlot.captured.workSpec.input
        assertThat(inputData.getString(RepositoryIndexingWorker.KEY_REPO_ID)).isEqualTo(repoId)
        assertThat(inputData.getString(RepositoryIndexingWorker.KEY_CLONE_URL))
            .isEqualTo("https://github.com/owner/repo.git")
        assertThat(inputData.getString(RepositoryIndexingWorker.KEY_PROVIDER))
            .isEqualTo(RepositoryProvider.GITHUB.name)
    }

    // -------------------------------------------------------------------------
    // cancelSync
    // -------------------------------------------------------------------------

    @Test
    fun `cancelSync calls cancelUniqueWork and resets status to IDLE`() = runTest {
        coEvery { repositoryDao.updateSyncStatus(any(), any()) } just Runs
        every { workManager.cancelUniqueWork(any()) } returns mockk(relaxed = true)

        impl.cancelSync("repo-abc")

        verify(exactly = 1) { workManager.cancelUniqueWork("index_repo-abc") }
        coVerify(exactly = 1) {
            repositoryDao.updateSyncStatus("repo-abc", SyncStatus.IDLE.name)
        }
    }

    // -------------------------------------------------------------------------
    // observeSyncProgress
    // -------------------------------------------------------------------------

    @Test
    fun `observeSyncProgress maps RUNNING WorkInfo with CLONE step to SyncProgress`() = runTest {
        val progressData = Data.Builder()
            .putString(RepositoryIndexingWorker.KEY_STEP, SyncStep.CLONE.name)
            .build()

        val workInfo = buildWorkInfo(WorkInfo.State.RUNNING, progress = progressData)
        every {
            workManager.getWorkInfosForUniqueWorkFlow("index_repo-1")
        } returns flowOf(listOf(workInfo))

        impl.observeSyncProgress("repo-1").test {
            val progress = awaitItem()
            assertThat(progress).isNotNull()
            assertThat(progress!!.currentStep).isEqualTo(SyncStep.CLONE)
            assertThat(progress.errorMessage).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSyncProgress maps SUCCEEDED WorkInfo to DONE step`() = runTest {
        val workInfo = buildWorkInfo(WorkInfo.State.SUCCEEDED)
        every {
            workManager.getWorkInfosForUniqueWorkFlow("index_repo-2")
        } returns flowOf(listOf(workInfo))

        impl.observeSyncProgress("repo-2").test {
            val progress = awaitItem()
            assertThat(progress).isNotNull()
            assertThat(progress!!.currentStep).isEqualTo(SyncStep.DONE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSyncProgress maps FAILED WorkInfo with error message`() = runTest {
        val outputData = Data.Builder()
            .putString(RepositoryIndexingWorker.KEY_ERROR, "Network timeout")
            .build()
        val workInfo = buildWorkInfo(WorkInfo.State.FAILED, output = outputData)
        every {
            workManager.getWorkInfosForUniqueWorkFlow("index_repo-3")
        } returns flowOf(listOf(workInfo))

        impl.observeSyncProgress("repo-3").test {
            val progress = awaitItem()
            assertThat(progress).isNotNull()
            assertThat(progress!!.errorMessage).isEqualTo("Network timeout")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSyncProgress emits null when no work info exists`() = runTest {
        every {
            workManager.getWorkInfosForUniqueWorkFlow("index_repo-none")
        } returns flowOf(emptyList())

        impl.observeSyncProgress("repo-none").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSyncProgress emits null for CANCELLED WorkInfo`() = runTest {
        val workInfo = buildWorkInfo(WorkInfo.State.CANCELLED)
        every {
            workManager.getWorkInfosForUniqueWorkFlow("index_repo-cancel")
        } returns flowOf(listOf(workInfo))

        impl.observeSyncProgress("repo-cancel").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // Helper — construct a WorkInfo via reflection (WorkInfo has no public ctor)
    // -------------------------------------------------------------------------

    private fun buildWorkInfo(
        state: WorkInfo.State,
        progress: Data = Data.EMPTY,
        output: Data = Data.EMPTY,
    ): WorkInfo {
        // WorkInfo constructor: (UUID, State, Data, List<String>, Data, int, int)
        val ctor = WorkInfo::class.java.getDeclaredConstructor(
            java.util.UUID::class.java,
            WorkInfo.State::class.java,
            Data::class.java,
            List::class.java,
            Data::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        ctor.isAccessible = true
        return ctor.newInstance(
            java.util.UUID.randomUUID(),
            state,
            output,
            emptyList<String>(),
            progress,
            0,
            0,
        )
    }
}
