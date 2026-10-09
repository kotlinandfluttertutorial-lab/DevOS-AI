package com.devos.ai.feature.chat.answer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSMarkdownText
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSCodeTextStyle
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * AI Answer Detail screen — shows the full AI answer with source evidence rows.
 *
 * Stateless composable. State and events are wired in [answerDetailNavigation].
 *
 * Matches mockup #s-ai-answer:
 * - DevOSTopBar: back arrow + "AI Answer" + copy icon
 * - Question card
 * - Full markdown answer body (DevOSMarkdownText)
 * - "Source Evidence (N)" section header
 * - Single DevOSCard containing evidence rows with 1dp dividers
 * - Actions row: "View Sources" (Secondary) + "Ask Follow-up" (Primary)
 *
 * DEVOS-029 / DA-40
 */
@Composable
fun AIAnswerDetailScreen(
    uiState: AnswerDetailUiState,
    onNavigateBack: () -> Unit,
    onCopyAnswer: () -> Unit,
    onViewSources: () -> Unit,
    onAskFollowUp: () -> Unit,
    onSourceTap: (filePath: String, line: Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "AI Answer",
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
                actions = {
                    IconButton(
                        onClick = onCopyAnswer,
                        modifier = Modifier.semantics { contentDescription = "Copy answer" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is AnswerDetailUiState.Loading -> DevOSLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is AnswerDetailUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is AnswerDetailUiState.Success -> AnswerDetailContent(
                answer = state.answer,
                onViewSources = onViewSources,
                onAskFollowUp = onAskFollowUp,
                onSourceTap = onSourceTap,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

// ── Success content ───────────────────────────────────────────────────────────

@Composable
private fun AnswerDetailContent(
    answer: AnswerDetail,
    onViewSources: () -> Unit,
    onAskFollowUp: () -> Unit,
    onSourceTap: (filePath: String, line: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        // ── Question card ──────────────────────────────────────────────────────
        item {
            DevOSCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base)
                    .padding(top = DevOSSpacing.base),
            ) {
                Column(
                    modifier = Modifier.padding(DevOSSpacing.base),
                ) {
                    Text(
                        text = "Your question:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                    Text(
                        text = answer.question,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        // ── Answer body (full markdown) ────────────────────────────────────────
        item {
            DevOSMarkdownText(
                markdown = answer.answer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base)
                    .padding(top = DevOSSpacing.base, bottom = DevOSSpacing.sm),
            )
        }

        // ── Source evidence section header ─────────────────────────────────────
        item {
            DevOSSectionHeader(
                title = "Source Evidence (${answer.sources.size})",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
            )
        }

        // ── Evidence rows card ─────────────────────────────────────────────────
        if (answer.sources.isNotEmpty()) {
            item {
                EvidenceListCard(
                    sources = answer.sources,
                    onSourceTap = onSourceTap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DevOSSpacing.base),
                )
            }
        }

        // ── Actions row ────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base)
                    .padding(bottom = DevOSSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                DevOSButton(
                    text = "View Sources",
                    onClick = onViewSources,
                    style = DevOSButtonStyle.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "View source evidence" },
                )
                DevOSButton(
                    text = "Ask Follow-up",
                    onClick = onAskFollowUp,
                    style = DevOSButtonStyle.Primary,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Ask a follow-up question" },
                )
            }
        }
    }
}

// ── Evidence list card ────────────────────────────────────────────────────────

@Composable
private fun EvidenceListCard(
    sources: List<SourceEvidence>,
    onSourceTap: (filePath: String, line: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DevOSCard(modifier = modifier) {
        Column {
            sources.forEachIndexed { index, source ->
                EvidenceRow(
                    source = source,
                    onClick = { onSourceTap(source.filePath, source.lineStart) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Source: ${source.filePath.substringAfterLast('/')}, " +
                                "${(source.relevance * 100).toInt()}% relevant"
                        },
                )
                if (index < sources.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = DevOSSpacing.dividerThickness,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvidenceRow(
    source: SourceEvidence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fileName = source.filePath.substringAfterLast('/')
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = DevOSSpacing.sm + DevOSSpacing.xs, vertical = DevOSSpacing.sm),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(
                text = "$fileName · lines ${source.lineStart}–${source.lineEnd}",
                style = DevOSCodeTextStyle,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.xxs))
            Text(
                text = "Relevance: ${(source.relevance * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
