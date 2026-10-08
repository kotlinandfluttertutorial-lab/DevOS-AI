package com.devos.ai.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.devos.ai.designsystem.theme.DevOSTheme
import com.devos.ai.feature.home.dashboard.HomeScreen
import com.devos.ai.feature.home.dashboard.HomeUiState
import com.devos.ai.feature.home.model.AIRecommendation
import com.devos.ai.feature.home.model.ChatSessionSummary
import com.devos.ai.feature.home.model.HealthStatus
import com.devos.ai.feature.home.model.ProjectHealth
import com.devos.ai.feature.home.model.ProjectSummary
import com.devos.ai.feature.home.model.RecommendationType
import org.junit.Rule
import org.junit.Test

/**
 * Minimal UI test for [HomeScreen].
 *
 * These are instrumented tests that run on a device or emulator.
 * Full coverage (all 4 states, interaction tests) is out of scope for this stub.
 */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_successState_displaysGreetingText() {
        val successState = HomeUiState.Success(
            recentProjects = listOf(
                ProjectSummary("p1", "DevOS AI", "Kotlin", HealthStatus.HEALTHY),
            ),
            recommendations = listOf(
                AIRecommendation(
                    id = "r1",
                    type = RecommendationType.SECURITY,
                    title = "Fix Hardcoded Secrets",
                    description = "API keys detected",
                ),
            ),
            health = ProjectHealth(
                securityCount = 2,
                securityLabel = "Critical",
                testCoverage = 34,
                architectureGrade = "B+",
                dependencyUpdates = 5,
            ),
            recentSessions = listOf(
                ChatSessionSummary(
                    id = "s1",
                    title = "How do I paginate?",
                    projectName = "DevOS AI",
                    relativeTime = "2 hours ago",
                    iconEmoji = "💬",
                ),
            ),
        )

        composeTestRule.setContent {
            DevOSTheme {
                HomeScreen(
                    uiState = successState,
                    onSearchTap = {},
                    onProjectTap = {},
                    onSessionTap = {},
                    onNotificationTap = {},
                    onProfileTap = {},
                    onImportTap = {},
                    onSeeAllProjectsTap = {},
                    onSeeAllSessionsTap = {},
                    onDismissRecommendation = {},
                    onRetry = {},
                )
            }
        }

        // Verify greeting is displayed (text from HomeTopBar)
        composeTestRule
            .onNodeWithText("Good morning, Dev 👋")
            .assertIsDisplayed()
    }
}
