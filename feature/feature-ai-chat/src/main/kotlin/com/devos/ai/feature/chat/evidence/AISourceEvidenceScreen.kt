package com.devos.ai.feature.chat.evidence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSCodeBlock
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSCodeTextStyle
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.chat.answer.SourceEvidence

/**
 * AI Source Evidence screen — shows all grounding sources for an AI answer.
 *
 * Stateless composable. State and events are wired in [sourceEvidenceNavigation].
 *
 * Matches mockup #s-ai-evidence:
 * - DevOSTopBar: back arrow + "Source Evidence"
 * - Subtitle: "Grounding sources for this AI answer"
 * - Per-source DevOSCard:
 *   - Row: filename (monospace primary) + relevance badge (right-aligned)
 *   - Line range + package (11sp onSurfaceVariant)
 *   - DevOSCodeBlock (showLineNumbers=false, max ~4 lines shown)
 *   - "Open in Code Viewer →" Ghost button (32dp height)
 *
 * DEVOS-030 / DA-43
 */
@Composable
fun AISourceEvidenceScreen(
    uiState: SourceEvidenceUiState,
    onNavigateBack: () -> Unit,
    onOpenInCodeViewer: (filePath: String, line: Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Source Evidence",
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is SourceEvidenceUiState.Loading -> DevOSLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is SourceEvidenceUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is SourceEvidenceUiState.Success -> EvidenceListContent(
                sources = state.sources,
                questionSummary = state.questionSummary,
                onOpenInCodeViewer = onOpenInCodeViewer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

// ── Success content ───────────────────────────────────────────────────────────

@Composable
private fun EvidenceListContent(
    sources: List<SourceEvidence>,
    questionSummary: String,
    onOpenInCodeViewer: (filePath: String, line: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        // Subtitle
        item {
            Text(
                text = "Grounding sources for this AI answer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DevOSSpacing.base,
                        vertical = DevOSSpacing.sm + DevOSSpacing.xxs,
                    ),
            )
        }

        // Evidence cards
        items(items = sources, key = { "${it.filePath}:${it.lineStart}" }) { source ->
            EvidenceSourceCard(
                source = source,
                onOpenInCodeViewer = onOpenInCodeViewer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
            )
        }

        // Bottom spacer
        item { Spacer(modifier = Modifier.height(DevOSSpacing.xl)) }
    }
}

// ── Evidence source card ──────────────────────────────────────────────────────

@Composable
private fun EvidenceSourceCard(
    source: SourceEvidence,
    onOpenInCodeViewer: (filePath: String, line: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fileName = source.filePath.substringAfterLast('/')
    val relevancePercent = (source.relevance * 100).toInt()
    val badgeStatus = when {
        relevancePercent >= 90 -> DevOSBadgeStatus.SUCCESS
        relevancePercent >= 70 -> DevOSBadgeStatus.INFO
        else                    -> DevOSBadgeStatus.PENDING
    }

    DevOSCard(
        modifier = modifier.semantics {
            contentDescription = "Source: $fileName, $relevancePercent% relevant"
        },
    ) {
        Column(
            modifier = Modifier.padding(DevOSSpacing.base),
        ) {
            // File name + relevance badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = fileName,
                    style = DevOSCodeTextStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                DevOSStatusBadge(
                    status = badgeStatus,
                    label = "$relevancePercent% relevant",
                )
            }

            // Line range + package
            Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            Text(
                text = "Lines ${source.lineStart}–${source.lineEnd} · ${source.packageName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Code snippet
            Spacer(modifier = Modifier.height(DevOSSpacing.sm))
            DevOSCodeBlock(
                code = source.snippet,
                language = source.language,
                showLineNumbers = false,
                modifier = Modifier.fillMaxWidth(),
            )

            // "Open in Code Viewer" ghost button
            Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            DevOSButton(
                text = "Open in Code Viewer →",
                onClick = { onOpenInCodeViewer(source.filePath, source.lineStart) },
                style = DevOSButtonStyle.Ghost,
                modifier = Modifier
                    .height(DevOSSpacing.touchTarget)
                    .semantics {
                        contentDescription = "Open ${source.filePath} at line ${source.lineStart} in Code Viewer"
                    },
            )
        }
    }
}
