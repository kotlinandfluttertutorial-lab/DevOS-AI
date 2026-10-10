package com.devos.ai.data.ai.rag

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import org.junit.jupiter.api.Test

/**
 * Unit tests for [Tokenizer].
 */
class TokenizerTest {

    // ── tokenize ──────────────────────────────────────────────────────────────

    @Test
    fun `tokenize splits camelCase into lower-case terms`() {
        val tokens = Tokenizer.tokenize("getUserById")
        assertThat(tokens).contains("get")
        assertThat(tokens).contains("user")
        assertThat(tokens).contains("by")
        assertThat(tokens).contains("id")
    }

    @Test
    fun `tokenize splits PascalCase`() {
        val tokens = Tokenizer.tokenize("RepositoryViewModel")
        assertThat(tokens).contains("repository")
        assertThat(tokens).contains("view")
        assertThat(tokens).contains("model")
    }

    @Test
    fun `tokenize splits snake_case`() {
        val tokens = Tokenizer.tokenize("sync_status")
        assertThat(tokens).contains("sync")
        assertThat(tokens).contains("status")
    }

    @Test
    fun `tokenize removes stop words`() {
        val tokens = Tokenizer.tokenize("val fun class")
        // All are stop words — should be filtered
        assertThat(tokens).isEmpty()
    }

    @Test
    fun `tokenize removes single-char tokens`() {
        val tokens = Tokenizer.tokenize("a b c foo")
        assertThat(tokens).doesNotContain("a")
        assertThat(tokens).doesNotContain("b")
        assertThat(tokens).doesNotContain("c")
        assertThat(tokens).contains("foo")
    }

    @Test
    fun `tokenize returns empty list for blank input`() {
        assertThat(Tokenizer.tokenize("")).isEmpty()
        assertThat(Tokenizer.tokenize("   ")).isEmpty()
    }

    @Test
    fun `tokenize handles Kotlin source snippet`() {
        val source = "suspend fun fetchUserData(userId: String): Result<User>"
        val tokens = Tokenizer.tokenize(source)
        assertThat(tokens).isNotEmpty()
        assertThat(tokens).contains("fetch")
        assertThat(tokens).contains("user")
        assertThat(tokens).contains("data")
        assertThat(tokens).contains("result")
    }

    // ── buildVocabulary ───────────────────────────────────────────────────────

    @Test
    fun `buildVocabulary assigns unique indices`() {
        val corpus = listOf(
            listOf("alpha", "beta", "gamma"),
            listOf("beta", "delta"),
            listOf("gamma", "epsilon"),
        )
        val vocab = Tokenizer.buildVocabulary(corpus, maxTerms = 100)
        // All indices are unique
        val indices = vocab.values.toList()
        assertThat(indices.toSet().size).isEqualTo(indices.size)
    }

    @Test
    fun `buildVocabulary respects maxTerms cap`() {
        val corpus = (0 until 200).map { i -> listOf("term$i") }
        val vocab  = Tokenizer.buildVocabulary(corpus, maxTerms = 50)
        assertThat(vocab.size).isEqualTo(50)
    }

    @Test
    fun `buildVocabulary ranks by document frequency`() {
        // "common" appears in 3 docs, "rare" in 1
        val corpus = listOf(
            listOf("common", "rare"),
            listOf("common"),
            listOf("common"),
        )
        val vocab = Tokenizer.buildVocabulary(corpus, maxTerms = 10)
        // "common" should have a lower (higher-priority) index than "rare"
        assertThat(vocab["common"]!! < vocab["rare"]!!).isEqualTo(true)
    }

    // ── tfidf ─────────────────────────────────────────────────────────────────

    @Test
    fun `tfidf returns non-empty for known tokens`() {
        val vocab = mapOf("hello" to 0, "world" to 1)
        val df    = mapOf("hello" to 1, "world" to 1)
        val pairs = Tokenizer.tfidf(listOf("hello", "world"), vocab, df, n = 5)
        assertThat(pairs).isNotEmpty()
    }

    @Test
    fun `tfidf returns empty for tokens not in vocabulary`() {
        val vocab = mapOf("known" to 0)
        val df    = mapOf("known" to 1)
        val pairs = Tokenizer.tfidf(listOf("unknown"), vocab, df, n = 5)
        assertThat(pairs).isEmpty()
    }

    @Test
    fun `tfidf assigns higher weight to rare terms`() {
        val vocab  = mapOf("common" to 0, "rare" to 1)
        val df     = mapOf("common" to 10, "rare" to 1)
        val pairs  = Tokenizer.tfidf(listOf("common", "rare"), vocab, df, n = 10)
        val wCommon = pairs.find { it.first == 0 }?.second ?: 0f
        val wRare   = pairs.find { it.first == 1 }?.second ?: 0f
        // Rare term should have higher TF-IDF weight
        assertThat(wRare > wCommon).isEqualTo(true)
    }

    @Test
    fun `tfidf returns empty for empty token list`() {
        val vocab = mapOf("hello" to 0)
        val df    = mapOf("hello" to 1)
        assertThat(Tokenizer.tfidf(emptyList(), vocab, df, n = 5)).isEmpty()
    }
}
