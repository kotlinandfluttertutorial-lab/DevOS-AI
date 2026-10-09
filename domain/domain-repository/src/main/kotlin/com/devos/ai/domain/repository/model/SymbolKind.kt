package com.devos.ai.domain.repository.model

/**
 * Kind of a code symbol extracted from a Kotlin or Java file.
 *
 * Stored as a string name in [com.devos.ai.core.database.entity.SymbolEntity.kind].
 * Pure Kotlin — zero Android imports.
 */
enum class SymbolKind(val label: String) {
    CLASS("Class"),
    INTERFACE("Interface"),
    OBJECT("Object"),
    ENUM("Enum"),
    FUNCTION("Function"),
    PROPERTY("Property"),
    CONSTRUCTOR("Constructor"),
    ANNOTATION("Annotation"),
}
