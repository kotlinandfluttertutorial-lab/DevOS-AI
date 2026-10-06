# Kiro Prompt — DEVOS-026: AI Chat Screen

**Jira:** DEVOS-026 / DEVOS-027 / DEVOS-028  
**Epic:** DEVOS-E04  
**Figma:** FIGMA-16  
**Kiro Spec:** `.kiro/specs/ai-platform/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the AI Chat screen — the primary interaction surface of DevOS AI.

**Existing files to read first:**
- `.kiro/specs/ai-platform/core.md` — full architecture and models
- `docs/figma/screen-inventory.md` — Screen 16 specification
- `docs/figma/component-inventory.md` — DevOSAIMessage, DevOSChatInput, DevOSContextSelector, DevOSActionChip
- `.kiro/steering/android.md` — architecture rules

**Module:** `feature/feature-ai-chat/`  
**Package:** `com.devos.ai.feature.chat`

**What to implement:**

### 1. UiState
```
AIChatUiState:
  Loading
  Success(messages, context, isStreaming, streamingError?)
  Empty  (no conversation yet)
  Error(message, retryable)
```

### 2. AIChatViewModel
- `sendMessage(text: String)` — streaming via Flow
- `setContext(context: AIContext)` — update context selector
- `retry()` — replay last failed message
- `clearConversation()` — confirm dialog → clear
- One-time events via SharedFlow: `NavigateToAnswer(answerId)`, `NavigateToCode(path, line)`

### 3. AIChatScreen Composable
Layout (top to bottom):
```
DevOSTopBar("DevOS AI", actions = [ContextButton, ClearButton])
DevOSContextSelector (horizontal chip row — current context)
LazyColumn (messages, reverse=false, scroll to bottom on new)
  - DevOSAIMessage for AI messages (markdown + sources + agent steps)
  - DevOSUserMessage for user messages (right-aligned bubble)
  - DevOSLoadingState (shimmer bubble when isStreaming=true + no message yet)
DevOSActionChips (Explain | Find | Debug | Analyze | Review | Learn | Build)
  → visible only when input is empty
DevOSChatInput (text field + send + attach)
```

### 4. Message Rendering
- AI message: `DevOSAIMessage` with `DevOSMarkdownText` content
- Source references: `DevOSSourceReference` chips below message
- Agent steps: inline `DevOSAgentStep` list (expandable)
- User message: right-aligned bubble, `surfaceVariant` background, `bodyLarge` text

### 5. Context Selector
Bottom sheet with options:
- Global workspace
- Current project (if one is active)
- Current repository (if one is open)
- Current file (if code viewer is open)
- Current symbol (if symbol is selected)

### 6. Navigation
```kotlin
fun NavGraphBuilder.aiChatNavigation(navController: NavController) {
    composable(DevOSRoutes.AI_CHAT) {
        AIChatScreen(
            onNavigateToAnswer = { navController.navigate("ai/answer/$it") },
            onNavigateToCode = { repoId, path, line -> ... },
            onNavigateToAgentRun = { navController.navigate("agent/run/$it") },
        )
    }
}
```

**Streaming behavior:**
- Each token appended to last AI message in real time
- Message list does NOT reload — update in-place via `_uiState.update {}`
- Animated "..." cursor while streaming
- Error banner (not full error state) if streaming fails mid-message

**Accessibility:**
- AI messages: `contentDescription = "AI response: ${message.content.take(100)}"`
- Send button: `contentDescription = "Send message"`
- Context selector: `contentDescription = "AI context: ${context.label}"`

**Tests to write:**
- `AIChatViewModelTest`: loading, success, streaming, error states
- `AIChatScreenTest`: all 4 UI states render; send fires callback; action chips hide on input
- `SendMessageUseCaseTest`: RAG retrieval called; memory injected; stream collected

**Acceptance Criteria:**
- [ ] Messages appear as they stream (character by character)
- [ ] Context selector changes RAG retrieval scope
- [ ] All 7 action chips render and pre-fill input on tap
- [ ] Sources rendered below AI answers
- [ ] Agent steps shown when present
- [ ] Clear conversation shows confirmation dialog
- [ ] Empty state: "Ask DevOS anything..." with suggested prompts
- [ ] Error state shows inline banner, not full-screen error
