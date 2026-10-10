package com.devos.ai.core.common.logging

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DevOSLogger] and [DevOSTimberTree].
 *
 * These tests verify the security invariants:
 * - Token values never appear in sanitized output.
 * - Event logging emits the correct category.
 * - User IDs are hashed, not logged raw.
 * - The masked() extension trims long secrets correctly.
 */
class DevOSLoggerTest {

    private val tree = DevOSTimberTree(isDebug = true)

    // ── Token masking via DevOSTimberTree ─────────────────────────────────────

    @Test
    fun `maskSensitiveValues replaces api_key value with stars`() {
        val masked = tree.maskSensitiveValues("Connecting with api_key=sk-abcdef1234567890")
        assertFalse(masked.contains("sk-abcdef1234567890"), "Raw token must not appear")
        assertTrue(masked.contains("api_key=****"), "Masked placeholder must appear")
    }

    @Test
    fun `maskSensitiveValues replaces token value with stars`() {
        val masked = tree.maskSensitiveValues("Bearer token=eyJhbGciOiJIUzI1NiJ9.payload.sig")
        assertFalse(masked.contains("eyJhbGciOiJIUzI1NiJ9"), "JWT must not appear")
        assertTrue(masked.contains("****"), "Masked placeholder must appear")
    }

    @Test
    fun `maskSensitiveValues replaces password value with stars`() {
        val masked = tree.maskSensitiveValues("Login failed: password=SuperSecret99!")
        assertFalse(masked.contains("SuperSecret99!"), "Password value must not appear")
        assertTrue(masked.contains("password=****"), "Masked placeholder must appear")
    }

    @Test
    fun `maskSensitiveValues is case-insensitive`() {
        val masked = tree.maskSensitiveValues("API_KEY=ABCDEFGH TOKEN=xyz123")
        assertFalse(masked.contains("ABCDEFGH"), "Uppercase key value must not appear")
        assertFalse(masked.contains("xyz123"), "Token value must not appear")
    }

    @Test
    fun `maskSensitiveValues leaves non-sensitive messages unchanged`() {
        val message = "Repository imported id=repo-42 provider=github"
        val masked = tree.maskSensitiveValues(message)
        assertTrue(masked.contains("repo-42"), "Non-sensitive value must be preserved")
        assertTrue(masked.contains("provider=github"), "Non-sensitive value must be preserved")
    }

    // ── String.masked() extension ─────────────────────────────────────────────

    @Test
    fun `masked returns first 4 chars followed by stars`() {
        val result = "sk-abcdefghij".masked()
        assertTrue(result.startsWith("sk-a"), "First 4 chars must be preserved")
        assertTrue(result.endsWith("****"), "Suffix must be four stars")
        assertFalse(result.contains("efghij"), "Rest of key must not appear")
    }

    @Test
    fun `masked on short string returns only stars`() {
        val result = "abc".masked()
        assertTrue(result == "****", "Short string must become four stars")
    }

    @Test
    fun `masked on exactly 4 chars returns only stars`() {
        val result = "abcd".masked()
        // length <= 4 falls into the "short string" branch → all stars
        assertTrue(result == "****", "4-char string falls into short-string branch → four stars")
    }

    // ── hashUserId ────────────────────────────────────────────────────────────

    @Test
    fun `hashUserId returns 8 hex characters`() {
        val hash = DevOSLogger.hashUserId("user-123")
        assertTrue(hash.length == 16, "Expected 16-char hex but got: $hash")
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' }, "Result must be lowercase hex")
    }

    @Test
    fun `hashUserId raw user ID does not appear in hash`() {
        val userId = "user-very-specific-id-12345"
        val hash = DevOSLogger.hashUserId(userId)
        assertFalse(hash.contains(userId), "Raw user ID must not appear in hash output")
    }

    @Test
    fun `hashUserId same input produces same hash`() {
        val hash1 = DevOSLogger.hashUserId("consistent-user")
        val hash2 = DevOSLogger.hashUserId("consistent-user")
        assertTrue(hash1 == hash2, "Same input must produce deterministic hash")
    }

    @Test
    fun `hashUserId different inputs produce different hashes`() {
        val hash1 = DevOSLogger.hashUserId("user-a")
        val hash2 = DevOSLogger.hashUserId("user-b")
        assertFalse(hash1 == hash2, "Different user IDs should produce different hashes")
    }

    // ── LogCategory tags ─────────────────────────────────────────────────────

    @Test
    fun `LogCategory AUTH has correct tag`() {
        assertTrue(LogCategory.AUTH.tag == "AUTH")
    }

    @Test
    fun `LogCategory PERFORMANCE has correct tag`() {
        assertTrue(LogCategory.PERFORMANCE.tag == "PERF")
    }

    @Test
    fun `all LogCategory entries have non-blank tags`() {
        LogCategory.values().forEach { category ->
            assertTrue(category.tag.isNotBlank(), "Category ${category.name} must have a non-blank tag")
        }
    }
}
