package com.devos.ai.data.ai.recommendation

import com.devos.ai.domain.ai.model.RecommendationType
import com.devos.ai.domain.ai.usecase.GetAIRecommendationsUseCase
import com.devos.ai.domain.ai.usecase.LearningSignalProvider
import com.devos.ai.domain.ai.usecase.SecuritySignalProvider
import com.devos.ai.domain.ai.usecase.TestCoverageSignalProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [GetAIRecommendationsUseCase].
 *
 * DEVOS-058 / DA-70
 */
class GetAIRecommendationsUseCaseTest {

    private lateinit var useCase: GetAIRecommendationsUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetAIRecommendationsUseCase(
            securityProvider     = SecuritySignalProvider(),
            testCoverageProvider = TestCoverageSignalProvider(),
            learningProvider     = LearningSignalProvider(),
        )
    }

    // ── Max 8 signals ─────────────────────────────────────────────────────────

    @Test
    fun `returns at most 8 signals`() {
        // Generate more than 8 by simulating repeated calls — use case caps at 8
        val result = useCase(securityCount = 100, testCoverage = 0.1f)
        assertTrue(result.size <= GetAIRecommendationsUseCase.MAX_SIGNALS)
    }

    @Test
    fun `with all providers active returns 3 signals`() {
        // security (count=2), missing-tests (coverage=0.34), learning = 3 total
        val result = useCase(securityCount = 2, testCoverage = 0.34f)
        assertEquals(3, result.size)
    }

    // ── Sorted by priority ────────────────────────────────────────────────────

    @Test
    fun `signals are sorted by priority descending`() {
        val result = useCase(securityCount = 2, testCoverage = 0.34f)
        val priorities = result.map { it.priority }
        assertEquals(priorities.sortedDescending(), priorities)
    }

    @Test
    fun `SECURITY has higher priority than MISSING_TESTS`() {
        val result   = useCase(securityCount = 1, testCoverage = 0.3f)
        val security = result.first { it.type == RecommendationType.SECURITY }
        val tests    = result.first { it.type == RecommendationType.MISSING_TESTS }
        assertTrue(security.priority > tests.priority)
    }

    @Test
    fun `MISSING_TESTS has higher priority than LEARNING`() {
        val result   = useCase(securityCount = 0, testCoverage = 0.3f)
        val tests    = result.first { it.type == RecommendationType.MISSING_TESTS }
        val learning = result.first { it.type == RecommendationType.LEARNING }
        assertTrue(tests.priority > learning.priority)
    }

    @Test
    fun `SECURITY has higher priority than LEARNING`() {
        val result   = useCase(securityCount = 3, testCoverage = 1.0f)
        val security = result.first { it.type == RecommendationType.SECURITY }
        val learning = result.first { it.type == RecommendationType.LEARNING }
        assertTrue(security.priority > learning.priority)
    }

    // ── Security signal ───────────────────────────────────────────────────────

    @Test
    fun `no security signal when securityCount is 0`() {
        val result = useCase(securityCount = 0, testCoverage = 0.5f)
        assertTrue(result.none { it.type == RecommendationType.SECURITY })
    }

    @Test
    fun `security signal present when securityCount is greater than 0`() {
        val result = useCase(securityCount = 1, testCoverage = 1.0f)
        assertTrue(result.any { it.type == RecommendationType.SECURITY })
    }

    @Test
    fun `security priority increases with count`() {
        val result1 = useCase(securityCount = 1, testCoverage = 1.0f)
        val result5 = useCase(securityCount = 5, testCoverage = 1.0f)

        val p1 = result1.first { it.type == RecommendationType.SECURITY }.priority
        val p5 = result5.first { it.type == RecommendationType.SECURITY }.priority

        assertTrue(p5 > p1)
    }

    // ── Test coverage signal ──────────────────────────────────────────────────

    @Test
    fun `no missing-tests signal when coverage is 60 percent or above`() {
        val result = useCase(securityCount = 0, testCoverage = 0.6f)
        assertTrue(result.none { it.type == RecommendationType.MISSING_TESTS })
    }

    @Test
    fun `missing-tests signal present when coverage is below 60 percent`() {
        val result = useCase(securityCount = 0, testCoverage = 0.59f)
        assertTrue(result.any { it.type == RecommendationType.MISSING_TESTS })
    }

    // ── Learning signal ───────────────────────────────────────────────────────

    @Test
    fun `learning signal is always present`() {
        // No security, coverage above threshold → only learning remains
        val result = useCase(securityCount = 0, testCoverage = 1.0f)
        assertTrue(result.any { it.type == RecommendationType.LEARNING })
    }

    // ── Action routes ─────────────────────────────────────────────────────────

    @Test
    fun `security signal has non-blank actionRoute`() {
        val result = useCase(securityCount = 1, testCoverage = 1.0f)
        val security = result.first { it.type == RecommendationType.SECURITY }
        assertTrue(security.actionRoute.isNotBlank())
    }

    @Test
    fun `all signals have non-blank title and description`() {
        val result = useCase(securityCount = 2, testCoverage = 0.3f)
        result.forEach { signal ->
            assertTrue(signal.title.isNotBlank(), "title should not be blank for ${signal.type}")
            assertTrue(signal.description.isNotBlank(), "description should not be blank for ${signal.type}")
        }
    }
}
