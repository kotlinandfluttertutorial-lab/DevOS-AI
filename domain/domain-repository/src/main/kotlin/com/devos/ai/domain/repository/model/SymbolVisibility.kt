package com.devos.ai.domain.repository.model

/**
 * Visibility modifier of a code symbol.
 *
 * Stored as a string name in [com.devos.ai.core.database.entity.SymbolEntity.visibility].
 * Pure Kotlin — zero Android imports.
 */
enum class SymbolVisibility {
    PUBLIC,
    INTERNAL,
    PROTECTED,
    PRIVATE,
    PACKAGE_PRIVATE,  // Java default visibility
}
