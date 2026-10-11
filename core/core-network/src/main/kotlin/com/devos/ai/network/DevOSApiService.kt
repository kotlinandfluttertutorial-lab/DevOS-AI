package com.devos.ai.network

import com.devos.ai.network.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the DevOS AI backend API.
 *
 * Base URL is injected at build time via product flavor [BuildConfig.BASE_URL]:
 *  - local      → http://10.0.2.2:8000/v1/
 *  - staging    → https://api-staging.devos.ai/v1/
 *  - production → https://api.devos.ai/v1/
 *
 * VCS tokens (GitHub/GitLab PATs) are passed per-call via [Header] annotations
 * to avoid storing them in a shared interceptor or Room DB.
 *
 * Security: never log raw token values — only boolean presence or `take(4)+"****"`.
 */
interface DevOSApiService {

    // ── Health ─────────────────────────────────────────────────────────────────

    @GET("health")
    suspend fun getHealth(): HealthResponse

    // ── Repositories ───────────────────────────────────────────────────────────

    @GET("repositories")
    suspend fun listRepositories(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("source") source: String? = null,
    ): RepositoryListResponse

    @POST("repositories")
    suspend fun createRepository(@Body request: CreateRepositoryRequest): RepositoryResponse

    @GET("repositories/{id}")
    suspend fun getRepository(@Path("id") id: String): RepositoryResponse

    @PUT("repositories/{id}")
    suspend fun updateRepository(
        @Path("id") id: String,
        @Body request: UpdateRepositoryRequest,
    ): RepositoryResponse

    @DELETE("repositories/{id}")
    suspend fun deleteRepository(@Path("id") id: String): Response<Unit>

    @POST("repositories/{id}/index")
    suspend fun triggerIndex(
        @Path("id") id: String,
        @Body request: IndexRequest = IndexRequest(),
    ): Response<Unit>

    @GET("repositories/{id}/index/status")
    suspend fun getIndexStatus(@Path("id") id: String): IndexStatusResponse

    @GET("repositories/{id}/tree")
    suspend fun getFileTree(@Path("id") id: String): FileNode

    @GET("repositories/{id}/file")
    suspend fun getFileContent(
        @Path("id") id: String,
        @Query("path") path: String,
    ): FileContentResponse

    // ── AI Chat ────────────────────────────────────────────────────────────────

    @POST("chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @GET("providers")
    suspend fun getProviders(): ProvidersResponse

    // ── RAG ────────────────────────────────────────────────────────────────────

    @POST("rag/query")
    suspend fun ragQuery(@Body request: RAGQueryRequest): RAGResult

    // ── Agents ─────────────────────────────────────────────────────────────────

    @POST("agents/execute")
    suspend fun executeAgent(@Body request: AgentExecuteRequest): AgentExecuteResponse

    // ── Git ────────────────────────────────────────────────────────────────────

    /**
     * List commits for a repository.
     *
     * @param vcsToken  Personal access token loaded from EncryptedSharedPreferences.
     *                  Never hardcoded or logged raw.
     * @param vcsProvider  "github" | "gitlab"
     */
    @GET("git/{owner}/{repo}/commits")
    suspend fun listCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("X-VCS-Token") vcsToken: String,
        @Header("X-VCS-Provider") vcsProvider: String = "github",
        @Query("branch") branch: String = "main",
        @Query("page") page: Int = 1,
    ): CommitListResponse

    @GET("git/{owner}/{repo}/branches")
    suspend fun listBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("X-VCS-Token") vcsToken: String,
        @Header("X-VCS-Provider") vcsProvider: String = "github",
    ): BranchListResponse

    // ── Issues ─────────────────────────────────────────────────────────────────

    @GET("issues/{owner}/{repo}")
    suspend fun listIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("X-VCS-Token") vcsToken: String,
        @Header("X-VCS-Provider") vcsProvider: String = "github",
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
    ): IssueListResponse

    // ── PRs ────────────────────────────────────────────────────────────────────

    @GET("prs/{owner}/{repo}")
    suspend fun listPRs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("X-VCS-Token") vcsToken: String,
        @Header("X-VCS-Provider") vcsProvider: String = "github",
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
    ): PRListResponse

    @POST("prs/{owner}/{repo}")
    suspend fun createPR(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("X-VCS-Token") vcsToken: String,
        @Header("X-VCS-Provider") vcsProvider: String = "github",
        @Body request: CreatePRRequest,
    ): PRResponse
}
