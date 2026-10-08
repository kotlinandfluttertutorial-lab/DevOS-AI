package com.devos.ai.feature.repository.sync

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.repository.model.StepState
import com.devos.ai.feature.repository.model.SyncStep

/**
 * Repository Sync progress screen — stateless composable matching the #s-repo-sync mockup.
 *
 * Shows an animated spinner, step-by-step pipeline progress, overall progress bar,
 * and a Cancel button.
 *
 * All state and callbacks are supplied by [RepositoryNavigation].
 */
@Composable
fun RepositorySyncScreen(
    uiState: SyncUiState,
    repoName: String,
    provider: String = "GitHub",
    branch: String = "main",
    onCancel: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Syncing Repository",
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(DevOSSpacing.touchTarget)
                            .semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is SyncUiState.Error -> {
                DevOSErrorState(
                    description = uiState.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    onRetry = if (uiState.retryable) onCancel else null,
                )
            }

            is SyncUiState.Complete -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Sync complete",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(DevOSSpacing.iconSizeXLarge),
                    )
                    Spacer(modifier = Modifier.height(DevOSSpacing.base))
                    Text(
                        text = "Sync complete",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                    Text(
                        text = repoName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is SyncUiState.Cancelled -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Sync cancelled",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // ── Header ─────────────────────────────────────────────────
                    Spacer(modifier = Modifier.height(DevOSSpacing.xl))

                    Text(
                        text = repoName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                    Text(
                        text = "Syncing from $provider · $branch",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(DevOSSpacing.lg))

                    // ── Animated spinner ───────────────────────────────────────
                    SyncSpinner(
                        modifier = Modifier.size(DevOSSpacing.spinnerSize),
                    )

                    Spacer(modifier = Modifier.height(DevOSSpacing.base))

                    // ── "Indexing files…" label ────────────────────────────────
                    if (uiState is SyncUiState.Syncing) {
                        Text(
                            text = "Indexing files…",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(DevOSSpacing.xl))

                    // ── Steps list ─────────────────────────────────────────────
                    if (uiState is SyncUiState.Syncing) {
                        val steps = uiState.steps
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DevOSSpacing.base),
                        ) {
                            steps.forEachIndexed { index, step ->
                                SyncStepRow(
                                    step = step,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (index < steps.lastIndex) {
                                    // Connector line between steps
                                    val prevComplete = step.state == StepState.COMPLETE
                                    val nextComplete = steps[index + 1].state == StepState.COMPLETE
                                    val connectorColor = if (prevComplete && nextComplete) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(start = DevOSSpacing.lg + DevOSSpacing.sm) // 20+8=28dp to align under circle center
                                            .width(DevOSSpacing.connectorWidth)
                                            .height(DevOSSpacing.xl)
                                            .background(connectorColor),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(DevOSSpacing.base))

                        // ── Overall progress ───────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DevOSSpacing.base),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Overall progress",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${(uiState.overallProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                            LinearProgressIndicator(
                                progress = { uiState.overallProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Sync progress ${(uiState.overallProgress * 100).toInt()}%" },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DevOSSpacing.xl))

                    // ── Cancel button ──────────────────────────────────────────
                    DevOSButton(
                        text = "Cancel Sync",
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DevOSSpacing.base),
                        style = DevOSButtonStyle.Secondary,
                    )

                    Spacer(modifier = Modifier.height(DevOSSpacing.base))
                }
            }
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

/**
 * Animated circular spinner matching the mockup's CSS spin animation.
 *
 * Renders an 80dp circle with a spinning arc using InfiniteTransition.
 */
@Composable
private fun SyncSpinner(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinner_rotation",
    )

    Box(
        modifier = modifier.semantics { contentDescription = "Syncing in progress" },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation),
            strokeWidth = DevOSSpacing.strokeWidthNormal,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * A single row in the sync steps list.
 *
 * COMPLETE → tertiary circle with checkmark + "Done" badge
 * IN_PROGRESS → spinning CircularProgressIndicator (small)
 * PENDING → outlined empty circle
 * FAILED → error circle with X icon
 */
@Composable
private fun SyncStepRow(
    step: SyncStep,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(DevOSSpacing.touchTarget)
            .semantics {
                contentDescription = "${step.name}: ${step.state.name.lowercase().replace('_', ' ')}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Step icon (40dp touch target)
        StepIcon(state = step.state)

        Spacer(modifier = Modifier.width(DevOSSpacing.sm))

        // Step text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = step.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Optional badge
        when (step.state) {
            StepState.COMPLETE -> {
                DevOSStatusBadge(
                    status = DevOSBadgeStatus.SUCCESS,
                    label = "Done",
                )
            }
            StepState.IN_PROGRESS -> {
                val progressText = step.subtitle
                    .substringBefore(" /")
                    .trim()
                    .toIntOrNull()
                val totalText = step.subtitle
                    .substringAfter("/ ")
                    .substringBefore(" ")
                    .trim()
                    .toIntOrNull()
                if (progressText != null && totalText != null && totalText > 0) {
                    Text(
                        text = "${(progressText * 100 / totalText)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            else -> Unit
        }
    }
}

/**
 * Circular icon for each step state.
 */
@Composable
private fun StepIcon(
    state: StepState,
    modifier: Modifier = Modifier,
) {
    val size = DevOSSpacing.stepIconSize
    when (state) {
        StepState.COMPLETE -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Complete",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                )
            }
        }

        StepState.IN_PROGRESS -> {
            Box(
                modifier = modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(DevOSSpacing.lg),
                    strokeWidth = DevOSSpacing.strokeWidthThin,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        StepState.PENDING -> {
            Box(
                modifier = modifier
                    .size(size)
                    .border(
                        width = DevOSSpacing.dividerThickness,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {}
        }

        StepState.FAILED -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                )
            }
        }
    }
}
