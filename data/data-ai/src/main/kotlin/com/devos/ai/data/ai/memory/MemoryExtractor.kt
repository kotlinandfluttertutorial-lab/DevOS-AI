package com.devos.ai.data.ai.memory

import com.devos.ai.domain.ai.model.MemoryCategory
import com.devos.ai.domain.ai.model.MemoryEntry
import com.devos.ai.domain.ai.model.MemorySource
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts memorable information from an AI conversation turn.
 *
 * Rules applied in order:
 * - "prefer …"            → [MemoryCategory.CODE_PREFERENCE]
 * - "always use …"        → [MemoryCategory.CODE_PREFERENCE]
 * - "never use …"         → [MemoryCategory.CODE_PREFERENCE]
 * - "decided …"           → [MemoryCategory.DECISION]
 * - "chose …"             → [MemoryCategory.DECISION]
 *
 * ## Security
 * Patterns that look like API keys / tokens / passwords are stripped from
 * the stored content before any entry is created. This prevents accidental
 * persistence of secrets extracted from copy-pasted code or config snippets.
 *
 * DEVOS-056 / DA-67
 */
@Singleton
class MemoryExtractor @Inject constructor() {

    companion object {
        // Matches common secret patterns: key=<value>, token=<value>, password=<value>
        // Replaces the sensitive value portion with [REDACTED].
        private val SECRET_PATTERN = Regex(
            pattern = """(?i)(api[_-]?key|token|password|secret|bearer)\s*[=:]\s*\S+""",
            options = setOf(RegexOption.IGNORE_CASE),
        )

        // Triggers that signal code preferences
        private val PREFERENCE_PATTERNS = listOf(
            Regex("""(?i)\bprefer\b(.{3,120})"""),
            Regex("""(?i)\balways use\b(.{3,120})"""),
            Regex("""(?i)\bnever use\b(.{3,120})"""),
        )

        // Triggers that signal architectural/code decisions
        private val DECISION_PATTERNS = listOf(
            Regex("""(?i)\bdecided\b(.{3,120})"""),
            Regex("""(?i)\bchose\b(.{3,120})"""),
        )

        /** Maximum number of characters stored in a single memory entry. */
        private const val MAX_CONTENT_LENGTH = 500
    }

    /**
     * Analyses [userMessage] and [aiResponse] for memorable information.
     *
     * @return a list of [MemoryEntry] objects (may be empty) if memorable content was found,
     *         or `null` if no memorable signal was detected in either text.
     *         An empty-list return is not possible: the function either returns `null`
     *         or at least one entry.
     */
    fun extractFromConversation(
        userMessage: String,
        aiResponse: String,
        userId: String = "default",
    ): List<MemoryEntry>? {
        val combinedText = "$userMessage\n$aiResponse"
        val sanitized    = sanitize(combinedText)

        val entries = mutableListOf<MemoryEntry>()

        // Check preference patterns
        for (pattern in PREFERENCE_PATTERNS) {
            val match = pattern.find(sanitized) ?: continue
            val content = sanitize(match.value).take(MAX_CONTENT_LENGTH)
            entries += newEntry(userId, content, MemoryCategory.CODE_PREFERENCE)
            break // one preference entry per conversation turn
        }

        // Check decision patterns (only if no preference already captured same trigger)
        if (entries.none { it.category == MemoryCategory.DECISION }) {
            for (pattern in DECISION_PATTERNS) {
                val match = pattern.find(sanitized) ?: continue
                val content = sanitize(match.value).take(MAX_CONTENT_LENGTH)
                entries += newEntry(userId, content, MemoryCategory.DECISION)
                break
            }
        }

        return if (entries.isEmpty()) null else entries
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Strips secret-looking patterns from [text] before persistence. */
    fun sanitize(text: String): String =
        SECRET_PATTERN.replace(text) { match ->
            // Keep the key name, redact the value
            val fullMatch = match.value
            val eqIndex   = fullMatch.indexOfFirst { it == '=' || it == ':' }
            if (eqIndex >= 0) {
                fullMatch.substring(0, eqIndex + 1) + " [REDACTED]"
            } else {
                "[REDACTED]"
            }
        }

    private fun newEntry(
        userId: String,
        content: String,
        category: MemoryCategory,
    ) = MemoryEntry(
        id        = UUID.randomUUID().toString(),
        userId    = userId,
        content   = content,
        category  = category,
        source    = MemorySource.AI_CHAT,
        createdAt = System.currentTimeMillis(),
        isPinned  = false,
    )
}
