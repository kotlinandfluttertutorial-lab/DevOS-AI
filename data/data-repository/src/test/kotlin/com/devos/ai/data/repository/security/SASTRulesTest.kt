package com.devos.ai.data.repository.security

import com.devos.ai.domain.repository.model.Severity
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SASTRules] regex patterns.
 *
 * Each test verifies that a rule fires on a positive sample and does NOT fire
 * on a clean counterexample.
 *
 * DEVOS-047 / DA-58
 */
class SASTRulesTest {

    // ── HARDCODED_SECRET ──────────────────────────────────────────────────────

    @Test
    fun `HARDCODED_SECRET fires on api_key assignment with quoted string`() {
        val line = """val api_key = "sk-abc12345def67890" """
        assertTrue(
            SASTRules.HARDCODED_SECRET.pattern.containsMatchIn(line),
            "Expected HARDCODED_SECRET to match api_key assignment",
        )
    }

    @Test
    fun `HARDCODED_SECRET fires on password assignment case-insensitive`() {
        val line = """val PASSWORD = "s3cr3tP@ss!" """
        assertTrue(
            SASTRules.HARDCODED_SECRET.pattern.containsMatchIn(line),
            "Expected HARDCODED_SECRET to match PASSWORD assignment",
        )
    }

    @Test
    fun `HARDCODED_SECRET fires on token assignment`() {
        val line = """const val token = "ghp_ABCDEFGHIJKLMNOP" """
        assertTrue(
            SASTRules.HARDCODED_SECRET.pattern.containsMatchIn(line),
            "Expected HARDCODED_SECRET to match token assignment",
        )
    }

    @Test
    fun `HARDCODED_SECRET does NOT fire on short strings (less than 8 chars)`() {
        val line = """val token = "abc" """
        assertFalse(
            SASTRules.HARDCODED_SECRET.pattern.containsMatchIn(line),
            "HARDCODED_SECRET must not match strings shorter than 8 characters",
        )
    }

    @Test
    fun `HARDCODED_SECRET does NOT fire on clean variable assignment`() {
        val line = "val userName = user.name"
        assertFalse(
            SASTRules.HARDCODED_SECRET.pattern.containsMatchIn(line),
            "HARDCODED_SECRET must not match clean variable assignment",
        )
    }

    @Test
    fun `HARDCODED_SECRET severity is CRITICAL`() {
        assertTrue(SASTRules.HARDCODED_SECRET.severity == Severity.CRITICAL)
    }

    // ── SQL_INJECTION ─────────────────────────────────────────────────────────

    @Test
    fun `SQL_INJECTION fires on rawQuery with dollar sign interpolation`() {
        val line = """db.rawQuery("SELECT * FROM users WHERE id = ${'$'}userId", null)"""
        assertTrue(
            SASTRules.SQL_INJECTION.pattern.containsMatchIn(line),
            "Expected SQL_INJECTION to match rawQuery with string interpolation",
        )
    }

    @Test
    fun `SQL_INJECTION does NOT fire on rawQuery with selection args placeholder`() {
        val line = """db.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(userId))"""
        assertFalse(
            SASTRules.SQL_INJECTION.pattern.containsMatchIn(line),
            "SQL_INJECTION must not fire on parameterised rawQuery",
        )
    }

    @Test
    fun `SQL_INJECTION does NOT fire on regular query call`() {
        val line = """dao.getUserById(userId)"""
        assertFalse(
            SASTRules.SQL_INJECTION.pattern.containsMatchIn(line),
            "SQL_INJECTION must not fire on DAO method calls",
        )
    }

    @Test
    fun `SQL_INJECTION severity is HIGH`() {
        assertTrue(SASTRules.SQL_INJECTION.severity == Severity.HIGH)
    }

    // ── CLEARTEXT_HTTP ────────────────────────────────────────────────────────

    @Test
    fun `CLEARTEXT_HTTP fires on non-localhost http URL`() {
        val line = """val baseUrl = "http://api.example.com/v1/" """
        assertTrue(
            SASTRules.CLEARTEXT_HTTP.pattern.containsMatchIn(line),
            "Expected CLEARTEXT_HTTP to match non-localhost http URL",
        )
    }

    @Test
    fun `CLEARTEXT_HTTP does NOT fire on https URL`() {
        val line = """val baseUrl = "https://api.example.com/v1/" """
        assertFalse(
            SASTRules.CLEARTEXT_HTTP.pattern.containsMatchIn(line),
            "CLEARTEXT_HTTP must not match HTTPS URLs",
        )
    }

    @Test
    fun `CLEARTEXT_HTTP does NOT fire on localhost http URL`() {
        val line = """val devUrl = "http://localhost:8080/api" """
        assertFalse(
            SASTRules.CLEARTEXT_HTTP.pattern.containsMatchIn(line),
            "CLEARTEXT_HTTP must not match localhost HTTP (development use is acceptable)",
        )
    }

    @Test
    fun `CLEARTEXT_HTTP severity is HIGH`() {
        assertTrue(SASTRules.CLEARTEXT_HTTP.severity == Severity.HIGH)
    }

    // ── ALL list ──────────────────────────────────────────────────────────────

    @Test
    fun `ALL contains exactly 3 rules`() {
        assertTrue(SASTRules.ALL.size == 3)
    }

    @Test
    fun `ALL list contains HARDCODED_SECRET as first rule (highest severity first)`() {
        assertTrue(SASTRules.ALL.first().ruleId == "SAST-001")
    }
}
