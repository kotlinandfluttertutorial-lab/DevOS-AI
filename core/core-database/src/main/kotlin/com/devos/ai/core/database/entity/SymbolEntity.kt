package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a code symbol extracted from an indexed file.
 *
 * Extraction logic is intentionally left empty in DEVOS-015; it will be
 * filled in by DEVOS-023 (Symbol indexing service).
 *
 * [kind] stores a string name from the SymbolKind enum defined in DEVOS-023
 * (e.g. "CLASS", "FUNCTION", "INTERFACE", "PROPERTY", "OBJECT").
 */
@Entity(
    tableName = "symbols",
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
data class SymbolEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    val name: String,
    /** Enum name: CLASS, FUNCTION, INTERFACE, PROPERTY, OBJECT */
    val kind: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val signature: String?,
    val docComment: String?,
    val visibility: String,
)
