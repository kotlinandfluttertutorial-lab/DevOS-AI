package com.devos.ai.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── Health ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "status") val status: String,
    @Json(name = "version") val version: String,
    @Json(name = "environment") val environment: String,
)

// ── Repositories ───────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RepositoryResponse(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "owner") val owner: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "description") val description: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "stars") val stars: Int = 0,
    @Json(name = "forks") val forks: Int = 0,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "clone_url") val cloneUrl: String,
    @Json(name = "provider") val provider: String,
    @Json(name = "health_score") val healthScore: Float = 0f,
    @Json(name = "last_sync_at") val lastSyncAt: String?,
    @Json(name = "sync_status") val syncStatus: String,
    @Json(name = "indexed") val indexed: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class RepositoryListResponse(
    @Json(name = "items") val items: List<RepositoryResponse>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "page_size") val pageSize: Int,
)

@JsonClass(generateAdapter = true)
data class CreateRepositoryRequest(
    @Json(name = "clone_url") val cloneUrl: String,
    @Json(name = "provider") val provider: String,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "name") val name: String? = null,
    @Json(name = "owner") val owner: String? = null,
)

@JsonClass(generateAdapter = true)
data class UpdateRepositoryRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "default_branch") val defaultBranch: String? = null,
)

@JsonClass(generateAdapter = true)
data class IndexRequest(
    @Json(name = "force_reindex") val forceReindex: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class IndexStatusResponse(
    @Json(name = "repo_id") val repoId: String,
    @Json(name = "status") val status: String,
    @Json(name = "step") val step: String?,
    @Json(name = "progress") val progress: Int = 0,
    @Json(name = "error") val error: String?,
)

// ── File Tree ──────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class FileNode(
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: String,
    @Json(name = "type") val type: String, // "file" | "directory"
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "children") val children: List<FileNode>? = null,
)

@JsonClass(generateAdapter = true)
data class FileContentResponse(
    @Json(name = "path") val path: String,
    @Json(name = "content") val content: String,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "language") val language: String?,
)

// ── AI Chat ────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String,
)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    @Json(name = "messages") val messages: List<ChatMessageDto>,
    @Json(name = "context") val context: ChatContextDto? = null,
    @Json(name = "stream") val stream: Boolean = false,
    @Json(name = "provider") val provider: String? = null,
    @Json(name = "model") val model: String? = null,
)

@JsonClass(generateAdapter = true)
data class ChatContextDto(
    @Json(name = "repo_id") val repoId: String?,
    @Json(name = "file_path") val filePath: String?,
    @Json(name = "scope") val scope: String = "global",
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "id") val id: String,
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "content") val content: String,
    @Json(name = "role") val role: String,
    @Json(name = "sources") val sources: List<SourceDto> = emptyList(),
    @Json(name = "provider") val provider: String? = null,
    @Json(name = "model") val model: String? = null,
)

@JsonClass(generateAdapter = true)
data class SourceDto(
    @Json(name = "file_path") val filePath: String,
    @Json(name = "line_start") val lineStart: Int,
    @Json(name = "line_end") val lineEnd: Int,
    @Json(name = "snippet") val snippet: String,
    @Json(name = "relevance") val relevance: Float,
)

// ── RAG ────────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RAGQueryRequest(
    @Json(name = "query") val query: String,
    @Json(name = "repo_id") val repoId: String?,
    @Json(name = "top_k") val topK: Int = 8,
    @Json(name = "min_relevance") val minRelevance: Float = 0.2f,
)

@JsonClass(generateAdapter = true)
data class RAGResult(
    @Json(name = "query") val query: String,
    @Json(name = "sources") val sources: List<RAGSource>,
    @Json(name = "answer") val answer: String?,
)

@JsonClass(generateAdapter = true)
data class RAGSource(
    @Json(name = "file_path") val filePath: String,
    @Json(name = "line_start") val lineStart: Int,
    @Json(name = "line_end") val lineEnd: Int,
    @Json(name = "content") val content: String,
    @Json(name = "language") val language: String?,
    @Json(name = "relevance") val relevance: Float,
)

// ── Agents ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AgentExecuteRequest(
    @Json(name = "goal") val goal: String,
    @Json(name = "repo_id") val repoId: String? = null,
    @Json(name = "max_steps") val maxSteps: Int = 10,
)

@JsonClass(generateAdapter = true)
data class AgentExecuteResponse(
    @Json(name = "run_id") val runId: String,
    @Json(name = "goal") val goal: String,
    @Json(name = "status") val status: String,
    @Json(name = "steps") val steps: List<AgentStepDto>,
    @Json(name = "final_answer") val finalAnswer: String?,
    @Json(name = "error") val error: String?,
)

@JsonClass(generateAdapter = true)
data class AgentStepDto(
    @Json(name = "step") val step: Int,
    @Json(name = "thought") val thought: String,
    @Json(name = "action") val action: String?,
    @Json(name = "action_input") val actionInput: String?,
    @Json(name = "observation") val observation: String?,
    @Json(name = "status") val status: String,
)

// ── Providers ──────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ProvidersResponse(
    @Json(name = "providers") val providers: List<ProviderInfo>,
)

@JsonClass(generateAdapter = true)
data class ProviderInfo(
    @Json(name = "name") val name: String,
    @Json(name = "has_key") val hasKey: Boolean,
    @Json(name = "default_model") val defaultModel: String?,
    @Json(name = "available") val available: Boolean = true,
)

// ── Git ────────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CommitListResponse(
    @Json(name = "items") val items: List<GitCommit>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
)

@JsonClass(generateAdapter = true)
data class GitCommit(
    @Json(name = "sha") val sha: String,
    @Json(name = "message") val message: String,
    @Json(name = "author") val author: GitAuthor,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "additions") val additions: Int = 0,
    @Json(name = "deletions") val deletions: Int = 0,
)

@JsonClass(generateAdapter = true)
data class GitAuthor(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "avatar_url") val avatarUrl: String?,
)

@JsonClass(generateAdapter = true)
data class BranchListResponse(
    @Json(name = "items") val items: List<GitBranch>,
)

@JsonClass(generateAdapter = true)
data class GitBranch(
    @Json(name = "name") val name: String,
    @Json(name = "is_default") val isDefault: Boolean,
    @Json(name = "is_protected") val isProtected: Boolean = false,
    @Json(name = "last_commit_sha") val lastCommitSha: String?,
)

// ── Issues ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class IssueListResponse(
    @Json(name = "items") val items: List<IssueDto>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
)

@JsonClass(generateAdapter = true)
data class IssueDto(
    @Json(name = "id") val id: Int,
    @Json(name = "number") val number: Int,
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String?,
    @Json(name = "state") val state: String,
    @Json(name = "labels") val labels: List<IssueLabelDto>,
    @Json(name = "assignee") val assignee: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "comments") val comments: Int = 0,
    @Json(name = "ai_priority") val aiPriority: String?,
)

@JsonClass(generateAdapter = true)
data class IssueLabelDto(
    @Json(name = "name") val name: String,
    @Json(name = "color") val color: String?,
)

// ── PRs ────────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class PRListResponse(
    @Json(name = "items") val items: List<PRDto>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
)

@JsonClass(generateAdapter = true)
data class PRDto(
    @Json(name = "id") val id: Int,
    @Json(name = "number") val number: Int,
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String?,
    @Json(name = "state") val state: String,
    @Json(name = "draft") val draft: Boolean = false,
    @Json(name = "head_branch") val headBranch: String,
    @Json(name = "base_branch") val baseBranch: String,
    @Json(name = "author") val author: String,
    @Json(name = "ai_review_score") val aiReviewScore: Int?,
    @Json(name = "ci_status") val ciStatus: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "comments") val comments: Int = 0,
    @Json(name = "review_comments") val reviewComments: Int = 0,
)

@JsonClass(generateAdapter = true)
data class PRResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "number") val number: Int,
    @Json(name = "title") val title: String,
    @Json(name = "html_url") val htmlUrl: String,
)

@JsonClass(generateAdapter = true)
data class CreatePRRequest(
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String = "",
    @Json(name = "head") val head: String,
    @Json(name = "base") val base: String,
    @Json(name = "draft") val draft: Boolean = false,
)
