package com.devos.ai.feature.memory

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSSearchBar
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing

/**
 * Developer Memory screen (DEVOS-055 / DA-68).
 *
 * Stateless composable — all state comes from [DeveloperMemoryUiState],
 * all events go up via callback parameters.
 *
 * Matches mockup #s-memory.
 */
@Composable
fun DeveloperMemoryScreen(
    uiState: DeveloperMemoryUiState,
    onNavigateBack: () -> Unit,
    onDismissEntry: (String) -> Unit,
    onClearAll: () -> Unit,
    onCancelClear: () -> Unit,
    onConfirmClear: () -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DevOSTopBar(
                title = "Developer Memory",
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.semantics { contentDescription = "Search memories" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is DeveloperMemoryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    DevOSLoadingState()
                }
            }

            is DeveloperMemoryUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    DevOSEmptyState(
                        icon = Icons.Outlined.Search,
                        title = "No memories yet",
                        description = "AI will remember your preferences and decisions from past sessions",
                    )
                }
            }

            is DeveloperMemoryUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    DevOSErrorState(
                        description = state.message,
                        onRetry = null,
                    )
                }
            }

            is DeveloperMemoryUiState.Success -> {
                MemorySuccessContent(
                    state = state,
                    onDismissEntry = onDismissEntry,
                    onClearAll = onClearAll,
                    onCancelClear = onCancelClear,
                    onConfirmClear = onConfirmClear,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

// ── Success content ───────────────────────────────────────────────────────────────

@Composable
private fun MemorySuccessContent(
    state: DeveloperMemoryUiState.Success,
    onDismissEntry: (String) -> Unit,
    onClearAll: () -> Unit,
    onCancelClear: () -> Unit,
    onConfirmClear: () -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        // Subtitle
        item {
            Text(
                text = "AI remembers your preferences, decisions, and patterns from past sessions.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.base,
                    vertical = MaterialTheme.spacing.md,
                ),
            )
        }

        // Search bar
        item {
            DevOSSearchBar(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = "Search memories…",
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }

        // Section: Code Preferences
        if (state.data.codePreferences.isNotEmpty()) {
            item {
                MemorySectionHeader(title = "Code Preferences")
            }
            items(
                items = state.data.codePreferences,
                key = { it.id },
            ) { entry ->
                MemoryEntryCard(
                    entry = entry,
                    onDismiss = { onDismissEntry(entry.id) },
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.base,
                        vertical = MaterialTheme.spacing.xs,
                    ),
                )
            }
        }

        // Section: Recent Decisions
        if (state.data.recentDecisions.isNotEmpty()) {
            item {
                MemorySectionHeader(
                    title = "Recent Decisions",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                )
            }
            items(
                items = state.data.recentDecisions,
                key = { it.id },
            ) { entry ->
                MemoryEntryCard(
                    entry = entry,
                    onDismiss = { onDismissEntry(entry.id) },
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.base,
                        vertical = MaterialTheme.spacing.xs,
                    ),
                )
            }
        }

        // Divider before action area
        item {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = DevOSSpacing.dividerThickness,
                modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
            )
        }

        // Clear All Memories button
        item {
            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base)
                    .height(DevOSSpacing.touchTarget)
                    .semantics { contentDescription = "Clear all memories" },
                border = androidx.compose.foundation.BorderStroke(
                    width = DevOSSpacing.xxs,
                    color = MaterialTheme.colorScheme.error,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(
                    text = "Clear All Memories",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }

    // Clear-all confirmation dialog
    if (state.showClearDialog) {
        AlertDialog(
            onDismissRequest = onCancelClear,
            title = {
                Text(
                    text = "Clear All Memories",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all AI memories. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmClear,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelClear) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

// ── Section header ────────────────────────────────────────────────────────────────

@Composable
private fun MemorySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.W600,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(
            horizontal = MaterialTheme.spacing.base,
            vertical = MaterialTheme.spacing.sm,
        ),
    )
}

// ── Memory entry card ─────────────────────────────────────────────────────────────

@Composable
private fun MemoryEntryCard(
    entry: MemoryEntry,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DevOSCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.base,
                    vertical = MaterialTheme.spacing.md,
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            // Leading emoji / icon text
            Text(
                text = entry.emoji,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                modifier = Modifier.padding(top = MaterialTheme.spacing.xxs),
            )

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W500,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = entry.source,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Dismiss button (only when entry is dismissible)
            if (entry.isDismissible) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(DevOSSpacing.touchTarget)
                        .semantics { contentDescription = "Dismiss ${entry.title}" },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
