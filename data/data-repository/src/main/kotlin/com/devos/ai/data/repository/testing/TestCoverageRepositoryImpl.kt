package com.devos.ai.data.repository.testing

import com.devos.ai.core.database.dao.FileCoverageDao
import com.devos.ai.core.database.entity.FileCoverageEntity
import com.devos.ai.domain.repository.TestCoverageRepository
import com.devos.ai.domain.repository.model.FileCoverage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [TestCoverageRepository].
 *
 * Responsibilities:
 * - Delegates JaCoCo XML parsing to [JaCoCoParser].
 * - Maps [FileCoverageEntity] ↔ [FileCoverage] (domain model).
 * - Persists coverage data via [FileCoverageDao].
 *
 * Feature modules must **never** import this class — inject [TestCoverageRepository].
 *
 * DEVOS-049 / DA-60
 */
@Singleton
class TestCoverageRepositoryImpl @Inject constructor(
    private val fileCoverageDao: FileCoverageDao,
    private val parser: JaCoCoParser,
) : TestCoverageRepository {

    // ── Observe ───────────────────────────────────────────────────────────────

    override fun observeCoverage(repoId: String): Flow<List<FileCoverage>> =
        fileCoverageDao.getByRepo(repoId).map { entities ->
            entities.map { entity ->
                FileCoverage(
                    filePath     = entity.filePath,
                    lineCoverage = entity.lineCoverage,
                    coveredLines = entity.coveredLines,
                    totalLines   = entity.totalLines,
                )
            }
        }

    // ── Parse and store ───────────────────────────────────────────────────────

    override suspend fun parseAndStoreCoverage(
        repoId: String,
        xmlInputStream: InputStream,
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val coverageList = parser.parse(xmlInputStream)

            val now = System.currentTimeMillis()
            val entities = coverageList.map { coverage ->
                FileCoverageEntity(
                    id             = "$repoId:${coverage.filePath}",
                    repoId         = repoId,
                    filePath       = coverage.filePath,
                    lineCoverage   = coverage.lineCoverage,
                    branchCoverage = 0f, // JaCoCo LINE counter; branch is a separate counter
                    coveredLines   = coverage.coveredLines,
                    totalLines     = coverage.totalLines,
                    updatedAt      = now,
                )
            }

            // Replace existing coverage for this repo atomically
            fileCoverageDao.deleteByRepo(repoId)
            fileCoverageDao.upsertAll(entities)

            Timber.d(
                "parseAndStoreCoverage: stored %d file coverage records for repo %s",
                entities.size,
                repoId,
            )
        }
    }
}
