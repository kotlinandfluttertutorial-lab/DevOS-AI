package com.devos.ai.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.devos.ai.designsystem.components.DevOSAIMessage
import com.devos.ai.designsystem.components.DevOSChatInput
import com.devos.ai.designsystem.components.DevOSChip
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSMessageRole
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.model.ChatMessage
import com.devos.ai.domain.ai.model.MessageRole
import com.devos.ai.domain.ai.model.SourceReference

/**
 * AI Chat screen — primary AI interaction surface for DevOS AI.
 *
 * Stateless composable. All state and events are supplied via parameters.
 * The actual ViewModel is wired in [AIChatNavigation].
 *
 * Implements:
 * - DEVOS-026 / DA-38: AI Chat core
 * - DEVOS-027 / DA-42: Context selector chip row
 * - DEVOS-028 / DA-44: Suggested action chips
 *
 * Figma: FIGMA-16 (#s-ai-chat)
 */
@Composable
fun AIChatScreen(
    uiState: AIChatUiState,
    onSendMessage: (String) -> Unit,
    onActionChipTap: (ActionChip) -> Unit,
    onSetContext: (AIContext) -> Unit,
    onClearConversation: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAnswer: (String) -> Unit,
    onNavigateToCode: (String, Int) -> Unit,
    onNavigateToAgentRun: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show streaming error as snackbar (inline, not full-screen)
    val streamingError = (uiState as? AIChatUiState.Success)?.streamingError
    LaunchedEffect(streamingError) {
        if (streamingError != null) {
            snackbarHostState.showSnackbar(streamingError)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AIChatTopBar(
                context = when (uiState) {
                    is AIChatUiState.Success -> uiState.context
                    is AIChatUiState.Empty   -> uiState.context
                    else -> AIContext.Global
                },
                onNavigateBack = onNavigateBack,
                onClearConversation = onClearConversation,
            )
        },
        bottomBar = {
            AIChatInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = uiState !is AIChatUiState.Loading,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Context selector chip row (below top bar)
            val contextItems = contextChipsFor(
                current = when (uiState) {
                    is AIChatUiState.Success -> uiState.context
                    is AIChatUiState.Empty   -> uiState.context
                    else -> AIContext.Global
                },
            )
            ContextSelectorRow(
                items = contextItems,
                onSelect = onSetContext,
            )

            // Main content area
            when (val state = uiState) {
                is AIChatUiState.Loading -> DevOSLoadingState()

                is AIChatUiState.Error -> DevOSErrorState(
                    description = state.message,
                    onRetry = if (state.retryable) ({ /* retry via navEvent */ }) else null,
                )

                is AIChatUiState.Empty -> EmptyChatContent(
                    actionChips = state.actionChips,
                    inputText = inputText,
                    onActionChipTap = { chip ->
                        inputText = chip.promptPrefix
                        onActionChipTap(chip)
                    },
                )

                is AIChatUiState.Success -> ChatMessageList(
                    messages = state.messages,
                    isStreaming = state.isStreaming,
                    inputText = inputText,
                    actionChips = defaultActionChips,
                    onActionChipTap = { chip ->
                        inputText = chip.promptPrefix
                        onActionChipTap(chip)
                    },
                    onSourceTap = onNavigateToCode,
                )
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIChatTopBar(
    context: AIContext,
    onNavigateBack: () -> Unit,
    onClearConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DevOSSpacing.xxs,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DevOSSpacing.topBarHeight)
                .padding(horizontal = DevOSSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back arrow
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(DevOSSpacing.touchTarget)
                    .semantics { contentDescription = "Navigate back" },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Title
            Text(
                text = "DevOS AI",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )

            // Context chip
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(DevOSSpacing.md),
                    )
                    .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.xs)
                    .semantics { contentDescription = "AI context: ${context.label}" },
            ) {
                Text(
                    text = context.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.width(DevOSSpacing.xs))

            // Clear / trash icon
            IconButton(
                onClick = onClearConversation,
                modifier = Modifier
                    .size(DevOSSpacing.touchTarget)
                    .semantics { contentDescription = "Clear conversation" },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Context selector row ────────────────────────────────────────────────────────

private data class ContextChipItem(
    val context: AIContext,
    val label: String,
    val isActive: Boolean,
)

private fun contextChipsFor(current: AIContext): List<ContextChipItem> = listOf(
    ContextChipItem(
        context = AIContext.Global,
        label = "🌐 Global",
        isActive = current is AIContext.Global,
    ),
    ContextChipItem(
        context = AIContext.Repository(repoId = "devos-ai", name = "devos-ai"),
        label = "📁 devos-ai",
        isActive = current is AIContext.Repository && (current as AIContext.Repository).name == "devos-ai",
    ),
    ContextChipItem(
        context = AIContext.File(repoId = "devos-ai", path = "feature/feature-ai-chat/AIChatViewModel.kt"),
        label = "📄 AIChatViewModel.kt",
        isActive = current is AIContext.File,
    ),
)

@Composable
private fun ContextSelectorRow(
    items: List<ContextChipItem>,
    onSelect: (AIContext) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        items(items = items, key = { it.label }) { item ->
            FilterChip(
                selected = item.isActive,
                onClick = { onSelect(item.context) },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = "Context: ${item.label}"
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

// ── Action chips row ────────────────────────────────────────────────────────────

@Composable
private fun ActionChipsRow(
    chips: List<ActionChip>,
    onChipTap: (ActionChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = DevOSSpacing.base, end = DevOSSpacing.base, bottom = DevOSSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        items(items = chips, key = { it.id }) { chip ->
            Surface(
                onClick = { onChipTap(chip) },
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.semantics { contentDescription = chip.label },
            ) {
                Text(
                    text = chip.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(
                        horizontal = DevOSSpacing.sm,
                        vertical = DevOSSpacing.xs,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Empty state content ─────────────────────────────────────────────────────────

@Composable
private fun EmptyChatContent(
    actionChips: List<ActionChip>,
    inputText: String,
    onActionChipTap: (ActionChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = inputText.isBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ActionChipsRow(
                chips = actionChips,
                onChipTap = onActionChipTap,
            )
        }

        DevOSEmptyState(
            icon = Icons.Outlined.Code,
            title = "Ask DevOS anything",
            description = "Ask about your code, architecture, bugs, or best practices. Your AI developer companion is ready.",
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Chat message list ─────────────────────────────────────────────────────────

@Composable
private fun ChatMessageList(
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    inputText: String,
    actionChips: List<ActionChip>,
    onActionChipTap: (ActionChip) -> Unit,
    onSourceTap: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Scroll to the bottom whenever messages change
    LaunchedEffect(messages.size, isStreaming) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1 + if (isStreaming) 1 else 0)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            horizontal = DevOSSpacing.sm,
            vertical = DevOSSpacing.sm,
        ),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        // Action chips — shown when input is empty
        item(key = "action_chips") {
            AnimatedVisibility(
                visible = inputText.isBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ActionChipsRow(
                    chips = actionChips,
                    onChipTap = onActionChipTap,
                )
            }
        }

        // Messages
        items(items = messages, key = { it.id }) { message ->
            when (message.role) {
                MessageRole.USER -> DevOSAIMessage(
                    content = message.content,
                    role = DevOSMessageRole.USER,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Your message: ${message.content.take(100)}" },
                )

                MessageRole.AI -> AIMessageWithAvatar(
                    message = message,
                    onSourceTap = onSourceTap,
                )

                MessageRole.SYSTEM -> { /* System messages are not rendered */ }
            }
        }

        // Thinking indicator — shown while streaming and last message is user
        if (isStreaming && messages.lastOrNull()?.role == MessageRole.USER) {
            item(key = "thinking_indicator") {
                ThinkingIndicator()
            }
        }
    }
}

// ── AI message with avatar ────────────────────────────────────────────────────

@Composable
private fun AIMessageWithAvatar(
    message: ChatMessage,
    onSourceTap: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DevOSSpacing.xs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        // 32dp gradient circle avatar
        Box(
            modifier = Modifier
                .size(DevOSSpacing.iconSizeLarge) // 32dp
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF82AAFF), // --primary
                            Color(0xFF89DDFF), // --secondary
                        ),
                    ),
                )
                .semantics { contentDescription = "AI avatar" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🤖",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            // AI message bubble
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "AI response: ${message.content.take(100)}"
                    },
            ) {
                DevOSAIMessage(
                    content = message.content,
                    role = DevOSMessageRole.AI,
                    modifier = Modifier.padding(DevOSSpacing.sm),
                )
            }

            // Source reference chips below AI bubble
            if (message.sources.isNotEmpty()) {
                SourceReferencesRow(
                    sources = message.sources,
                    onSourceTap = onSourceTap,
                )
            }
        }
    }
}

// ── Source reference chips ────────────────────────────────────────────────────

@Composable
private fun SourceReferencesRow(
    sources: List<SourceReference>,
    onSourceTap: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
    ) {
        items(items = sources, key = { "${it.filePath}:${it.lineStart}" }) { source ->
            val fileName = source.filePath.substringAfterLast('/')
            Surface(
                onClick = { onSourceTap(source.filePath, source.lineStart) },
                shape = RoundedCornerShape(DevOSSpacing.sm),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.semantics {
                    contentDescription = "Source: $fileName line ${source.lineStart}"
                },
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = DevOSSpacing.sm,
                        vertical = DevOSSpacing.xs,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = null,
                        modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

// ── Thinking indicator ─────────────────────────────────────────────────────────

@Composable
private fun ThinkingIndicator(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")

    // 3 dots with staggered alpha animations
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot1_alpha",
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot2_alpha",
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot3_alpha",
    )

    Row(
        modifier = modifier
            .padding(start = DevOSSpacing.xxl + DevOSSpacing.sm) // align with AI bubble content
            .padding(vertical = DevOSSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
    ) {
        repeat(3) { index ->
            val alpha = when (index) {
                0 -> dot1Alpha
                1 -> dot2Alpha
                else -> dot3Alpha
            }
            Box(
                modifier = Modifier
                    .size(DevOSSpacing.sm)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .alpha(alpha),
            )
        }
        Spacer(modifier = Modifier.width(DevOSSpacing.xs))
        Text(
            text = "Thinking…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun AIChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DevOSSpacing.xxs,
    ) {
        Column {
            // 1dp top divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DevOSSpacing.dividerThickness)
                    .background(MaterialTheme.colorScheme.outline),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DevOSSpacing.md,
                        end = DevOSSpacing.md,
                        top = DevOSSpacing.sm,
                        bottom = DevOSSpacing.xl, // 24dp safe-area-like bottom padding
                    ),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                // Attach icon button
                IconButton(
                    onClick = { /* attach action — not implemented in this ticket */ },
                    enabled = enabled,
                    modifier = Modifier
                        .size(DevOSSpacing.touchTarget)
                        .semantics { contentDescription = "Attach file" },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Attachment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Text input via design system component (fills remaining space)
                DevOSChatInput(
                    value = inputText,
                    onValueChange = onInputChange,
                    onSend = onSend,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )

                // Mic icon button
                IconButton(
                    onClick = { /* voice input — not implemented in this ticket */ },
                    enabled = enabled,
                    modifier = Modifier
                        .size(DevOSSpacing.touchTarget)
                        .semantics { contentDescription = "Voice input" },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
