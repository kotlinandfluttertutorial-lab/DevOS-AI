# Spec: Agents & MCP — Core

**Jira:** DEVOS-035 / DEVOS-036 / DEVOS-037 / DEVOS-038 / DEVOS-039  
**Epic:** DEVOS-E05  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement the AI agent execution engine, transparent agent UI, MCP server integration, and MCP tools browser.

## Agent Architecture

```
RunAgentUseCase
    ↓
AgentEngine (ReAct loop)
    ↓ think(context + tools + history)
    ↓ AIProvider.streamChat()
    ↓ parse tool calls from response
    ↓ execute tool (with confirmation if destructive)
    ↓ append result to context
    ↓ repeat until complete or maxSteps reached
    ↓
Flow<AgentProgress> → AgentRunViewModel → AgentRunScreen
```

## Domain Models

```kotlin
data class AgentRun(
    val id: String,
    val goal: String,
    val status: AgentStatus,
    val steps: List<AgentStep>,
    val finalAnswer: String?,
    val startedAt: Long,
    val completedAt: Long?,
)

data class AgentStep(
    val id: String,
    val runId: String,
    val index: Int,
    val thought: String,
    val toolCall: ToolCall?,
    val toolResult: ToolResult?,
    val status: StepStatus,
    val durationMs: Long,
)

data class ToolCall(
    val toolName: String,
    val toolServer: String,   // "builtin" or MCP server name
    val parameters: Map<String, Any>,
    val isDestructive: Boolean,
)

enum class StepStatus { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }
enum class AgentStatus { RUNNING, COMPLETED, FAILED, CANCELLED }
```

## Built-in Tools

| Tool | Description | Destructive |
|------|-------------|-------------|
| `search_code` | Full-text code search | No |
| `get_file` | Read file content | No |
| `get_symbol` | Get symbol details | No |
| `get_dependencies` | Get dependency graph | No |
| `ask_question` | Ask user a clarifying question | No |
| `write_file` | Write to a file | **Yes** |
| `create_commit` | Create a git commit | **Yes** |
| `create_pr` | Create a pull request | **Yes** |

## MCP Integration

```kotlin
interface MCPClient {
    suspend fun connect(server: MCPServer): Boolean
    suspend fun listTools(server: MCPServer): List<MCPTool>
    suspend fun invokeTool(server: MCPServer, tool: MCPTool, args: Map<String, Any>): MCPResult
    fun disconnect(server: MCPServer)
}

data class MCPServer(
    val id: String,
    val name: String,
    val command: String,
    val args: List<String>,
    val env: Map<String, String>,
    val status: MCPServerStatus,
)

data class MCPTool(
    val name: String,
    val description: String,
    val parameters: List<MCPParameter>,
    val isDestructive: Boolean,
)
```

## Screens

### AgentRunScreen (FIGMA-19)
- Goal description header
- Step list with status icons:
  - `○` Pending (muted)
  - `⟳` Running (animated spin)
  - `✓` Completed (tertiary/green)
  - `✗` Failed (error/red)
- Each step: thought summary + tool name
- Expand step → DevOSToolExecution detail
- Final answer panel (slides up when complete)
- Cancel button (stops loop cleanly)

### AgentToolExecutionScreen (FIGMA-20)
- Tool name + server badge
- Input parameters formatted as key-value
- Output / result (rendered or code block)
- Duration + estimated tokens
- Raw JSON toggle

### MCPToolsScreen (FIGMA-33)
- Connected servers list with status badge
- Tools per server (expandable)
- Tool detail: name, description, parameters
- "Run" button → parameter input dialog → confirmation for destructive → execute
- Execution history

## Destructive Action Confirmation
```kotlin
// Always show ConfirmationDialog before destructive tool execution
if (toolCall.isDestructive) {
    showConfirmationDialog(
        title = "Confirm: ${toolCall.toolName}",
        description = "This action cannot be undone. Parameters:\n${toolCall.parameters}",
        confirmLabel = "Proceed",
        destructive = true,
    )
}
```

## Acceptance Criteria
- [ ] Agent completes a multi-step code analysis task
- [ ] All steps shown in real time with correct status icons
- [ ] Step expansion shows input/output
- [ ] Cancel stops execution within 2 seconds
- [ ] Destructive tools always show confirmation dialog
- [ ] MCP servers connectable from settings
- [ ] MCP tools browsable and invocable
- [ ] Agent run persisted in Room for history
- [ ] max 20 steps enforced by default (configurable)
