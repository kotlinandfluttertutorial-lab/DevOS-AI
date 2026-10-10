package com.devos.ai.data.ai.rag

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isEmpty
import com.devos.ai.core.database.entity.CodeChunkEntity
import org.junit.jupiter.api.Test

/**
 * Unit tests for [VectorStore].
 */
class VectorStoreTest {

    private val vocabSize = 10

    private fun chunk(
        id: String,
        repoId: String = "repo1",
        filePath: String = "Foo.kt",
        lineStart: Int = 1,
        embeddingJson: String? = null,
    ) = CodeChunkEntity(
        id            = id,
        repoId        = repoId,
        filePath      = filePath,
        lineStart     = lineStart,
        lineEnd       = lineStart + 10,
        content       = "fun $id() {}",
        language      = "kotlin",
        embeddingJson = embeddingJson,
        indexedAt     = 0L,
    )

    private fun sparseJson(vararg pairs: Pair<Int, Float>): String =
        VectorStore.toJson(pairs.toList())

    // ── toJson / parseEmbedding round-trip ─────────────────────────────────

    @Test
    fun `toJson and parseEmbedding round-trip correctly`() {
        val pairs = listOf(0 to 0.5f, 3 to 0.8f, 7 to 0.2f)
        val json  = VectorStore.toJson(pairs)
        val dense = VectorStore.parseEmbedding(json, vocabSize)!!

        assertThat(dense[0]).isBetween(0.49f, 0.51f)
        assertThat(dense[3]).isBetween(0.79f, 0.81f)
        assertThat(dense[7]).isBetween(0.19f, 0.21f)
        // Unset indices should be 0
        assertThat(dense[1]).isEqualTo(0f)
    }

    @Test
    fun `parseEmbedding returns null for malformed json`() {
        assertThat(VectorStore.parseEmbedding("not_json", vocabSize)).isEqualTo(null)
        assertThat(VectorStore.parseEmbedding("{}", vocabSize)).isEqualTo(null)
    }

    @Test
    fun `parseEmbedding ignores out-of-bounds indices`() {
        // Index 100 is beyond vocabSize=10 — should be silently ignored
        val json  = VectorStore.toJson(listOf(100 to 1.0f, 2 to 0.5f))
        val dense = VectorStore.parseEmbedding(json, vocabSize)!!
        assertThat(dense[2]).isBetween(0.49f, 0.51f)
    }

    // ── from ──────────────────────────────────────────────────────────────

    @Test
    fun `from skips chunks without embeddingJson`() {
        val chunks = listOf(
            chunk("a", embeddingJson = null),
            chunk("b", embeddingJson = sparseJson(0 to 1.0f)),
        )
        val store = VectorStore.from(chunks, vocabSize)
        // Only one chunk has an embedding — search should have 1 candidate
        val query = FloatArray(vocabSize) { if (it == 0) 1.0f else 0f }
        val results = store.search(query, topK = 10, minRelevance = 0f)
        assertThat(results).hasSize(1)
    }

    // ── search ────────────────────────────────────────────────────────────

    @Test
    fun `search returns most similar chunk first`() {
        // chunk-a: strong signal at dim 0; chunk-b: strong signal at dim 1
        val chunks = listOf(
            chunk("a", embeddingJson = sparseJson(0 to 1.0f, 1 to 0.1f)),
            chunk("b", embeddingJson = sparseJson(0 to 0.1f, 1 to 1.0f)),
        )
        val store = VectorStore.from(chunks, vocabSize)

        // Query is close to chunk-a (high on dim 0)
        val query = FloatArray(vocabSize) { if (it == 0) 1.0f else 0f }
        val results = store.search(query, topK = 2, minRelevance = 0f)

        assertThat(results).hasSize(2)
        assertThat(results[0].first.id).isEqualTo("a")  // best match first
        assertThat(results[1].first.id).isEqualTo("b")
    }

    @Test
    fun `search respects topK limit`() {
        val chunks = (1..10).map { i ->
            chunk("c$i", embeddingJson = sparseJson(i - 1 to 1.0f))
        }
        val store  = VectorStore.from(chunks, vocabSize)
        val query  = FloatArray(vocabSize) { 1.0f }
        val result = store.search(query, topK = 3, minRelevance = 0f)
        assertThat(result).hasSize(3)
    }

    @Test
    fun `search filters by minRelevance`() {
        val chunks = listOf(
            chunk("high", embeddingJson = sparseJson(0 to 1.0f)),
            chunk("low",  embeddingJson = sparseJson(5 to 1.0f)),
        )
        val store = VectorStore.from(chunks, vocabSize)
        val query = FloatArray(vocabSize) { if (it == 0) 1.0f else 0f }

        // High threshold — only "high" should pass
        val results = store.search(query, topK = 10, minRelevance = 0.9f)
        assertThat(results).hasSize(1)
        assertThat(results[0].first.id).isEqualTo("high")
    }

    @Test
    fun `search returns empty for zero query vector`() {
        val chunks  = listOf(chunk("a", embeddingJson = sparseJson(0 to 1.0f)))
        val store   = VectorStore.from(chunks, vocabSize)
        val results = store.search(FloatArray(vocabSize), topK = 10, minRelevance = 0f)
        assertThat(results).isEmpty()
    }

    @Test
    fun `search returns empty when store has no embedded chunks`() {
        val store   = VectorStore.from(emptyList(), vocabSize)
        val query   = FloatArray(vocabSize) { 1.0f }
        val results = store.search(query, topK = 5, minRelevance = 0f)
        assertThat(results).isEmpty()
    }

    // ── Cosine similarity sanity checks ──────────────────────────────────

    @Test
    fun `identical vectors score 1_0`() {
        val vec   = sparseJson(0 to 1.0f, 2 to 0.5f)
        val chunk = chunk("same", embeddingJson = vec)
        val store = VectorStore.from(listOf(chunk), vocabSize)

        val dense = VectorStore.parseEmbedding(vec, vocabSize)!!
        val results = store.search(dense, topK = 1, minRelevance = 0f)
        assertThat(results[0].second).isBetween(0.99f, 1.01f)
    }

    @Test
    fun `orthogonal vectors score 0_0`() {
        // chunk at dim 0; query at dim 1 — orthogonal
        val chunk   = chunk("orth", embeddingJson = sparseJson(0 to 1.0f))
        val store   = VectorStore.from(listOf(chunk), vocabSize)
        val query   = FloatArray(vocabSize) { if (it == 1) 1.0f else 0f }
        val results = store.search(query, topK = 10, minRelevance = 0.01f)
        assertThat(results).isEmpty()
    }
}
