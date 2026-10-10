package com.devos.ai.data.ai.memory

import com.devos.ai.domain.ai.model.MemoryCategory
import com.devos.ai.domain.ai.model.MemorySource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MemoryExtractor].
 *
 * DEVOS-056 / DA-67
 */
class MemoryExtractorTest {

    private lateinit var extractor: MemoryExtractor

    @BeforeEach
    fun setUp() {
        extractor = MemoryExtractor()
    }

    // ── Preference extraction ─────────────────────────────────────────────────

    @Test
    fun `prefer triggers CODE_PREFERENCE entry`() {
        val result = extractor.extractFromConversation(
            userMessage = "I prefer MockK over Mockito for unit tests",
            aiResponse  = "Great choice! MockK is idiomatic for Kotlin.",
        )

        assertNotNull(result)
        val entry = result!!.first()
        assertEquals(MemoryCategory.CODE_PREFERENCE, entry.category)
        assertTrue(entry.content.contains("MockK", ignoreCase = true))
    }

    @Test
    fun `always use triggers CODE_PREFERENCE entry`() {
        val result = extractor.extractFromConversation(
            userMessage = "always use coroutines for async operations",
            aiResponse  = "Agreed, Kotlin coroutines are the standard.",
        )

        assertNotNull(result)
        assertEquals(MemoryCategory.CODE_PREFERENCE, result!!.first().category)
    }

    @Test
    fun `never use triggers CODE_PREFERENCE entry`() {
        val result = extractor.extractFromConversation(
            userMessage = "never use AsyncTask in new code",
            aiResponse  = "Correct, AsyncTask is deprecated.",
        )

        assertNotNull(result)
        assertEquals(MemoryCategory.CODE_PREFERENCE, result!!.first().category)
    }

    // ── Decision extraction ───────────────────────────────────────────────────

    @Test
    fun `decided triggers DECISION entry`() {
        val result = extractor.extractFromConversation(
            userMessage = "We decided to use MVVM architecture for this project",
            aiResponse  = "MVVM is a solid choice with Jetpack.",
        )

        assertNotNull(result)
        assertEquals(MemoryCategory.DECISION, result!!.first().category)
    }

    @Test
    fun `chose triggers DECISION entry`() {
        val result = extractor.extractFromConversation(
            userMessage = "We chose Hilt over Koin for dependency injection",
            aiResponse  = "Hilt integrates well with the Jetpack ecosystem.",
        )

        assertNotNull(result)
        assertEquals(MemoryCategory.DECISION, result!!.first().category)
    }

    // ── Source ────────────────────────────────────────────────────────────────

    @Test
    fun `extracted entries have AI_CHAT source`() {
        val result = extractor.extractFromConversation(
            userMessage = "prefer Kotlin DSL for Gradle",
            aiResponse  = "Kotlin DSL offers type safety.",
        )

        assertNotNull(result)
        result!!.forEach { entry ->
            assertEquals(MemorySource.AI_CHAT, entry.source)
        }
    }

    // ── No memorable content ──────────────────────────────────────────────────

    @Test
    fun `plain conversation with no memorable signals returns null`() {
        val result = extractor.extractFromConversation(
            userMessage = "What is the capital of France?",
            aiResponse  = "Paris is the capital of France.",
        )

        assertNull(result)
    }

    @Test
    fun `empty strings return null`() {
        val result = extractor.extractFromConversation(
            userMessage = "",
            aiResponse  = "",
        )

        assertNull(result)
    }

    // ── Sanitization ──────────────────────────────────────────────────────────

    @Test
    fun `api_key pattern is redacted from stored content`() {
        val result = extractor.extractFromConversation(
            userMessage = "prefer to store api_key=sk-abc123supersecret in config",
            aiResponse  = "That is not recommended.",
        )

        assertNotNull(result)
        val content = result!!.first().content
        // The secret value must not appear verbatim
        assertTrue(
            !content.contains("sk-abc123supersecret"),
            "Secret value should be redacted but content was: $content",
        )
        assertTrue(
            content.contains("[REDACTED]", ignoreCase = true),
            "Content should contain [REDACTED] marker but was: $content",
        )
    }

    @Test
    fun `token pattern is redacted from stored content`() {
        val result = extractor.extractFromConversation(
            userMessage = "decided to use token=ghp_xyz789 for GitHub API",
            aiResponse  = "Tokens should be stored in EncryptedSharedPreferences.",
        )

        assertNotNull(result)
        val content = result!!.first().content
        assertTrue(
            !content.contains("ghp_xyz789"),
            "Token value should be redacted but content was: $content",
        )
    }

    @Test
    fun `password pattern is redacted from stored content`() {
        val sanitized = extractor.sanitize("password=hunter2 is bad practice")
        assertTrue(!sanitized.contains("hunter2"))
        assertTrue(sanitized.contains("[REDACTED]"))
    }

    // ── Content length ────────────────────────────────────────────────────────

    @Test
    fun `content is capped at 500 characters`() {
        val longText = "prefer " + "x".repeat(600)
        val result   = extractor.extractFromConversation(
            userMessage = longText,
            aiResponse  = "",
        )

        assertNotNull(result)
        result!!.forEach { entry ->
            assertTrue(entry.content.length <= 500)
        }
    }

    // ── Entry fields ──────────────────────────────────────────────────────────

    @Test
    fun `entry has non-blank id and createdAt greater than zero`() {
        val result = extractor.extractFromConversation(
            userMessage = "prefer Jetpack Compose over XML layouts",
            aiResponse  = "Compose is the modern approach.",
        )

        assertNotNull(result)
        val entry = result!!.first()
        assertTrue(entry.id.isNotBlank())
        assertTrue(entry.createdAt > 0)
        assertEquals(false, entry.isPinned)
    }
}
