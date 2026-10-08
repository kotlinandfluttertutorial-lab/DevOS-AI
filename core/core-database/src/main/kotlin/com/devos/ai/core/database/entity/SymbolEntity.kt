package com.devos.ai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a code symbol (class, function, interface, property, object)
 * extracted from a repository file.
 *
 * Extraction logic is intentionally left empty here — DEVOS-023 fills it in.
 *
 * [kind]        — one of: CLASS, FUNCTION, INTERFACE, PROPERTY, OBJECT
 * [visibility]  — one of: PUBLIC, INTERNAL, PROTECTED, PRIVATE
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
    /** CLASS | FUNCTION | INTERFACE | PROPERTY | OBJECT */
    val kind: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    /** Full signature string, e.g. "fun greet(name: String): String" — null if unavailable */
    val signature: String?,
    /** KDoc / Javadoc comment text — null if absent */
    val docComment: String?,
    /** PUBLIC | INTERNAL | PROTECTED | PRIVATE */
    val visibility: String,
)
