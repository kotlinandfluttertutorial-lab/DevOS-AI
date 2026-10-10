package com.devos.ai.data.repository.security

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.devos.ai.core.database.dao.SecurityFindingDao
import com.devos.ai.domain.repository.model.FindingStatus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SecurityRepositoryImpl].
 *
 * WorkManager is mocked via MockK static mocking.
 *
 * DEVOS-047 / DA-58
 */
class SecurityRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var securityFindingDao: SecurityFindingDao
    private lateinit var workManager: WorkManager
    private lateinit var impl: SecurityRepositoryImpl

    @BeforeEach
    fun setUp() {
        context           = mockk(relaxed = true)
        securityFindingDao = mockk(relaxed = true)
        workManager       = mockk(relaxed = true)

        // Static mock for WorkManager.getInstance
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(context) } returns workManager

        impl = SecurityRepositoryImpl(
            context            = context,
            securityFindingDao = securityFindingDao,
        )
    }

    // ── triggerScan ───────────────────────────────────────────────────────────

    @Test
    fun `triggerScan enqueues SecurityScanWorker with REPLACE policy`() = runTest {
        val result = impl.triggerScan("repo-abc")

        assertTrue(result.isSuccess)
        verify {
            workManager.enqueueUniqueWork(
                "security_scan_repo-abc",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun `triggerScan returns success on successful enqueue`() = runTest {
        val result = impl.triggerScan("repo-xyz")
        assertTrue(result.isSuccess)
    }

    // ── markFixed ─────────────────────────────────────────────────────────────

    @Test
    fun `markFixed updates status to FIXED in the DAO`() = runTest {
        coEvery { securityFindingDao.updateStatus(any(), any()) } just Runs

        val result = impl.markFixed("finding-001")

        assertTrue(result.isSuccess)
        coVerify { securityFindingDao.updateStatus("finding-001", FindingStatus.FIXED.name) }
    }

    // ── dismiss ───────────────────────────────────────────────────────────────

    @Test
    fun `dismiss updates status to DISMISSED in the DAO`() = runTest {
        coEvery { securityFindingDao.updateStatus(any(), any()) } just Runs

        val result = impl.dismiss("finding-002")

        assertTrue(result.isSuccess)
        coVerify { securityFindingDao.updateStatus("finding-002", FindingStatus.DISMISSED.name) }
    }
}
