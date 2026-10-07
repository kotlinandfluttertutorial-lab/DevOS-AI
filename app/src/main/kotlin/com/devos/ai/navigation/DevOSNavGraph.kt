package com.devos.ai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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
        // TODO(FEAT-003): Replace with splashNavigation(navController)
        composable(route = DevOSRoutes.SPLASH) {
            PlaceholderScreen(route = DevOSRoutes.SPLASH)
        }

        // TODO(FEAT-003): Replace with onboardingNavigation(navController)
        composable(route = DevOSRoutes.ONBOARDING) {
            PlaceholderScreen(route = DevOSRoutes.ONBOARDING)
        }

        // TODO(FEAT-003): Replace with authNavigation(navController)
        composable(route = DevOSRoutes.LOGIN) {
            PlaceholderScreen(route = DevOSRoutes.LOGIN)
        }

        // ── Primary tabs ────────────────────────────────────────────────────────
        composable(route = DevOSRoutes.HOME) {
            PlaceholderScreen(route = DevOSRoutes.HOME)
        }

        composable(route = DevOSRoutes.PROJECT_LIST) {
            PlaceholderScreen(route = DevOSRoutes.PROJECT_LIST)
        }

        composable(route = DevOSRoutes.AI_CHAT) {
            PlaceholderScreen(route = DevOSRoutes.AI_CHAT)
        }

        composable(route = DevOSRoutes.LEARNING_DASHBOARD) {
            PlaceholderScreen(route = DevOSRoutes.LEARNING_DASHBOARD)
        }

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

        composable(route = DevOSRoutes.REPOSITORY_IMPORT) {
            PlaceholderScreen(route = DevOSRoutes.REPOSITORY_IMPORT)
        }

        composable(
            route = DevOSRoutes.REPOSITORY_SYNC,
            arguments = listOf(navArgument("repoId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "repository_sync")
        }

        // ── Repository ──────────────────────────────────────────────────────────
        composable(
            route = DevOSRoutes.REPOSITORY_OVERVIEW,
            arguments = listOf(navArgument("repoId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "repository_overview")
        }

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
        composable(
            route = DevOSRoutes.AI_ANSWER_DETAIL,
            arguments = listOf(navArgument("answerId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "ai_answer_detail")
        }

        composable(
            route = DevOSRoutes.AI_SOURCE_EVIDENCE,
            arguments = listOf(navArgument("answerId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "ai_source_evidence")
        }

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
        composable(
            route = DevOSRoutes.COURSE_DETAILS,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType }),
        ) {
            PlaceholderScreen(route = "course_details")
        }

        composable(
            route = DevOSRoutes.LESSON,
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("lessonId") { type = NavType.StringType },
            ),
        ) {
            PlaceholderScreen(route = "lesson")
        }

        composable(
            route = DevOSRoutes.QUIZ,
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("quizId") { type = NavType.StringType },
            ),
        ) {
            PlaceholderScreen(route = "quiz")
        }

        // ── Developer tools ───────────────────────────────────────────────────────
        composable(route = DevOSRoutes.DEVELOPER_MEMORY) {
            PlaceholderScreen(route = DevOSRoutes.DEVELOPER_MEMORY)
        }

        composable(route = DevOSRoutes.MCP_TOOLS) {
            PlaceholderScreen(route = DevOSRoutes.MCP_TOOLS)
        }

        composable(route = DevOSRoutes.NOTIFICATIONS) {
            PlaceholderScreen(route = DevOSRoutes.NOTIFICATIONS)
        }

        composable(route = DevOSRoutes.PROFILE) {
            PlaceholderScreen(route = DevOSRoutes.PROFILE)
        }

        composable(
            route = DevOSRoutes.SEARCH,
            arguments = listOf(
                navArgument("query") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            PlaceholderScreen(route = "search")
        }

        // ── Settings ─────────────────────────────────────────────────────────────
        composable(route = DevOSRoutes.SETTINGS) {
            PlaceholderScreen(route = DevOSRoutes.SETTINGS)
        }

        composable(route = DevOSRoutes.AI_SETTINGS) {
            PlaceholderScreen(route = DevOSRoutes.AI_SETTINGS)
        }

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
