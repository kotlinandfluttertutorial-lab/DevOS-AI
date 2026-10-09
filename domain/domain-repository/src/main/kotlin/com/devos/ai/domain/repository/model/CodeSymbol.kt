package com.devos.ai.domain.repository.model

/**
 * Domain model for a code symbol extracted from a Kotlin or Java source file.
 *
 * Pure Kotlin — zero Android imports.
 *
 * @param id          Composite key: `"$repoId:$filePath:$name:$lineStart"`
 * @param repoId      Parent repository ID
 * @param name        Simple symbol name, e.g. "MyClass" or "doSomething"
 * @param kind        [SymbolKind] — class, function, interface, etc.
 * @param filePath    Repository-relative path, e.g. "src/main/kotlin/Foo.kt"
 * @param lineStart   1-based line number where the symbol declaration begins
 * @param lineEnd     1-based line number where the declaration ends (best-effort)
 * @param signature   Full declaration string including parameters/return type
 * @param docComment  KDoc or Javadoc extracted from the preceding comment block
 * @param visibility  [SymbolVisibility] — public/internal/protected/private
 */
data class CodeSymbol(
    val id: String,
    val repoId: String,
    val name: String,
    val kind: SymbolKind,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val signature: String?,
    val docComment: String?,
    val visibility: SymbolVisibility,
)
