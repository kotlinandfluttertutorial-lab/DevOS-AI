# Kiro Prompt — DEVOS-038: MCP Integration (Protocol Client + Tools Screen)

**Jira:** DEVOS-038 / DEVOS-039  
**Epic:** DEVOS-E05  
**Figma:** FIGMA-33  
**Kiro Spec:** `.kiro/specs/agents-mcp/mcp.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the Model Context Protocol (MCP) integration for DevOS AI: the MCP JSON-RPC client in `core-network`, the MCP repository and domain layer, and the MCP Tools screen where users browse and invoke tools from connected MCP servers.

**Existing files to read first:**
- `.kiro/specs/agents-mcp/mcp.md`
- `docs/figma/screen-inventory.md` — Screen FIGMA-33
- `docs/figma/component-inventory.md` — DevOSCard, DevOSButton, DevOSErrorState
- `feature/feature-agents/` — existing Agent Run screen (DEVOS-037) for patterns to follow
- `domain/domain-ai/` — existing domain layer

**Modules:**
- `core/core-network/` — MCPClient (HTTP + SSE)
- `domain/domain-ai/` — MCPRepository interface + domain models
- `data/data-ai/` — MCPRepositoryImpl
- `feature/feature-agents/` — MCPToolsScreen + ViewModel

**Package:** `com.devos.ai.feature.agents.mcp`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- **Destructive MCP tool invocations MUST require explicit AlertDialog confirmation before execution — this is a Non-Negotiable Rule.** Destructive tools: any whose name contains "delete", "remove", "drop", "destroy", "write", "create", "update", "push", "commit".
- MCP server URLs stored in Room — never hardcoded.
- Tool execution results stream via Flow where the server supports SSE responses.
- Tool parameters are user-supplied data — validate types before sending; never inject parameters directly into system prompts.

**What to implement:**

### 1. MCP Domain Models

File: `domain/domain-ai/src/main/kotlin/com/devos/ai/domain/ai/mcp/`

```kotlin
data class MCPServer(
    val id: String,
    val name: String,
    val url: String,
    val isConnected: Boolean,
    val toolCount: Int,
    val lastConnectedAt: Instant?,
)

data class MCPTool(
    val name: String,
    val description: String,
    val serverId: String,
    val inputSchema: JsonObject,    // JSON Schema for parameters
    val isDestructive: Boolean,     // derived from name pattern
)

data class MCPToolResult(
    val toolName: String,
    val output: String,             // formatted result (JSON or text)
    val isError: Boolean,
    val durationMs: Long,
)

interface MCPRepository {
    fun observeServers(): Flow<List<MCPServer>>
    suspend fun discoverTools(serverId: String): Result<List<MCPTool>>
    suspend fun executeTool(
        serverId: String,
        toolName: String,
        params: Map<String, Any>,
    ): Result<MCPToolResult>
    suspend fun addServer(name: String, url: String): Result<MCPServer>
    suspend fun removeServer(serverId: String): Result<Unit>
    suspend fun testConnection(serverId: String): Result<Unit>
}
```

### 2. MCPClient (core-network)

File: `core/core-network/src/main/kotlin/com/devos/ai/network/mcp/MCPClient.kt`

MCP uses JSON-RPC 2.0 over HTTP. SSE for streaming responses.

```kotlin
class MCPClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    /** List available tools from a server. */
    suspend fun listTools(serverUrl: String): Result<List<MCPToolDto>> {
        val request = JsonRpcRequest(
            method = "tools/list",
            params = emptyMap<String, Any>(),
        )
        return executeRequest(serverUrl, request)
    }

    /** Invoke a tool. Returns streaming output if server supports SSE, otherwise single response. */
    fun callTool(
        serverUrl: String,
        toolName: String,
        arguments: Map<String, Any>,
    ): Flow<MCPStreamChunk> = flow {
        val request = JsonRpcRequest(
            method = "tools/call",
            params = mapOf("name" to toolName, "arguments" to arguments),
        )
        // Attempt SSE first; fall back to single HTTP response
        executeStreamingRequest(serverUrl, request).collect { emit(it) }
    }

    private suspend fun executeRequest(url: String, body: JsonRpcRequest): Result<*> { ... }
    private fun executeStreamingRequest(url: String, body: JsonRpcRequest): Flow<MCPStreamChunk> { ... }
}

data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String = UUID.randomUUID().toString(),
    val method: String,
    val params: Map<String, Any>,
)
```

### 3. MCPToolsScreen (DEVOS-039, FIGMA-33)

Route: `MCP_TOOLS`

UiState:
```kotlin
sealed interface MCPToolsUiState {
    data object Loading : MCPToolsUiState
    data class Success(
        val servers: List<MCPServer>,
        val selectedServer: MCPServer?,
        val tools: List<MCPTool>,
        val isLoadingTools: Boolean,
        val executionResult: MCPToolResult?,
        val pendingConfirmation: MCPToolExecutionRequest?,
    ) : MCPToolsUiState
    data object Empty : MCPToolsUiState
    data class Error(val message: String, val retryable: Boolean) : MCPToolsUiState
}

data class MCPToolExecutionRequest(
    val tool: MCPTool,
    val params: Map<String, Any>,
)
```

Layout:
```kotlin
Scaffold(
    topBar = { DevOSTopBar(title = "MCP Tools") },
    floatingActionButton = {
        FloatingActionButton(onClick = onAddServer) {
            Icon(Icons.Outlined.Add, contentDescription = "Add MCP server")
        }
    }
) { padding ->
    when (val s = uiState) {
        is Loading -> DevOSLoadingState()
        is Empty   -> DevOSEmptyState(
            icon = Icons.Outlined.Extension,
            title = "No MCP servers",
            description = "Add an MCP server to browse and invoke tools",
            action = { DevOSButton("Add Server", onClick = onAddServer) }
        )
        is Error   -> DevOSErrorState(description = s.message,
            onRetry = if (s.retryable) viewModel::retry else null)
        is Success -> MCPToolsContent(
            state = s,
            onSelectServer = viewModel::selectServer,
            onExecuteTool = viewModel::requestToolExecution,
        )
    }

    // Destructive operation confirmation dialog
    if (uiState is Success && (uiState as Success).pendingConfirmation != null) {
        val req = (uiState as Success).pendingConfirmation!!
        AlertDialog(
            onDismissRequest = viewModel::cancelExecution,
            title = { Text("Confirm Tool Execution") },
            text = {
                Text("\"${req.tool.name}\" is a destructive operation. " +
                    "This action cannot be undone. Proceed?")
            },
            confirmButton = {
                DevOSButton("Execute", onClick = { viewModel.confirmExecution(req) })
            },
            dismissButton = {
                DevOSButton("Cancel", onClick = viewModel::cancelExecution,
                    style = DevOSButtonStyle.Secondary)
            },
        )
    }
}

@Composable
fun MCPToolsContent(state: Success, onSelectServer: (MCPServer) -> Unit,
                    onExecuteTool: (MCPTool, Map<String, Any>) -> Unit) {
    Row(Modifier.fillMaxSize()) {
        // Server list (left panel)
        LazyColumn(Modifier.width(160.dp)) {
            items(state.servers) { server ->
                ServerListItem(
                    server = server,
                    isSelected = server == state.selectedServer,
                    onClick = { onSelectServer(server) },
                )
            }
        }
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
        // Tools list (right panel)
        if (state.isLoadingTools) {
            DevOSLoadingState(Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(state.tools) { tool ->
                    ToolCard(tool = tool, onInvoke = { params -> onExecuteTool(tool, params) })
                }
            }
        }
    }
}

@Composable
fun ToolCard(tool: MCPTool, onInvoke: (Map<String, Any>) -> Unit) {
    DevOSCard {
        Column(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tool.name, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                if (tool.isDestructive) {
                    DevOSStatusBadge("Destructive", MaterialTheme.colorScheme.error)
                }
            }
            Text(tool.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            // Dynamic parameter form built from inputSchema
            ToolParameterForm(schema = tool.inputSchema, onSubmit = onInvoke)
        }
    }
}
```

ViewModel logic:
```kotlin
fun requestToolExecution(tool: MCPTool, params: Map<String, Any>) {
    if (tool.isDestructive) {
        // Store pending confirmation — show dialog
        _uiState.update { it.copy(pendingConfirmation = MCPToolExecutionRequest(tool, params)) }
    } else {
        executeTool(tool, params)
    }
}

fun confirmExecution(request: MCPToolExecutionRequest) {
    _uiState.update { it.copy(pendingConfirmation = null) }
    executeTool(request.tool, request.params)
}

fun cancelExecution() {
    _uiState.update { it.copy(pendingConfirmation = null) }
}
```

**Tests:**
- `MCPClientTest`: `listTools` parses JSON-RPC response; `callTool` streams SSE chunks; error response parsed as failure
- `MCPRepositoryImplTest`: tools cached in Room; `executeTool` maps result correctly; `testConnection` returns success/failure
- `MCPToolsViewModelTest`: server selection loads tool list; destructive tool sets `pendingConfirmation`; confirm executes; cancel clears confirmation; non-destructive tool executes without dialog
- `MCPToolsScreenTest`: server list renders; tool cards render; destructive badge shown; confirmation dialog appears for destructive tools; dismiss cancels

**Acceptance Criteria:**
- AC1 (DEVOS-038): MCP JSON-RPC client implemented and connects to configured servers
- AC2 (DEVOS-038): Tool discovery via `tools/list` returns tool list for connected server
- AC3 (DEVOS-038): Tool invocation via `tools/call` works with correct parameters
- AC4 (DEVOS-038): Destructive tool invocations require user confirmation via `AlertDialog` before execution
- AC5 (DEVOS-038): MCP server URLs stored in Room — not hardcoded
- AC6 (DEVOS-039): Server list renders with name, URL, connected/disconnected status, and tool count
- AC7 (DEVOS-039): Tools per server listed with name, description, and parameter count
- AC8 (DEVOS-039): Tool detail shows dynamic parameter form built from JSON schema
- AC9 (DEVOS-039): Execution result rendered — success shows formatted output; error shows `DevOSErrorState`
- AC10 (DEVOS-039): Destructive tools show "Destructive" badge and require confirmation dialog
- AC11 (DEVOS-039): Tool execution history accessible from the screen
