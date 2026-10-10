package com.devos.ai.domain.learning

/**
 * Pure-Kotlin engine that maps a list of symbol names / annotations found in a
 * codebase to a set of course topic identifiers.
 *
 * Rules (applied in order, not exclusive — a symbol set can trigger multiple topics):
 *
 * | Trigger symbols                                      | Topic ID                |
 * |------------------------------------------------------|-------------------------|
 * | `@Inject`, `@HiltViewModel`, `@Module`              | `dependency_injection`  |
 * | `StateFlow`, `Flow`, `suspend`                       | `kotlin_coroutines`     |
 * | `Retrofit`, `@GET`, `@POST`                          | `android_networking`    |
 * | `@Entity`, `Room`, `@Dao`                            | `room_database`         |
 *
 * DEVOS-054 / DA-66
 */
class LearningRecommendationEngine {

    /**
     * Analyses [symbols] and returns a deduplicated list of course topic IDs
     * that are relevant to the detected patterns.
     *
     * @param symbols A list of symbol names, annotation names, or code tokens
     *                extracted from the project's source files.
     * @return Ordered list of topic IDs matching the detected patterns.
     *         Empty if no patterns match.
     */
    fun analyzeSymbols(symbols: List<String>): List<String> {
        val topics = mutableListOf<String>()

        if (symbols.any { it in DI_TRIGGERS }) {
            topics += TOPIC_DI
        }
        if (symbols.any { it in COROUTINE_TRIGGERS }) {
            topics += TOPIC_COROUTINES
        }
        if (symbols.any { it in NETWORKING_TRIGGERS }) {
            topics += TOPIC_NETWORKING
        }
        if (symbols.any { it in ROOM_TRIGGERS }) {
            topics += TOPIC_ROOM
        }

        return topics
    }

    // ── Constants ──────────────────────────────────────────────────────────────

    companion object {
        const val TOPIC_DI          = "dependency_injection"
        const val TOPIC_COROUTINES  = "kotlin_coroutines"
        const val TOPIC_NETWORKING  = "android_networking"
        const val TOPIC_ROOM        = "room_database"

        private val DI_TRIGGERS = setOf("@Inject", "@HiltViewModel", "@Module")
        private val COROUTINE_TRIGGERS = setOf("StateFlow", "Flow", "suspend")
        private val NETWORKING_TRIGGERS = setOf("Retrofit", "@GET", "@POST")
        private val ROOM_TRIGGERS = setOf("@Entity", "Room", "@Dao")
    }
}
