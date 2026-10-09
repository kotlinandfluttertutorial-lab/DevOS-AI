package com.devos.ai.feature.code.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSCodeTextStyle
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.code.model.SearchResult

/**
 * Code Search screen — DEVOS-021 / FIGMA-12.
 *
 * Shows a monospace search input with filter chips (Case/Regex/Semantic/Scope)
 * and a result list with highlighted match snippets.
 */
@Composable
fun CodeSearchScreen(
    uiState: CodeSearchUiState,
    query: String,
    caseEnabled: Boolean,
    regexEnabled: Boolean,
    semanticEnabled: Boolean,
    onQueryChange: (String) -> Unit,
    onCaseToggle: () -> Unit,
    onRegexToggle: () -> Unit,
    onSemanticToggle: () -> Unit,
    onResultTap: (SearchResult) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                DevOSTopBar(
                    title = "Code Search",
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

                // Search input
                SearchInput(
                    query = query,
                    onQueryChange = onQueryChange,
                    hitCount = if (uiState is CodeSearchUiState.Success) uiState.totalCount else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DevOSSpacing.base),
                )

                Spacer(modifier = Modifier.height(DevOSSpacing.sm))

                // Filter chips
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = DevOSSpacing.base),
                    horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
                ) {
                    FilterChip(
                        selected = caseEnabled,
                        onClick = onCaseToggle,
                        label = {
                            Text("Aa Case", style = MaterialTheme.typography.labelSmall)
                        },
                        colors = activeFilterColors(),
                    )
                    FilterChip(
                        selected = regexEnabled,
                        onClick = onRegexToggle,
                        label = {
                            Text(".* Regex", style = MaterialTheme.typography.labelSmall)
                        },
                        colors = activeFilterColors(),
                    )
                    FilterChip(
                        selected = semanticEnabled,
                        onClick = onSemanticToggle,
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                                )
                                Text("Semantic", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        colors = activeFilterColors(),
                    )
                    FilterChip(
                        selected = false,
                        onClick = {},
                        label = {
                            Text("Scope: All", style = MaterialTheme.typography.labelSmall)
                        },
                    )
                }

                Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            }
        },
    ) { innerPadding ->
        when (val s = uiState) {
            is CodeSearchUiState.Idle -> SearchIdleHint(
                modifier = Modifier.padding(innerPadding),
            )
            is CodeSearchUiState.Searching -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )
            is CodeSearchUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.SearchOff,
                title = "No results",
                description = "Try a different search term or toggle search mode",
                modifier = Modifier.padding(innerPadding),
            )
            is CodeSearchUiState.Error -> DevOSErrorState(
                description = s.message,
                onRetry = if (s.retryable) ({}) else null,
                modifier = Modifier.padding(innerPadding),
            )
            is CodeSearchUiState.Success -> {
                Column(modifier = Modifier.padding(innerPadding)) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(s.results, key = { _, r -> "${r.filePath}:${r.lineNumber}" }) { index, result ->
                            SearchResultItem(
                                result = result,
                                query = s.query,
                                onClick = { onResultTap(result) },
                            )
                            if (index < s.results.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    thickness = DevOSSpacing.dividerThickness,
                                    modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                                )
                            }
                        }
                    }

                    // "N more results" footer
                    if (s.totalCount > s.results.size) {
                        Text(
                            text = "+ ${s.totalCount - s.results.size} more results",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DevOSSpacing.base),
                        )
                    }
                }
            }
        }
    }
}

// ── Search input ──────────────────────────────────────────────────────────────

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    hitCount: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
            )
            .border(
                width = DevOSSpacing.xxs,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
        )

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Code search input" },
            singleLine = true,
            textStyle = DevOSCodeTextStyle.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Search code…",
                        style = DevOSCodeTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
            },
        )

        if (hitCount != null) {
            Text(
                text = "$hitCount hits",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Idle hint ─────────────────────────────────────────────────────────────────

@Composable
private fun SearchIdleHint(modifier: Modifier = Modifier) {
    DevOSEmptyState(
        icon = Icons.Outlined.Search,
        title = "Search your codebase",
        description = "Use full-text, regex, or AI-powered semantic search",
        modifier = modifier,
    )
}

// ── Search result item ────────────────────────────────────────────────────────

@Composable
private fun SearchResultItem(
    result: SearchResult,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = DevOSSpacing.base,
                vertical = DevOSSpacing.sm,
            )
            .semantics {
                contentDescription = "${result.fileName} line ${result.lineNumber}"
                role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
    ) {
        // File name + line number header
        Row(
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = result.fileName,
                style = DevOSCodeTextStyle.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "line ${result.lineNumber}",
                style = DevOSCodeTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Highlighted code snippet
        val annotated = buildAnnotatedString {
            val content = result.lineContent
            val start = result.matchStart.coerceIn(0, content.length)
            val end = result.matchEnd.coerceIn(start, content.length)

            append(content.substring(0, start))
            withStyle(
                SpanStyle(
                    background = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append(content.substring(start, end))
            }
            append(content.substring(end))
        }

        Text(
            text = annotated,
            style = DevOSCodeTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

// ── Filter chip colors helper ─────────────────────────────────────────────────

@Composable
private fun activeFilterColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
)
