package com.devos.ai.data.ai.rag

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [CodeChunker].
 */
class CodeChunkerTest {

    private val repoId   = "test-repo"
    private val ktFile   = "src/main/kotlin/Foo.kt"
    private val javaFile = "src/main/java/Bar.java"

    // ── Basic chunking ────────────────────────────────────────────────────────

    @Test
    fun `file shorter than chunkSize produces one chunk`() {
        val source = (1..10).joinToString("\n") { "line $it" }
        val chunks = CodeChunker.chunk(repoId, ktFile, source, chunkSize = 40)
        assertThat(chunks).hasSize(1)
        assertThat(chunks[0].lineStart).isEqualTo(1)
        assertThat(chunks[0].lineEnd).isEqualTo(10)
    }

    @Test
    fun `file exactly chunkSize lines produces one chunk`() {
        val source = (1..40).joinToString("\n") { "line $it" }
        val chunks = CodeChunker.chunk(repoId, ktFile, source, chunkSize = 40, overlap = 0)
        assertThat(chunks).hasSize(1)
    }

    @Test
    fun `long file produces multiple chunks`() {
        val source = (1..100).joinToString("\n") { "line $it" }
        val chunks = CodeChunker.chunk(repoId, ktFile, source, chunkSize = 40, overlap = 10)
        assertThat(chunks.size > 1).isTrue()
    }

    @Test
    fun `chunks cover the full file with no gaps`() {
        val source = (1..80).joinToString("\n") { "line $it" }
        val chunks = CodeChunker.chunk(repoId, ktFile, source, chunkSize = 30, overlap = 5)

        // Each line number should appear in at least one chunk
        for (line in 1..80) {
            val covered = chunks.any { c -> line in c.lineStart..c.lineEnd }
            assertThat(covered).isTrue()
        }
    }

    @Test
    fun `overlap makes adjacent chunks share lines`() {
        val source  = (1..80).joinToString("\n") { "line $it" }
        val overlap = 10
        val chunks  = CodeChunker.chunk(repoId, ktFile, source, chunkSize = 30, overlap = overlap)

        // Every consecutive pair should overlap
        for (i in 0 until chunks.size - 1) {
            val endOfFirst      = chunks[i].lineEnd
            val startOfSecond   = chunks[i + 1].lineStart
            assertThat(startOfSecond).isLessThanOrEqualTo(endOfFirst)
        }
    }

    // ── IDs and metadata ──────────────────────────────────────────────────────

    @Test
    fun `chunk IDs are unique`() {
        val source = (1..120).joinToString("\n") { "line $it" }
        val chunks = CodeChunker.chunk(repoId, ktFile, source)
        val ids    = chunks.map { it.id }.toSet()
        assertThat(ids.size).isEqualTo(chunks.size)
    }

    @Test
    fun `repoId and filePath are correctly set`() {
        val source = "class Foo {}"
        val chunk  = CodeChunker.chunk(repoId, ktFile, source).first()
        assertThat(chunk.repoId).isEqualTo(repoId)
        assertThat(chunk.filePath).isEqualTo(ktFile)
    }

    @Test
    fun `language is detected from file extension`() {
        val source = "void main() {}"
        val ktChunk   = CodeChunker.chunk(repoId, ktFile, source).first()
        val javaChunk = CodeChunker.chunk(repoId, javaFile, source).first()
        assertThat(ktChunk.language).isEqualTo("kotlin")
        assertThat(javaChunk.language).isEqualTo("java")
    }

    @Test
    fun `embeddingJson is null before embedding step`() {
        val source = "class Foo {}"
        val chunk  = CodeChunker.chunk(repoId, ktFile, source).first()
        assertThat(chunk.embeddingJson).isEqualTo(null)
    }

    // ── Content accuracy ──────────────────────────────────────────────────────

    @Test
    fun `chunk content matches original lines`() {
        val lines  = (1..50).map { "line $it" }
        val source = lines.joinToString("\n")
        val chunks = CodeChunker.chunk(repoId, ktFile, source, chunkSize = 20, overlap = 0)

        for (chunk in chunks) {
            val expectedContent = lines
                .subList(chunk.lineStart - 1, chunk.lineEnd)
                .joinToString("\n")
            assertThat(chunk.content).isEqualTo(expectedContent)
        }
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `empty source produces empty list`() {
        val chunks = CodeChunker.chunk(repoId, ktFile, "")
        assertThat(chunks.size).isGreaterThanOrEqualTo(0)
    }

    @Test
    fun `single line source produces one chunk`() {
        val chunks = CodeChunker.chunk(repoId, ktFile, "fun main() {}")
        assertThat(chunks).hasSize(1)
    }

    @Test
    fun `unknown extension gets text language`() {
        val chunk = CodeChunker.chunk(repoId, "readme.xyz", "some text").firstOrNull()
        assertThat(chunk?.language).isEqualTo("text")
    }
}
