package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a SAST or dependency security finding.
 *
 * [severity]  — stored as the enum name string (e.g. "CRITICAL").
 * [status]    — stored as the enum name string (e.g. "OPEN").
 * [detectedAt] — epoch-milliseconds UTC.
 *
 * Foreign key on [repoId] cascades deletes so findings are removed when a
 * repository is deleted from the device.
 *
 * DEVOS-047 / DA-58
 */
@Entity(
    tableName = "security_findings",
    foreignKeys = [
        ForeignKey(
            entity = RepositoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["repoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("repoId"), Index("status")],
)
data class SecurityFindingEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    /** Stored as Severity.name() */
    val severity: String,
    val ruleId: String,
    val title: String,
    val description: String,
    val filePath: String,
    val lineNumber: Int,
    val codeSnippet: String?,
    val cveId: String?,
    /** Stored as FindingStatus.name() */
    val status: String,
    /** Epoch-millis UTC */
    val detectedAt: Long,
)
