package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity storing a code chunk for Retrieval-Augmented Generation (RAG).
 *
 * A chunk is a contiguous slice of source text (typically 20–60 lines) with
 * an overlap with neighbouring chunks so context is preserved across splits.
 *
 * [embeddingJson] stores the TF-IDF sparse vector as a JSON array of
 * `[[termIndex, weight], ...]` pairs so the full float[] round-trips through
 * the DB without a separate blob column or external vector DB dependency.
 * For the MVP this gives good-enough retrieval quality on a mobile device;
 * DEVOS-069 can replace it with a proper embedding model later.
 *
 * A [ForeignKey] to [RepositoryEntity] with CASCADE delete keeps the table
 * clean when a repository is removed.
 */
@Entity(
    tableName = "code_chunks",
    foreignKeys = [
        ForeignKey(
            entity = RepositoryEntity::class,
            parentColumns = ["id"],
            childColumns  = ["repoId"],
            onDelete      = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("repoId"),
        Index("filePath"),
    ],
)
data class CodeChunkEntity(
    /** Composite key: "$repoId:$filePath:$lineStart" */
    @PrimaryKey val id: String,
    val repoId: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    /** Raw source text for this chunk — shown as evidence in AI answers. */
    val content: String,
    /** Source language detected from file extension, e.g. "kotlin" or "java". */
    val language: String,
    /**
     * TF-IDF sparse vector serialised as a compact JSON string.
     * Format: `[[termIndex,weight],...]` — parsed by [VectorStore].
     * Null until the embedding step runs.
     */
    val embeddingJson: String?,
    /** Epoch-milliseconds of the last time this chunk was (re-)embedded. */
    val indexedAt: Long = 0L,
)
