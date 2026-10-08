package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for an individual source file inside a cloned repository.
 *
 * [id]           — composite key: "$repoId:$relativePath"
 * [contentHash]  — SHA-256 hex of file contents; used for incremental re-indexing.
 */
@Entity(
    tableName = "repository_files",
    foreignKeys = [
        ForeignKey(
            entity = RepositoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["repoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("repoId")],
)
data class FileEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    /** Relative path from repo root, e.g. "src/main/kotlin/Foo.kt" */
    val path: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    /** File last-modified time as epoch-millis. */
    val lastModified: Long,
    /** SHA-256 hex digest of file contents. */
    val contentHash: String,
)
