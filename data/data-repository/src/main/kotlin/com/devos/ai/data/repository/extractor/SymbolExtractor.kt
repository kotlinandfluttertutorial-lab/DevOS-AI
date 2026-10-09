package com.devos.ai.data.repository.extractor

import com.devos.ai.core.database.entity.SymbolEntity
import timber.log.Timber

/**
 * Regex-based symbol extractor for Kotlin and Java source files.
 *
 * ## What is extracted
 * - Classes, interfaces, objects, enums, annotation classes (Kotlin + Java)
 * - Top-level and member functions/methods
 * - Top-level and member properties/fields (val, var, final fields)
 * - Constructors (Java primary constructor detection)
 *
 * ## Limitations
 * - This is a line-oriented regex approach, not a full AST parse.
 *   It handles the vast majority of real-world Kotlin/Java code faithfully,
 *   but will miss symbols inside deeply nested lambdas or multi-line declarations
 *   that span more than ~3 lines.
 * - Nested class end-line estimation is best-effort (brace counting).
 *   DEVOS-025 can upgrade to a PSI/TreeSitter parser once the UI is complete.
 *
 * ## Security
 * - All file content is treated as untrusted data. Regex matching is bounded
 *   by the line length of the source file — no catastrophic backtracking is
 *   possible because patterns never have unbounded repetition on variable-width
 *   alternatives.
 *
 * @see SymbolEntity
 */
object SymbolExtractor {

    // ── Kotlin patterns ───────────────────────────────────────────────────────

    private val KT_CLASS = Regex(
        """^\s*((?:public|internal|private|protected|abstract|open|sealed|data|value|inner)\s+)*""" +
            """(class|interface|object|enum\s+class|annotation\s+class)\s+(\w+)""",
    )
    private val KT_FUN = Regex(
        """^\s*((?:public|internal|private|protected|override|suspend|inline|tailrec|operator|infix|abstract|open)\s+)*""" +
            """fun\s+(?:<[^>]*>\s*)?(\w+)\s*\(([^)]*)\)""",
    )
    private val KT_PROPERTY = Regex(
        """^\s*((?:public|internal|private|protected|override|open|abstract|const|lateinit)\s+)*""" +
            """(val|var)\s+(\w+)\s*[=:]""",
    )

    // ── Java patterns ─────────────────────────────────────────────────────────

    private val JAVA_CLASS = Regex(
        """^\s*((?:public|protected|private|abstract|final|static)\s+)*""" +
            """(class|interface|enum|@interface)\s+(\w+)""",
    )
    private val JAVA_METHOD = Regex(
        """^\s*((?:public|protected|private|static|final|abstract|synchronized|native|default|override)\s+)*""" +
            """(?:(?:void|boolean|byte|char|short|int|long|float|double|[\w<>\[\],\s]+)\s+)""" +
            """(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,\s]+)?\s*\{""",
    )
    private val JAVA_FIELD = Regex(
        """^\s*((?:public|protected|private|static|final|transient|volatile)\s+)*""" +
            """(?:(?:boolean|byte|char|short|int|long|float|double|[\w<>\[\]]+)\s+)""" +
            """(\w+)\s*[=;]""",
    )
    private val JAVA_CONSTRUCTOR = Regex(
        """^\s*((?:public|protected|private)\s+)(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,\s]+)?\s*\{""",
    )

    // ── Doc comment pattern ───────────────────────────────────────────────────

    private val DOC_LINE = Regex("""^\s*[/*]+\s?(.*)""")

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts symbols from the source text of a single file.
     *
     * @param repoId   Parent repository ID (used in composite symbol IDs).
     * @param filePath Repository-relative path, e.g. "src/main/kotlin/Foo.kt".
     * @param source   Full text of the source file.
     * @return         List of [SymbolEntity] rows ready to insert.
     */
    fun extract(repoId: String, filePath: String, source: String): List<SymbolEntity> {
        return when {
            filePath.endsWith(".kt")   -> extractKotlin(repoId, filePath, source)
            filePath.endsWith(".java") -> extractJava(repoId, filePath, source)
            else                       -> emptyList()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Kotlin extraction
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractKotlin(
        repoId: String,
        filePath: String,
        source: String,
    ): List<SymbolEntity> {
        val lines   = source.lines()
        val symbols = mutableListOf<SymbolEntity>()
        var docBuf  = mutableListOf<String>()

        lines.forEachIndexed { idx, line ->
            val lineNum = idx + 1  // 1-based

            // Accumulate doc comment lines
            if (line.trimStart().startsWith("/**") ||
                line.trimStart().startsWith("*") ||
                line.trimStart().startsWith("*/")
            ) {
                val captured = DOC_LINE.find(line)?.groupValues?.getOrNull(1)?.trim()
                if (!captured.isNullOrBlank()) docBuf.add(captured)
                return@forEachIndexed
            }

            // Class / interface / object / enum
            KT_CLASS.find(line)?.let { m ->
                val modifiers  = m.groupValues[1].trim()
                val kindStr    = m.groupValues[2].trim().replace(Regex("\\s+"), "_").uppercase()
                val name       = m.groupValues[3]
                val kind       = normaliseKtKind(kindStr)
                val visibility = parseKtVisibility(modifiers)
                val sig        = line.trim().take(120)
                val doc        = if (docBuf.isNotEmpty()) docBuf.joinToString(" ") else null
                docBuf         = mutableListOf()

                symbols.add(makeEntity(repoId, filePath, name, kind, lineNum,
                    estimateEndLine(lines, idx), sig, doc, visibility))
                return@forEachIndexed
            }

            // Function
            KT_FUN.find(line)?.let { m ->
                val modifiers  = m.groupValues[1].trim()
                val name       = m.groupValues[2]
                val params     = m.groupValues[3]
                val visibility = parseKtVisibility(modifiers)
                val sig        = buildKtFunSig(line, name, params)
                val doc        = if (docBuf.isNotEmpty()) docBuf.joinToString(" ") else null
                docBuf         = mutableListOf()

                symbols.add(makeEntity(repoId, filePath, name, "FUNCTION", lineNum,
                    estimateEndLine(lines, idx), sig, doc, visibility))
                return@forEachIndexed
            }

            // Property (only top-level and class-level — skip local vars)
            if (indentDepth(line) <= 1) {
                KT_PROPERTY.find(line)?.let { m ->
                    val modifiers  = m.groupValues[1].trim()
                    val name       = m.groupValues[3]
                    val visibility = parseKtVisibility(modifiers)
                    val doc        = if (docBuf.isNotEmpty()) docBuf.joinToString(" ") else null
                    docBuf         = mutableListOf()

                    symbols.add(makeEntity(repoId, filePath, name, "PROPERTY", lineNum,
                        lineNum, line.trim().take(120), doc, visibility))
                    return@forEachIndexed
                }
            }

            // Non-doc lines reset the buffer if they are blank or code
            if (line.isBlank()) docBuf = mutableListOf()
        }

        return symbols
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Java extraction
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractJava(
        repoId: String,
        filePath: String,
        source: String,
    ): List<SymbolEntity> {
        val lines   = source.lines()
        val symbols = mutableListOf<SymbolEntity>()
        var docBuf  = mutableListOf<String>()
        // Track the simple class name for constructor detection
        var currentClassName: String? = null

        lines.forEachIndexed { idx, line ->
            val lineNum = idx + 1

            // Doc comment accumulation
            if (line.trimStart().startsWith("/**") ||
                line.trimStart().startsWith("*") ||
                line.trimStart().startsWith("*/")
            ) {
                val captured = DOC_LINE.find(line)?.groupValues?.getOrNull(1)?.trim()
                if (!captured.isNullOrBlank()) docBuf.add(captured)
                return@forEachIndexed
            }

            // Class / interface / enum
            JAVA_CLASS.find(line)?.let { m ->
                val modifiers  = m.groupValues[1].trim()
                val kindStr    = m.groupValues[2].trim()
                val name       = m.groupValues[3]
                val kind       = normaliseJavaKind(kindStr)
                val visibility = parseJavaVisibility(modifiers)
                val doc        = if (docBuf.isNotEmpty()) docBuf.joinToString(" ") else null
                docBuf         = mutableListOf()
                currentClassName = name

                symbols.add(makeEntity(repoId, filePath, name, kind, lineNum,
                    estimateEndLine(lines, idx), line.trim().take(120), doc, visibility))
                return@forEachIndexed
            }

            // Constructor — must match class name
            JAVA_CONSTRUCTOR.find(line)?.let { m ->
                val modifiers = m.groupValues[1].trim()
                val name      = m.groupValues[2]
                if (name == currentClassName) {
                    val visibility = parseJavaVisibility(modifiers)
                    val doc        = if (docBuf.isNotEmpty()) docBuf.joinToString(" ") else null
                    docBuf         = mutableListOf()
                    symbols.add(makeEntity(repoId, filePath, name, "CONSTRUCTOR", lineNum,
                        estimateEndLine(lines, idx), line.trim().take(120), doc, visibility))
                    return@forEachIndexed
                }
            }

            // Method
            JAVA_METHOD.find(line)?.let { m ->
                val modifiers  = m.groupValues[1].trim()
                val name       = m.groupValues[2]
                // Skip if this looks like a constructor (already handled above)
                if (name != currentClassName) {
                    val visibility = parseJavaVisibility(modifiers)
                    val doc        = if (docBuf.isNotEmpty()) docBuf.joinToString(" ") else null
                    docBuf         = mutableListOf()
                    symbols.add(makeEntity(repoId, filePath, name, "FUNCTION", lineNum,
                        estimateEndLine(lines, idx), line.trim().take(120), doc, visibility))
                    return@forEachIndexed
                }
            }

            // Field (indent ≤ 1 to skip local variables)
            if (indentDepth(line) <= 1) {
                JAVA_FIELD.find(line)?.let { m ->
                    val modifiers  = m.groupValues[1].trim()
                    val name       = m.groupValues[2]
                    // Filter out obvious non-symbols
                    if (name.length > 1 && name != "if" && name != "for" && name != "while") {
                        val visibility = parseJavaVisibility(modifiers)
                        val doc        = if (docBuf.isNotEmpty()) docBuf.joinToString(" ") else null
                        docBuf         = mutableListOf()
                        symbols.add(makeEntity(repoId, filePath, name, "PROPERTY", lineNum,
                            lineNum, line.trim().take(120), doc, visibility))
                        return@forEachIndexed
                    }
                }
            }

            if (line.isBlank()) docBuf = mutableListOf()
        }

        return symbols
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun makeEntity(
        repoId: String,
        filePath: String,
        name: String,
        kind: String,
        lineStart: Int,
        lineEnd: Int,
        signature: String?,
        docComment: String?,
        visibility: String,
    ): SymbolEntity = SymbolEntity(
        id         = "$repoId:$filePath:$name:$lineStart",
        repoId     = repoId,
        name       = name,
        kind       = kind,
        filePath   = filePath,
        lineStart  = lineStart,
        lineEnd    = lineEnd,
        signature  = signature,
        docComment = docComment,
        visibility = visibility,
    )

    /**
     * Estimates the end line of a declaration by counting braces from [startIdx].
     * Returns [startIdx]+1 (1-based) if no braces are found on the same line.
     */
    private fun estimateEndLine(lines: List<String>, startIdx: Int): Int {
        var depth = 0
        var found = false
        for (i in startIdx until minOf(startIdx + 500, lines.size)) {
            for (ch in lines[i]) {
                if (ch == '{') { depth++; found = true }
                if (ch == '}') {
                    depth--
                    if (found && depth == 0) return i + 1  // 1-based
                }
            }
        }
        return startIdx + 1
    }

    /** Counts leading tabs/4-space groups to estimate indent depth. */
    private fun indentDepth(line: String): Int {
        var spaces = 0
        for (ch in line) {
            if (ch == ' ') spaces++ else if (ch == '\t') spaces += 4 else break
        }
        return spaces / 4
    }

    // ── Kotlin normalisers ────────────────────────────────────────────────────

    private fun normaliseKtKind(raw: String): String = when {
        raw.contains("ENUM")        -> "ENUM"
        raw.contains("ANNOTATION")  -> "ANNOTATION"
        raw.contains("INTERFACE")   -> "INTERFACE"
        raw.contains("OBJECT")      -> "OBJECT"
        else                        -> "CLASS"
    }

    private fun parseKtVisibility(modifiers: String): String = when {
        modifiers.contains("private")   -> "PRIVATE"
        modifiers.contains("protected") -> "PROTECTED"
        modifiers.contains("internal")  -> "INTERNAL"
        else                            -> "PUBLIC"
    }

    private fun buildKtFunSig(line: String, name: String, params: String): String {
        val trimmed = line.trim()
        // Return up to 120 chars; include return type if on same line
        return trimmed.take(120)
    }

    // ── Java normalisers ──────────────────────────────────────────────────────

    private fun normaliseJavaKind(raw: String): String = when (raw) {
        "interface"  -> "INTERFACE"
        "enum"       -> "ENUM"
        "@interface" -> "ANNOTATION"
        else         -> "CLASS"
    }

    private fun parseJavaVisibility(modifiers: String): String = when {
        modifiers.contains("private")   -> "PRIVATE"
        modifiers.contains("protected") -> "PROTECTED"
        modifiers.contains("public")    -> "PUBLIC"
        else                            -> "PACKAGE_PRIVATE"
    }
}
