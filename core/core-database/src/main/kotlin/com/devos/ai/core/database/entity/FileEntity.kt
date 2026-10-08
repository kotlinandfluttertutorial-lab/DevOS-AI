package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a single file discovered during repository indexing.
 *
 * [id] is a composite "$repoId:$relativePath" string, ensuring uniqueness
 * across repositories without a compound primary key.
 *
 * A [ForeignKey] to [RepositoryEntity] with CASCADE delete keeps the table
 * clean when a repository is removed.
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
    /** Composite key: "$repoId:$path" */
    @PrimaryKey val id: String,
    val repoId: String,
    /** Repository-relative path, e.g. "src/main/kotlin/Foo.kt" */
    val path: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    /** Epoch-milliseconds from [java.io.File.lastModified]. */
    val lastModified: Long,
    /** SHA-256 hex digest used for incremental re-indexing. */
    val contentHash: String,
)
