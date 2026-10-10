package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for per-file JaCoCo / Kover coverage data.
 *
 * [id] is a composite key of "$repoId:$filePath" for uniqueness.
 * [lineCoverage] and [branchCoverage] are stored as Float in [0.0, 1.0].
 * [updatedAt] — epoch-milliseconds UTC of the last parse run.
 *
 * DEVOS-049 / DA-60
 */
@Entity(
    tableName = "file_coverage",
    indices = [Index("repoId")],
)
data class FileCoverageEntity(
    @PrimaryKey val id: String,           // "$repoId:$filePath"
    val repoId: String,
    val filePath: String,
    val lineCoverage: Float,              // 0.0 – 1.0
    val branchCoverage: Float,            // 0.0 – 1.0
    val coveredLines: Int,
    val totalLines: Int,
    val updatedAt: Long,                  // epoch-millis UTC
)
