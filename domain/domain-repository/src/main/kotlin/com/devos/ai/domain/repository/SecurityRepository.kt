package com.devos.ai.domain.repository

import com.devos.ai.domain.repository.model.SecurityFinding
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for the security scanning layer.
 *
 * Implemented by `data-repository` — feature modules must never import the
 * implementation directly; inject this interface via Hilt.
 *
 * DEVOS-047 / DA-58
 */
interface SecurityRepository {

    /**
     * Emits the list of security findings for [repoId] ordered by severity then
     * detection time. Re-emits whenever the underlying table changes.
     */
    fun observeFindings(repoId: String): Flow<List<SecurityFinding>>

    /**
     * Enqueues a background SAST scan for [repoId] via WorkManager.
     *
     * @return [Result.success] when the work is successfully enqueued,
     *         [Result.failure] on any internal error.
     */
    suspend fun triggerScan(repoId: String): Result<Unit>

    /**
     * Marks the finding with [findingId] as [FindingStatus.FIXED].
     */
    suspend fun markFixed(findingId: String): Result<Unit>

    /**
     * Marks the finding with [findingId] as [FindingStatus.DISMISSED].
     */
    suspend fun dismiss(findingId: String): Result<Unit>
}
