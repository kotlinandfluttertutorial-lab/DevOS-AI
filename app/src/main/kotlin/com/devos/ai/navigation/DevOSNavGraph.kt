package com.devos.ai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devos.ai.feature.auth.navigation.loginNavigation
import com.devos.ai.feature.auth.navigation.onboardingNavigation
import com.devos.ai.feature.auth.navigation.splashNavigation
import com.devos.ai.feature.chat.aiChatNavigation
import com.devos.ai.feature.chat.answerDetailNavigation
import com.devos.ai.feature.chat.sourceEvidenceNavigation
import com.devos.ai.feature.settings.aiSettingsNavigation
import com.devos.ai.feature.home.navigation.homeNavigation
import com.devos.ai.feature.home.navigation.notificationsNavigation
import com.devos.ai.feature.home.navigation.profileNavigation
import com.devos.ai.feature.home.navigation.searchNavigation
import com.devos.ai.feature.learning.navigation.courseDetailsNavigation
import com.devos.ai.feature.learning.navigation.learningDashboardNavigation
import com.devos.ai.feature.learning.navigation.lessonNavigation
import com.devos.ai.feature.learning.navigation.quizNavigation
import com.devos.ai.feature.repository.navigation.overviewNavigation
import com.devos.ai.feature.repository.navigation.projectListNavigation
import com.devos.ai.feature.repository.navigation.repositoryImportNavigation
import com.devos.ai.feature.repository.navigation.repositorySyncNavigation

/**
 * Root navigation graph for DevOS AI.
 *
 * All routes are defined in [DevOSRoutes].
 * Placeholder composables are used until each feature module provides its real screen.
 * Feature navigation extensions (e.g. splashNavigation, authNavigation) will replace
 * the placeholder entries in their respective FEAT tickets.
 *
 * Navigation hierarchy:
 *   splash → onboarding (first launch) | home (returning user)
 *   home → project | repository | ai_chat | learning | more
 *   more → search | git | agents | mcp | memory | notifications | settings | profile
 */
@Composable
fun DevOSNavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = DevOSRoutes.SPLASH,
        modifier = modifier,
    ) {

        // ── Onboarding ──────────────────────────────────────────────────────────
        splashNavigation(navController)

        onboardingNavigation(navController)

        // TODO(FEAT-004): Replace with authNavigation(navController)
        loginNavigation(navController)

        // ── Primary tabs ────────────────────────────────────────────────────────
        homeNavigation(navController)

        projectListNavigation(navController)

        aiChatNavigation(navController)

        learningDashboardNavigation(navController)

        composable(route = DevOSRoutes.MORE) {
            PlaceholderScreen(route = DevOSRoutes.MORE)
        }

        // ── Projects ────────────────────────────────────────────────────────────
        composable(
            route = DevOSRoutes.PROJECT_OVERVIEW,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "project_overview")
        }

        repositoryImportNavigation(navController)

        repositorySyncNavigation(navController)

        // ── Repository ──────────────────────────────────────────────────────────
        overviewNavigation(navController)

        composable(
            route = DevOSRoutes.FILE_EXPLORER,
            arguments = listOf(
                navArgument("repoId") { type = NavType.StringType },
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            PlaceholderScreen(route = "file_explorer")
        }

        composable(
            route = DevOSRoutes.CODE_VIEWER,
            arguments = listOf(
                navArgument("repoId") { type = NavType.StringType },
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("line") { type = NavType.StringType; defaultValue = "0" },
            ),
        ) {
            PlaceholderScreen(route = "code_viewer")
        }

        composable(
            route = DevOSRoutes.CODE_SEARCH,
            arguments = listOf(
                navArgument("repoId") { type = NavType.StringType },
                navArgument("query") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            PlaceholderScreen(route = "code_search")
        }

        composable(
            route = DevOSRoutes.SYMBOL_DETAILS,
            arguments = listOf(
                navArgument("repoId") { type = NavType.StringType },
                navArgument("symbolId") { type = NavType.StringType },
            ),
        ) {
            PlaceholderScreen(route = "symbol_details")
        }

        composable(
            route = DevOSRoutes.DEPENDENCY_GRAPH,
            arguments = listOf(navArgument("repoId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "dependency_graph")
        }

        composable(
            route = DevOSRoutes.ARCHITECTURE,
            arguments = listOf(navArgument("repoId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "architecture")
        }

        // ── AI ──────────────────────────────────────────────────────────────────
        answerDetailNavigation(navController)

        sourceEvidenceNavigation(navController)

        composable(
            route = DevOSRoutes.AGENT_RUN,
            arguments = listOf(navArgument("runId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "agent_run")
        }

        composable(
            route = DevOSRoutes.AGENT_TOOL_EXEC,
            arguments = listOf(
                navArgument("runId") { type = NavType.StringType },
                navArgument("toolId") { type = NavType.StringType },
            ),
        ) {
            PlaceholderScreen(route = "agent_tool_exec")
        }

        // ── Developer Intelligence ───────────────────────────────────────────────
        composable(
            route = DevOSRoutes.GIT_HISTORY,
            arguments = listOf(navArgument("repoId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "git_history")
        }

        composable(
            route = DevOSRoutes.ISSUE_LIST,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "issue_list")
        }

        composable(
            route = DevOSRoutes.ISSUE_DETAIL,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("issueId") { type = NavType.StringType },
            ),
        ) {
            PlaceholderScreen(route = "issue_detail")
        }

        composable(
            route = DevOSRoutes.PR_LIST,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "pr_list")
        }

        composable(
            route = DevOSRoutes.PR_REVIEW,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("prId") { type = NavType.StringType },
            ),
        ) {
            PlaceholderScreen(route = "pr_review")
        }

        // ── Quality ──────────────────────────────────────────────────────────────
        composable(
            route = DevOSRoutes.SECURITY_FINDINGS,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "security_findings")
        }

        composable(
            route = DevOSRoutes.TEST_INTELLIGENCE,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "test_intelligence")
        }

        // ── Learning ─────────────────────────────────────────────────────────────
        courseDetailsNavigation(navController)

        lessonNavigation(navController)

        quizNavigation(navController)

        // ── Developer tools ───────────────────────────────────────────────────────
        composable(route = DevOSRoutes.DEVELOPER_MEMORY) {
            PlaceholderScreen(route = DevOSRoutes.DEVELOPER_MEMORY)
        }

        composable(route = DevOSRoutes.MCP_TOOLS) {
            PlaceholderScreen(route = DevOSRoutes.MCP_TOOLS)
        }

        notificationsNavigation(navController)

        profileNavigation(navController)

        searchNavigation(navController)

        // ── Settings ─────────────────────────────────────────────────────────────
        composable(route = DevOSRoutes.SETTINGS) {
            PlaceholderScreen(route = DevOSRoutes.SETTINGS)
        }

        aiSettingsNavigation(navController)

        composable(route = DevOSRoutes.PROVIDER_SETTINGS) {
            PlaceholderScreen(route = DevOSRoutes.PROVIDER_SETTINGS)
        }

        composable(
            route = DevOSRoutes.PROJECT_SETTINGS,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "project_settings")
        }
    }
}
