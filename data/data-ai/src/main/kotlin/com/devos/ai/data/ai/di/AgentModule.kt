package com.devos.ai.data.ai.di

import com.devos.ai.data.ai.agent.AgentToolExecutor
import com.devos.ai.data.ai.agent.tools.ListFilesTool
import com.devos.ai.data.ai.agent.tools.ReadFileTool
import com.devos.ai.data.ai.agent.tools.SearchCodeTool
import com.devos.ai.data.ai.agent.tools.SearchSymbolsTool
import com.devos.ai.data.ai.repository.AgentRepositoryImpl
import com.devos.ai.domain.ai.repository.AgentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

/**
 * Hilt module wiring the agent orchestration engine.
 *
 * ## Tool multibinding
 * Each built-in [AgentToolExecutor] is bound into a `Map<String, AgentToolExecutor>`
 * keyed by [AgentTool.name]. The [ReActEngine] receives this map and dispatches
 * tool calls by name — adding a new tool is as simple as adding a new @IntoMap entry.
 *
 * ## Available built-in tools
 * | Key              | Tool          | Description |
 * |------------------|---------------|-------------|
 * | read_file        | ReadFileTool  | Read file content with line range |
 * | search_symbols   | SearchSymbolsTool | Symbol index search |
 * | search_code      | SearchCodeTool    | RAG semantic code search |
 * | list_files       | ListFilesTool     | List repo files with filters |
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {

    @Binds @Singleton
    abstract fun bindAgentRepository(impl: AgentRepositoryImpl): AgentRepository

    // ── Built-in tool multibinding ────────────────────────────────────────────

    @Binds @IntoMap
    @AgentToolKey("read_file")
    abstract fun bindReadFileTool(tool: ReadFileTool): AgentToolExecutor

    @Binds @IntoMap
    @AgentToolKey("search_symbols")
    abstract fun bindSearchSymbolsTool(tool: SearchSymbolsTool): AgentToolExecutor

    @Binds @IntoMap
    @AgentToolKey("search_code")
    abstract fun bindSearchCodeTool(tool: SearchCodeTool): AgentToolExecutor

    @Binds @IntoMap
    @AgentToolKey("list_files")
    abstract fun bindListFilesTool(tool: ListFilesTool): AgentToolExecutor
}
