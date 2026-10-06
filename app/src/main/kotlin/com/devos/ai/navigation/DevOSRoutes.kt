package com.devos.ai.navigation

/**
 * All navigation routes for DevOS AI.
 *
 * Full navigation spec: docs/figma/navigation.md
 */
object DevOSRoutes {

    // ── Onboarding ─────────────────────────────────────────────────────────────
    const val SPLASH              = "splash"
    const val ONBOARDING          = "onboarding"
    const val LOGIN               = "login"

    // ── Primary tabs ───────────────────────────────────────────────────────────
    const val HOME                = "home"
    const val PROJECT_LIST        = "project_list"
    const val AI_CHAT             = "ai_chat"
    const val LEARNING_DASHBOARD  = "learning_dashboard"
    const val MORE                = "more"

    // ── Projects ───────────────────────────────────────────────────────────────
    const val PROJECT_OVERVIEW    = "project/{projectId}"
    const val REPOSITORY_IMPORT   = "repository/import"
    const val REPOSITORY_SYNC     = "repository/{repoId}/sync"

    // ── Repository ─────────────────────────────────────────────────────────────
    const val REPOSITORY_OVERVIEW = "repository/{repoId}"
    const val FILE_EXPLORER       = "repository/{repoId}/files?path={path}"
    const val CODE_VIEWER         = "repository/{repoId}/file?path={path}&line={line}"
    const val CODE_SEARCH         = "repository/{repoId}/search?q={query}"
    const val SYMBOL_DETAILS      = "repository/{repoId}/symbol/{symbolId}"
    const val DEPENDENCY_GRAPH    = "repository/{repoId}/graph"
    const val ARCHITECTURE        = "repository/{repoId}/architecture"

    // ── AI ─────────────────────────────────────────────────────────────────────
    const val AI_ANSWER_DETAIL    = "ai/answer/{answerId}"
    const val AI_SOURCE_EVIDENCE  = "ai/evidence/{answerId}"
    const val AGENT_RUN           = "agent/run/{runId}"
    const val AGENT_TOOL_EXEC     = "agent/run/{runId}/tool/{toolId}"

    // ── Developer Intelligence ─────────────────────────────────────────────────
    const val GIT_HISTORY         = "repository/{repoId}/git"
    const val ISSUE_LIST          = "project/{projectId}/issues"
    const val ISSUE_DETAIL        = "project/{projectId}/issues/{issueId}"
    const val PR_LIST             = "project/{projectId}/prs"
    const val PR_REVIEW           = "project/{projectId}/prs/{prId}/review"

    // ── Quality ────────────────────────────────────────────────────────────────
    const val SECURITY_FINDINGS   = "project/{projectId}/security"
    const val TEST_INTELLIGENCE   = "project/{projectId}/tests"

    // ── Learning ───────────────────────────────────────────────────────────────
    const val COURSE_DETAILS      = "learn/course/{courseId}"
    const val LESSON              = "learn/course/{courseId}/lesson/{lessonId}"
    const val QUIZ                = "learn/course/{courseId}/quiz/{quizId}"

    // ── Developer tools ────────────────────────────────────────────────────────
    const val DEVELOPER_MEMORY    = "memory"
    const val MCP_TOOLS           = "mcp"
    const val NOTIFICATIONS       = "notifications"
    const val PROFILE             = "profile"
    const val SEARCH              = "search?q={query}"

    // ── Settings ───────────────────────────────────────────────────────────────
    const val SETTINGS            = "settings"
    const val AI_SETTINGS         = "settings/ai"
    const val PROVIDER_SETTINGS   = "settings/ai/providers"
    const val PROJECT_SETTINGS    = "settings/project/{projectId}"

    // ── Deep link helpers ──────────────────────────────────────────────────────
    fun project(projectId: String)              = "project/$projectId"
    fun repository(repoId: String)              = "repository/$repoId"
    fun codeViewer(repoId: String, path: String, line: Int = 0) =
        "repository/$repoId/file?path=$path&line=$line"
    fun symbol(repoId: String, symbolId: String) = "repository/$repoId/symbol/$symbolId"
    fun agentRun(runId: String)                  = "agent/run/$runId"
    fun aiAnswer(answerId: String)               = "ai/answer/$answerId"
    fun course(courseId: String)                 = "learn/course/$courseId"
    fun lesson(courseId: String, lessonId: String) = "learn/course/$courseId/lesson/$lessonId"
}
