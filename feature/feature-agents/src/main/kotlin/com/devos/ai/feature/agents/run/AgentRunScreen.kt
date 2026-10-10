package com.devos.ai.feature.agents.run

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSMarkdownText
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.domain.ai.model.AgentRun
import com.devos.ai.domain.ai.model.AgentStep
import com.devos.ai.domain.ai.model.AgentStepStatus

/**
 * Agent Run screen — DEVOS-035 / FIGMA-19.
 *
 * Matches the `#s-agent-run` mockup exactly:
 * - Top bar: back + "Agent Run" + green "● Running" status pill
 * - Goal card with agent name, goal text, step progress label, elapsed time, LinearProgressIndicator
 * - Step list: done (✓ green), active (⟳ pulsing primary), pending (○ surfaceVariant)
 * - Connector lines between steps: green, gradient, or grey
 * - Active step expands tool call box (amber text, monospace args)
 * - "Cancel Agent" full-width outlined button in error red
 * - On COMPLETED: final answer rendered via DevOSMarkdownText
 */
@Composable
fun AgentRunScreen(
    uiState: AgentRunUiState,
    onNavigateBack: () -> Unit,
    onCancelRun: () -> Unit,
    onStepTap: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusPill: @Composable () -> Unit = {
        val (bg, fg, label) = when (uiState) {
            is AgentRunUiState.Running   -> Triple(Color(0xFF2D5000), Color(0xFFC3E88D), "● Running")
            is AgentRunUiState.Completed -> Triple(Color(0xFF2D5000), Color(0xFFC3E88D), "✓ Done")
            is AgentRunUiState.Cancelled -> Triple(Color(0xFF252840), Color(0xFFA8B3CF), "Cancelled")
            is AgentRunUiState.Error     -> Triple(Color(0xFF930000), Color(0xFFFF5370), "Failed")
            else                         -> Triple(Color(0xFF252840), Color(0xFFA8B3CF), "…")
        }
        Box(
            modifier = Modifier
                .background(bg, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    Scaffold(
        modifier       = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar         = {
            DevOSTopBar(
                title          = "Agent Run",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                actions        = { statusPill() },
            )
        },
    ) { innerPadding ->
        when (val s = uiState) {
            is AgentRunUiState.Loading   -> DevOSLoadingState(modifier = Modifier.padding(innerPadding))

            is AgentRunUiState.Error     -> DevOSErrorState(
                description = s.message,
                onRetry     = if (s.retryable) onRetry else null,
                modifier    = Modifier.padding(innerPadding),
            )

            is AgentRunUiState.Running   -> AgentRunContent(
                state        = s,
                onCancelRun  = onCancelRun,
                onStepTap    = onStepTap,
                modifier     = Modifier.padding(innerPadding),
            )

            is AgentRunUiState.Completed -> AgentCompletedContent(
                state    = s,
                modifier = Modifier.padding(innerPadding),
            )

            is AgentRunUiState.Cancelled -> DevOSEmptyState(
                icon        = Icons.Outlined.Error,
                title       = "Run cancelled",
                description = "The agent run was cancelled by the user.",
                modifier    = Modifier.padding(innerPadding),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Running state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AgentRunContent(
    state: AgentRunUiState.Running,
    onCancelRun: () -> Unit,
    onStepTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier       = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = DevOSSpacing.xl),
    ) {
        // ── Goal card ─────────────────────────────────────────────────────────
        item(key = "goal_card") {
            GoalCard(
                run          = state.run,
                progress     = state.progress,
                elapsedLabel = state.elapsedLabel,
                stepLabel    = state.stepLabel,
            )
        }

        // ── Step list ─────────────────────────────────────────────────────────
        items(
            items = state.run.steps,
            key   = { it.id },
        ) { step ->
            val isLast = step == state.run.steps.lastOrNull()
            StepRow(
                step      = step,
                isLast    = isLast,
                nextStep  = state.run.steps.getOrNull(state.run.steps.indexOf(step) + 1),
                onTap     = { onStepTap(step.id) },
            )
        }

        // ── Cancel button ─────────────────────────────────────────────────────
        item(key = "cancel_btn") {
            Box(
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical   = DevOSSpacing.base,
                ),
            ) {
                DevOSButton(
                    text     = "Cancel Agent",
                    onClick  = onCancelRun,
                    style    = DevOSButtonStyle.Destructive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Cancel agent run" },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Goal card — matches the header card in the mockup
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GoalCard(
    run: AgentRun,
    progress: Float,
    elapsedLabel: String,
    stepLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width  = 1.dp,
                color  = MaterialTheme.colorScheme.outline,
                shape  = RoundedCornerShape(0.dp),  // flush with screen edges
            )
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
    ) {
        // Agent name
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            Icon(
                imageVector        = Icons.Outlined.SmartToy,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(DevOSSpacing.iconSizeSmall),
            )
            Text(
                text       = "Code Review Agent",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(DevOSSpacing.xs))

        // Goal text
        Text(
            text  = "Goal: ${run.goal}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(DevOSSpacing.sm))

        // Step label + elapsed
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stepLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(elapsedLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(DevOSSpacing.xs))

        // Progress bar — blue (#82AAFF = primary)
        LinearProgressIndicator(
            progress      = { progress },
            modifier      = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color         = MaterialTheme.colorScheme.primary,
            trackColor    = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step row — matches .step CSS class in the mockup
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepRow(
    step: AgentStep,
    isLast: Boolean,
    nextStep: AgentStep?,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .semantics { role = Role.Button; contentDescription = "Step: ${step.thought.take(60)}" }
                .padding(start = DevOSSpacing.base, end = DevOSSpacing.base, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            verticalAlignment     = Alignment.Top,
        ) {
            // ── Step icon ─────────────────────────────────────────────────────
            StepIcon(status = step.status)

            // ── Step body ─────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = step.thought.lines().firstOrNull()?.take(60)?.ifBlank { "Processing…" }
                        ?: "Processing…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text  = stepSubtitle(step),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // ── Tool call box (active step only) ──────────────────────────
                if (step.status == AgentStepStatus.RUNNING && step.toolName != null) {
                    ToolCallBox(toolName = step.toolName!!, toolInput = step.toolInput)
                }
            }
        }

        // ── Connector line between steps ──────────────────────────────────────
        if (!isLast) {
            ConnectorLine(
                topStatus    = step.status,
                bottomStatus = nextStep?.status,
            )
        }
    }
}

@Composable
private fun StepIcon(status: AgentStepStatus, modifier: Modifier = Modifier) {
    val size = 28.dp

    when (status) {
        AgentStepStatus.COMPLETED -> Box(
            modifier            = modifier
                .size(size)
                .background(Color(0xFF2D5000), CircleShape),
            contentAlignment    = Alignment.Center,
        ) {
            Text("✓", color = Color(0xFF1E3700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        AgentStepStatus.RUNNING   -> {
            val pulse by rememberInfiniteTransition(label = "pulse")
                .animateFloat(
                    initialValue  = 0.9f,
                    targetValue   = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation  = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label         = "scale",
                )
            Box(
                modifier            = modifier
                    .size(size)
                    .scale(pulse)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment    = Alignment.Center,
            ) {
                Text("⟳", color = Color(0xFF001E6E), fontSize = 14.sp)
            }
        }

        AgentStepStatus.FAILED    -> Box(
            modifier            = modifier
                .size(size)
                .background(MaterialTheme.colorScheme.error, CircleShape),
            contentAlignment    = Alignment.Center,
        ) {
            Text("✗", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        else -> Box(
            modifier            = modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment    = Alignment.Center,
        ) {
            Text("○", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ConnectorLine(
    topStatus: AgentStepStatus,
    bottomStatus: AgentStepStatus?,
    modifier: Modifier = Modifier,
) {
    val brush = when {
        topStatus == AgentStepStatus.COMPLETED &&
            bottomStatus == AgentStepStatus.RUNNING  ->
            Brush.verticalGradient(listOf(Color(0xFF2D5000), Color(0xFF82AAFF)))
        topStatus == AgentStepStatus.COMPLETED       ->
            Brush.verticalGradient(listOf(Color(0xFF2D5000), Color(0xFF2D5000)))
        else ->
            Brush.verticalGradient(listOf(
                Color(0xFF3A3F58), Color(0xFF3A3F58)
            ))
    }
    // 2dp wide line, 20dp tall, left-indented to align with centre of 28dp icon (28/2 + 16dp padding = 30dp)
    Box(
        modifier = modifier
            .padding(start = 30.dp)
            .width(2.dp)
            .height(20.dp)
            .background(brush = brush),
    )
}

/** Tool call box shown inside the active step. */
@Composable
private fun ToolCallBox(toolName: String, toolInput: Map<String, Any>?) {
    val amberColor = Color(0xFFFFCB6B)
    val argText    = toolInput?.entries?.take(3)?.joinToString("\n") { "${it.key}: ${it.value}" } ?: ""

    Box(
        modifier = Modifier
            .padding(top = DevOSSpacing.sm)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column {
            Text(
                text       = "🔧 TOOL: $toolName",
                color      = amberColor,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (argText.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = argText,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize   = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

private fun stepSubtitle(step: AgentStep): String = when (step.status) {
    AgentStepStatus.PENDING   -> "Pending"
    AgentStepStatus.RUNNING   -> "Running…"
    AgentStepStatus.COMPLETED -> {
        val dur = if (step.durationMs > 0) " · ${step.durationMs / 1000.0}s" else ""
        step.toolName?.let { "Tool: $it$dur" } ?: "Completed$dur"
    }
    AgentStepStatus.FAILED    -> step.toolOutput?.take(50) ?: "Failed"
    AgentStepStatus.CANCELLED -> "Cancelled"
}

// ─────────────────────────────────────────────────────────────────────────────
// Completed state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AgentCompletedContent(
    state: AgentRunUiState.Completed,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier       = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(DevOSSpacing.base),
    ) {
        // Final answer card
        item(key = "final_answer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(DevOSSpacing.base),
            ) {
                Column {
                    Text(
                        text       = "✓ Final Answer",
                        style      = MaterialTheme.typography.titleSmall,
                        color      = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.height(DevOSSpacing.sm))
                    DevOSMarkdownText(
                        markdown = state.finalAnswer,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Completed steps
        item(key = "steps_header") {
            Spacer(Modifier.height(DevOSSpacing.base))
            Text(
                text  = "STEPS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DevOSSpacing.xs))
        }

        items(state.run.steps, key = { it.id }) { step ->
            StepRow(
                step      = step,
                isLast    = step == state.run.steps.lastOrNull(),
                nextStep  = state.run.steps.getOrNull(state.run.steps.indexOf(step) + 1),
                onTap     = {},
            )
        }
    }
}
