package com.devos.ai.data.ai.rag

import com.devos.ai.core.database.entity.CodeChunkEntity
import org.json.JSONArray
import timber.log.Timber
import kotlin.math.sqrt

/**
 * In-memory vector store for TF-IDF cosine-similarity retrieval.
 *
 * All chunks for a repository are loaded once into memory and searched
 * with a linear scan. For the typical DevOS repository (≤10 k files,
 * ≤50 k chunks) this takes <50 ms on a mid-range Android device and
 * eliminates the need for an external vector database.
 *
 * Thread-safety: [VectorStore] instances are stateless — every call to
 * [search] creates a fresh working set. Build one per retrieval call via
 * the factory [from].
 */
class VectorStore private constructor(
    private val entries: List<Entry>,
) {

    data class Entry(
        val chunk: CodeChunkEntity,
        val vector: FloatArray,   // dense; index = vocabulary term index
    )

    /**
     * Returns the [topK] chunks most similar to [queryVector], all scoring
     * above [minRelevance] (0–1 cosine similarity).
     */
    fun search(
        queryVector: FloatArray,
        topK: Int,
        minRelevance: Float,
    ): List<Pair<CodeChunkEntity, Float>> {
        val qNorm = norm(queryVector)
        if (qNorm == 0f) return emptyList()

        return entries
            .map { entry ->
                val score = cosine(queryVector, qNorm, entry.vector)
                entry.chunk to score
            }
            .filter { (_, score) -> score >= minRelevance }
            .sortedByDescending { (_, score) -> score }
            .take(topK)
    }

    // ── Math helpers ──────────────────────────────────────────────────────────

    private fun cosine(a: FloatArray, aNorm: Float, b: FloatArray): Float {
        var dot = 0f
        val len = minOf(a.size, b.size)
        for (i in 0 until len) dot += a[i] * b[i]
        val bNorm = norm(b)
        return if (bNorm == 0f) 0f else dot / (aNorm * bNorm)
    }

    private fun norm(v: FloatArray): Float {
        var sum = 0f
        for (x in v) sum += x * x
        return sqrt(sum)
    }

    companion object {

        /**
         * Builds a [VectorStore] from a list of [CodeChunkEntity] rows.
         *
         * Chunks without a stored [CodeChunkEntity.embeddingJson] are skipped
         * (they have not been embedded yet).
         *
         * @param chunks   Entities loaded from Room.
         * @param vocabSize Number of vocabulary terms (dimension of each vector).
         */
        fun from(chunks: List<CodeChunkEntity>, vocabSize: Int): VectorStore {
            val entries = chunks.mapNotNull { chunk ->
                val json = chunk.embeddingJson ?: return@mapNotNull null
                val vector = parseEmbedding(json, vocabSize) ?: return@mapNotNull null
                Entry(chunk, vector)
            }
            return VectorStore(entries)
        }

        /**
         * Serialises sparse TF-IDF pairs to the JSON format stored in
         * [CodeChunkEntity.embeddingJson]: `[[termIdx,weight],...]`.
         */
        fun toJson(pairs: List<Pair<Int, Float>>): String {
            val arr = JSONArray()
            for ((idx, w) in pairs) {
                arr.put(JSONArray().put(idx).put(w.toDouble()))
            }
            return arr.toString()
        }

        /**
         * Parses [CodeChunkEntity.embeddingJson] back to a dense float array.
         * Returns null if the JSON is malformed.
         */
        fun parseEmbedding(json: String, vocabSize: Int): FloatArray? {
            return try {
                val arr   = JSONArray(json)
                val dense = FloatArray(vocabSize)
                for (i in 0 until arr.length()) {
                    val pair  = arr.getJSONArray(i)
                    val idx   = pair.getInt(0)
                    val w     = pair.getDouble(1).toFloat()
                    if (idx in 0 until vocabSize) dense[idx] = w
                }
                dense
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse embeddingJson")
                null
            }
        }
    }
}
