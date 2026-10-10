package com.devos.ai.domain.repository

import com.devos.ai.domain.repository.model.FileCoverage
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

/**
 * Domain contract for the test coverage analysis layer.
 *
 * Implemented by `data-repository` — feature modules must never import the
 * implementation directly; inject this interface via Hilt.
 *
 * DEVOS-049 / DA-60
 */
interface TestCoverageRepository {

    /**
     * Emits the list of per-file coverage records for [repoId] ordered by line
     * coverage ascending (lowest coverage first, for highest-impact triage).
     * Re-emits whenever the underlying table changes.
     */
    fun observeCoverage(repoId: String): Flow<List<FileCoverage>>

    /**
     * Parses a JaCoCo XML report from [xmlInputStream], stores the results in
     * Room, and returns [Result.success] on completion.
     *
     * Replaces any existing rows for [repoId] so stale data is never mixed with
     * the new parse result.
     *
     * @param repoId       The repository these coverage records belong to.
     * @param xmlInputStream  A readable stream of a JaCoCo XML report.
     */
    suspend fun parseAndStoreCoverage(
        repoId: String,
        xmlInputStream: InputStream,
    ): Result<Unit>
}
