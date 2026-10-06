# Kiro Prompt — DEVOS-037: Agent Orchestration Engine

**Jira:** DEVOS-037 / DEVOS-035 / DEVOS-036  
**Epic:** DEVOS-E05  
**Figma:** FIGMA-19 / FIGMA-20  
**Kiro Spec:** `.kiro/specs/agents-mcp/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the AI agent orchestration engine and the AgentRun/AgentToolExecution screens.

**Existing files to read first:**
- `.kiro/specs/agents-mcp/core.md` — full architecture and domain models
- `docs/figma/screen-inventory.md` — Screens 19 and 20
- `docs/figma/component-inventory.md` — DevOSAgentStep, DevOSToolExecution

**Module:** `data/data-ai/` (engine), `feature/feature-agents/` (screens)

**What to implement:**

### 1. Agent Engine (ReAct Loop)

```kotlin
class AgentEngineImpl @Inject constructor(
    private val aiProvider: AIProvider,
    private val toolRegistry: AgentToolRegistry,
    private val agentRunDao: AgentRunDao,
) : AgentEngine {

    override fun runAgent(goal: String, context: AIContext): Flow<AgentProgress> = flow {
        val runId = uuid()
        val steps = mutableListOf<AgentStep>()
        var iteration = 0
        val maxSteps = 20

        emit(AgentProgress.Started(runId, goal))

        while (iteration < maxSteps) {
            iteration++
            val step = AgentStep(id = uuid(), runId = runId, index = iteration, status = StepStatus.RUNNING)
            emit(AgentProgress.StepStarted(step))

            // Think: ask AI what to do next
            val thought = aiProvider.streamChat(buildThinkPrompt(goal, steps, context)).last()
            val toolCall = parseToolCall(thought.content)

            if (toolCall == null) {
                // AI produced final answer — done
                emit(AgentProgress.Complete(runId, thought.content, steps))
                return@flow
            }

            // Check if destructive — emit confirmation request
            if (toolCall.isDestructive) {
                emit(AgentProgress.AwaitingConfirmation(runId, toolCall))
                // Wait for confirmation signal (via SharedFlow or coroutine channel)
                val confirmed = awaitConfirmation(runId)
                if (!confirmed) {
                    emit(AgentProgress.Cancelled(runId))
                    return@flow
                }
            }

            // Execute tool
            val result = toolRegistry.execute(toolCall)
            val completedStep = step.copy(
                thought = thought.content,
                toolCall = toolCall,
                toolResult = result,
                status = if (result.isSuccess) StepStatus.COMPLETED else StepStatus.FAILED,
            )
            steps.add(completedStep)
            emit(AgentProgress.StepCompleted(completedStep))
        }

        emit(AgentProgress.MaxStepsReached(runId))
    }
}
```

### 2. Built-in Tool Registry

```kotlin
@Singleton
class AgentToolRegistry @Inject constructor(
    private val searchCodeUseCase: SearchCodeUseCase,
    private val getFileUseCase: GetFileContentUseCase,
    private val getSymbolUseCase: GetSymbolUseCase,
) {
    private val tools: Map<String, AgentTool> = mapOf(
        "search_code"       to SearchCodeTool(searchCodeUseCase),
        "get_file"          to GetFileTool(getFileUseCase),
        "get_symbol"        to GetSymbolTool(getSymbolUseCase),
        "get_dependencies"  to GetDependenciesTool(...),
    )

    suspend fun execute(call: ToolCall): ToolResult =
        tools[call.toolName]?.execute(call.parameters)
            ?: ToolResult.Error("Unknown tool: ${call.toolName}")
}
```

### 3. AgentRunScreen (FIGMA-19)

```
TopBar: "Agent Running..." / "Completed" / "Failed"
       + Cancel button (only when running)

Goal card: description of what agent is doing

Step list (LazyColumn):
  Each step: AgentStepRow(step, expanded, onExpand)

FinalAnswerPanel (slides up when complete):
  Rendered with DevOSMarkdownText
  "Copy answer" button
  "Ask follow-up" button → AIChatScreen
```

### 4. AgentStepRow

```kotlin
@Composable
fun AgentStepRow(step: AgentStep, expanded: Boolean, onExpandToggle: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandToggle() }
                .padding(MaterialTheme.spacing.listItemVerticalPadding, MaterialTheme.spacing.base)
        ) {
            StepStatusIcon(step.status)   // ○ ⟳ ✓ ✗
            Spacer(Modifier.width(MaterialTheme.spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(step.toolCall?.toolName ?: "Thinking...", style = MaterialTheme.typography.titleSmall)
                if (step.thought.isNotBlank())
                    Text(step.thought.take(80) + "...", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${step.durationMs}ms", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (expanded && step.toolCall != null) {
            DevOSToolExecution(
                step = step,
                expanded = true,
                onExpandToggle = onExpandToggle,
            )
        }
    }
}
```

### 5. Destructive Action Confirmation

When `AgentProgress.AwaitingConfirmation` emitted, show bottom sheet:
```kotlin
if (uiState.pendingConfirmation != null) {
    ConfirmationBottomSheet(
        title = "Confirm: ${uiState.pendingConfirmation.toolName}",
        description = buildString {
            append("This action cannot be undone.\n\nParameters:\n")
            uiState.pendingConfirmation.parameters.forEach { (k, v) -> append("  $k: $v\n") }
        },
        onConfirm = { viewModel.confirmToolCall() },
        onDeny   = { viewModel.cancelRun() },
    )
}
```

**Tests:**
- `AgentEngineTest`: single-step completion; max steps reached; cancellation; destructive tool blocked
- `AgentRunViewModelTest`: running state; step progress; final answer; cancel
- `AgentRunScreenTest`: steps render with correct icons; expand/collapse works; final answer appears
- `ToolRegistryTest`: search_code tool returns results; unknown tool returns error

**Acceptance Criteria:**
- [ ] Agent completes a code analysis task in ≤10 steps
- [ ] Each step shows thought + tool name in real time
- [ ] Step expansion shows input params and output
- [ ] Cancel button stops execution and shows cancelled state
- [ ] Destructive tools trigger confirmation bottom sheet
- [ ] Denying confirmation cancels the run
- [ ] Final answer rendered as markdown
- [ ] Max 20 steps enforced
- [ ] Agent run persisted in Room
