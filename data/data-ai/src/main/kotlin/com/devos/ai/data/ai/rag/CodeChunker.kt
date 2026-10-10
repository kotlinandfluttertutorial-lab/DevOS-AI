package com.devos.ai.data.ai.rag

import com.devos.ai.core.database.entity.CodeChunkEntity

/**
 * Splits source file text into overlapping chunks suitable for TF-IDF
 * embedding and RAG retrieval.
 *
 * ## Strategy
 * - Default chunk size: 40 lines with a 10-line overlap between adjacent chunks.
 *   This keeps each chunk under ~2 KB of text while preserving enough context
 *   for the AI to understand a function or class declaration.
 * - Files shorter than [chunkSize] lines are stored as a single chunk.
 * - Chunks are keyed by `"$repoId:$filePath:$lineStart"` to match the pattern
 *   used by [com.devos.ai.data.repository.extractor.SymbolExtractor].
 *
 * ## Language detection
 * Language is derived from the file extension. The list covers all extensions
 * that [com.devos.ai.data.repository.extractor.SymbolExtractor] processes.
 */
object CodeChunker {

    private const val DEFAULT_CHUNK_SIZE    = 40   // lines per chunk
    private const val DEFAULT_CHUNK_OVERLAP = 10   // overlap with previous chunk

    /**
     * Produces a list of [CodeChunkEntity] rows (without embeddings — those are
     * added by [com.devos.ai.data.ai.rag.RAGRepositoryImpl.indexChunks]).
     *
     * @param repoId    Parent repository ID.
     * @param filePath  Repository-relative file path.
     * @param source    Full text of the source file.
     * @param chunkSize Lines per chunk.
     * @param overlap   Lines of overlap between consecutive chunks.
     */
    fun chunk(
        repoId: String,
        filePath: String,
        source: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_CHUNK_OVERLAP,
    ): List<CodeChunkEntity> {
        val lines    = source.lines()
        val language = detectLanguage(filePath)
        val result   = mutableListOf<CodeChunkEntity>()

        var start = 0
        while (start < lines.size) {
            val end        = minOf(start + chunkSize, lines.size)
            val chunkLines = lines.subList(start, end)
            val content    = chunkLines.joinToString("\n")
            val lineStart  = start + 1           // 1-based
            val lineEnd    = end                  // inclusive

            result.add(
                CodeChunkEntity(
                    id            = "$repoId:$filePath:$lineStart",
                    repoId        = repoId,
                    filePath      = filePath,
                    lineStart     = lineStart,
                    lineEnd       = lineEnd,
                    content       = content,
                    language      = language,
                    embeddingJson = null,  // filled in by RAGRepositoryImpl.embed()
                    indexedAt     = 0L,
                ),
            )

            if (end >= lines.size) break
            start += chunkSize - overlap
        }

        return result
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun detectLanguage(filePath: String): String = when {
        filePath.endsWith(".kt")     -> "kotlin"
        filePath.endsWith(".java")   -> "java"
        filePath.endsWith(".swift")  -> "swift"
        filePath.endsWith(".py")     -> "python"
        filePath.endsWith(".ts")     -> "typescript"
        filePath.endsWith(".tsx")    -> "typescript"
        filePath.endsWith(".js")     -> "javascript"
        filePath.endsWith(".jsx")    -> "javascript"
        filePath.endsWith(".cpp")    -> "cpp"
        filePath.endsWith(".c")      -> "c"
        filePath.endsWith(".cs")     -> "csharp"
        filePath.endsWith(".go")     -> "go"
        filePath.endsWith(".rs")     -> "rust"
        filePath.endsWith(".rb")     -> "ruby"
        filePath.endsWith(".md")     -> "markdown"
        filePath.endsWith(".gradle") ||
        filePath.endsWith(".kts")    -> "gradle"
        filePath.endsWith(".xml")    -> "xml"
        filePath.endsWith(".json")   -> "json"
        filePath.endsWith(".yaml") ||
        filePath.endsWith(".yml")    -> "yaml"
        else                         -> "text"
    }
}
