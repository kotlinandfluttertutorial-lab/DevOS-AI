package com.devos.ai.domain.learning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [LearningRecommendationEngine].
 *
 * Each test verifies one or more symbol-to-topic mappings, as well as
 * edge cases (empty input, multiple topics from same symbol set).
 *
 * DEVOS-054 / DA-66
 */
class LearningRecommendationEngineTest {

    private val engine = LearningRecommendationEngine()

    // ── Dependency injection ──────────────────────────────────────────────────

    @Test
    fun `@HiltViewModel symbol returns dependency_injection topic`() {
        val topics = engine.analyzeSymbols(listOf("@HiltViewModel"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_DI))
    }

    @Test
    fun `@Inject symbol returns dependency_injection topic`() {
        val topics = engine.analyzeSymbols(listOf("@Inject"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_DI))
    }

    @Test
    fun `@Module symbol returns dependency_injection topic`() {
        val topics = engine.analyzeSymbols(listOf("@Module"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_DI))
    }

    // ── Kotlin coroutines ─────────────────────────────────────────────────────

    @Test
    fun `StateFlow symbol returns kotlin_coroutines topic`() {
        val topics = engine.analyzeSymbols(listOf("StateFlow"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_COROUTINES))
    }

    @Test
    fun `Flow symbol returns kotlin_coroutines topic`() {
        val topics = engine.analyzeSymbols(listOf("Flow"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_COROUTINES))
    }

    @Test
    fun `suspend symbol returns kotlin_coroutines topic`() {
        val topics = engine.analyzeSymbols(listOf("suspend"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_COROUTINES))
    }

    // ── Android networking ────────────────────────────────────────────────────

    @Test
    fun `Retrofit symbol returns android_networking topic`() {
        val topics = engine.analyzeSymbols(listOf("Retrofit"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_NETWORKING))
    }

    @Test
    fun `@GET symbol returns android_networking topic`() {
        val topics = engine.analyzeSymbols(listOf("@GET"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_NETWORKING))
    }

    @Test
    fun `@POST symbol returns android_networking topic`() {
        val topics = engine.analyzeSymbols(listOf("@POST"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_NETWORKING))
    }

    // ── Room database ─────────────────────────────────────────────────────────

    @Test
    fun `@Entity symbol returns room_database topic`() {
        val topics = engine.analyzeSymbols(listOf("@Entity"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_ROOM))
    }

    @Test
    fun `Room symbol returns room_database topic`() {
        val topics = engine.analyzeSymbols(listOf("Room"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_ROOM))
    }

    @Test
    fun `@Dao symbol returns room_database topic`() {
        val topics = engine.analyzeSymbols(listOf("@Dao"))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_ROOM))
    }

    // ── Multiple topics ───────────────────────────────────────────────────────

    @Test
    fun `mixed symbols return all matching topics`() {
        val symbols = listOf("@HiltViewModel", "StateFlow", "Retrofit", "@Entity")
        val topics = engine.analyzeSymbols(symbols)

        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_DI))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_COROUTINES))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_NETWORKING))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_ROOM))
        assertEquals(4, topics.size)
    }

    @Test
    fun `symbols with DI and networking only returns exactly two topics`() {
        val symbols = listOf("@Inject", "@GET")
        val topics = engine.analyzeSymbols(symbols)

        assertEquals(2, topics.size)
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_DI))
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_NETWORKING))
        assertFalse(topics.contains(LearningRecommendationEngine.TOPIC_COROUTINES))
        assertFalse(topics.contains(LearningRecommendationEngine.TOPIC_ROOM))
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `empty symbol list returns empty topic list`() {
        val topics = engine.analyzeSymbols(emptyList())
        assertTrue(topics.isEmpty())
    }

    @Test
    fun `unrecognised symbols return empty topic list`() {
        val topics = engine.analyzeSymbols(listOf("UnknownClass", "someFunction", "CONSTANT"))
        assertTrue(topics.isEmpty())
    }

    @Test
    fun `duplicate trigger symbols do not produce duplicate topics`() {
        val symbols = listOf("@Inject", "@Inject", "@HiltViewModel", "@Module")
        val topics = engine.analyzeSymbols(symbols)

        assertEquals(1, topics.size)
        assertTrue(topics.contains(LearningRecommendationEngine.TOPIC_DI))
    }
}
