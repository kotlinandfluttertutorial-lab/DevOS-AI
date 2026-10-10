package com.devos.ai.data.ai.rag

/**
 * Simple identifier-aware tokenizer for source code.
 *
 * Splits on whitespace, punctuation, and camelCase/snake_case boundaries so
 * that "getUserById" produces tokens ["get", "user", "by", "id"] — far more
 * useful for code retrieval than treating the whole identifier as one term.
 *
 * Stop-words common in code (keywords, common operators) are removed to
 * reduce noise in TF-IDF vectors.
 */
object Tokenizer {

    private val STOP_WORDS = setOf(
        // Kotlin / Java keywords
        "val", "var", "fun", "class", "object", "interface", "enum", "if", "else",
        "when", "for", "while", "do", "return", "throw", "try", "catch", "finally",
        "import", "package", "public", "private", "protected", "internal", "override",
        "abstract", "open", "sealed", "companion", "static", "final", "void",
        "new", "this", "super", "null", "true", "false", "is", "as", "in",
        "it", "set", "init", "constructor", "suspend", "inline", "reified",
        // Common code noise
        "the", "a", "an", "of", "to", "and", "or", "not", "with", "from",
    )

    /**
     * Tokenises [text] into a normalised list of lower-case terms.
     *
     * Process:
     * 1. Strip non-alphanumeric characters (keep letters, digits, underscores)
     * 2. Split camelCase and PascalCase at uppercase boundaries
     * 3. Split on underscores (snake_case)
     * 4. Lowercase everything
     * 5. Remove stop-words and tokens shorter than 2 characters
     */
    fun tokenize(text: String): List<String> {
        // Step 1 — insert space before each uppercase letter (camelCase split)
        val spaced = text
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")

        // Step 2 — replace non-alpha-numeric with space; split on underscores too
        val cleaned = spaced.replace(Regex("[^a-zA-Z0-9]"), " ")

        // Step 3 — lowercase, split on whitespace, filter
        return cleaned
            .lowercase()
            .split(Regex("\\s+"))
            .filter { token ->
                token.length >= 2 && token !in STOP_WORDS && !token.all { it.isDigit() }
            }
    }

    /**
     * Builds a term→index vocabulary from a corpus of documents.
     * Returns an immutable map that can be shared across all chunks in a repo.
     *
     * @param corpus List of tokenised documents (each is a List<String>).
     * @param maxTerms Cap on vocabulary size to bound vector dimensions.
     */
    fun buildVocabulary(
        corpus: List<List<String>>,
        maxTerms: Int = 8_000,
    ): Map<String, Int> {
        // Count document frequency for each term
        val df = mutableMapOf<String, Int>()
        for (doc in corpus) {
            for (term in doc.toSet()) {
                df[term] = (df[term] ?: 0) + 1
            }
        }

        // Keep the top-maxTerms by document frequency; assign stable indices
        return df.entries
            .sortedByDescending { it.value }
            .take(maxTerms)
            .mapIndexed { idx, entry -> entry.key to idx }
            .toMap()
    }

    /**
     * Computes a TF-IDF sparse vector for [tokens] given [vocabulary] and
     * corpus document-frequency counts [df] and document count [n].
     *
     * Returns a list of (termIndex, weight) pairs — only non-zero terms included.
     */
    fun tfidf(
        tokens: List<String>,
        vocabulary: Map<String, Int>,
        df: Map<String, Int>,
        n: Int,
    ): List<Pair<Int, Float>> {
        if (tokens.isEmpty()) return emptyList()

        // Term frequency — normalised by document length
        val tf = mutableMapOf<String, Float>()
        for (t in tokens) tf[t] = (tf[t] ?: 0f) + 1f
        val len = tokens.size.toFloat()

        val result = mutableListOf<Pair<Int, Float>>()
        for ((term, tfRaw) in tf) {
            val idx = vocabulary[term] ?: continue
            val idf = Math.log((n + 1.0) / ((df[term] ?: 0) + 1.0)).toFloat()
            val weight = (tfRaw / len) * idf
            if (weight > 0f) result.add(idx to weight)
        }
        return result
    }
}
