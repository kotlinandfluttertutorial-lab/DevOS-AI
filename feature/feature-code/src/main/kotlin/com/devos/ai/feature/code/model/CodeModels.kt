package com.devos.ai.feature.code.model

/**
 * Stub data models for Code Intelligence feature screens.
 * These are UI-layer models — not domain models.
 *
 * Full domain integration happens in a follow-up data layer ticket.
 */

// ── File Explorer ─────────────────────────────────────────────────────────────

enum class FileItemType { FOLDER, KOTLIN, XML, GRADLE, OTHER }

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val type: FileItemType,
    val sizeLabel: String = "",
    val lastModified: String = "",
    val fileCount: Int? = null,
)

// ── Code Viewer ───────────────────────────────────────────────────────────────

data class CodeLine(
    val lineNumber: Int,
    val content: String,
    val isHighlighted: Boolean = false,
)

// ── Code Search ───────────────────────────────────────────────────────────────

data class SearchResult(
    val fileName: String,
    val filePath: String,
    val lineNumber: Int,
    val lineContent: String,
    val matchStart: Int,
    val matchEnd: Int,
)

// ── Symbol Details ────────────────────────────────────────────────────────────

data class SymbolRef(
    val fileName: String,
    val lineNumber: Int,
    val context: String,
)

data class SymbolMethod(
    val name: String,
    val description: String,
)

data class CodeSymbolUi(
    val id: String,
    val name: String,
    val kind: String,
    val packageName: String,
    val filePath: String,
    val lineNumber: Int,
    val signature: String,
    val aiExplanation: String,
    val references: List<SymbolRef> = emptyList(),
    val methods: List<SymbolMethod> = emptyList(),
)
