package com.devos.ai.feature.home.search

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSSearchBar
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing

/**
 * Search screen — DEVOS-060 / FIGMA-39.
 *
 * Stateless composable. All state and events are managed by [SearchViewModel].
 */
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    query: String,
    activeScope: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onResultTap: (SearchResult) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Top bar with custom search input ─────────────────────────────
            SearchTopBar(
                query = query,
                onBack = onBack,
                onQueryChange = onQueryChange,
            )

            // ── Scope chips ──────────────────────────────────────────────────
            SearchScopeRow(
                scopes = allSearchScopes,
                activeScope = activeScope,
                onScopeChange = onScopeChange,
            )

            Spacer(modifier = Modifier.height(DevOSSpacing.xs))

            // ── Content ──────────────────────────────────────────────────────
            when (val state = uiState) {
                is SearchUiState.Idle -> IdleContent()

                is SearchUiState.Searching -> DevOSLoadingState()

                is SearchUiState.Empty -> DevOSEmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "No results",
                    description = "Try different keywords or scope",
                )

                is SearchUiState.Error -> DevOSErrorState(
                    description = state.message,
                    onRetry = if (state.retryable) onRetry else null,
                )

                is SearchUiState.Success -> SearchResults(
                    state = state,
                    onResultTap = onResultTap,
                )
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────────

@Composable
private fun SearchTopBar(
    query: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = DevOSSpacing.xs,
                end = MaterialTheme.spacing.base,
                top = DevOSSpacing.sm,
                bottom = DevOSSpacing.xs,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
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
        DevOSSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Search everything…",
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Scope chips ───────────────────────────────────────────────────────────────────

@Composable
private fun SearchScopeRow(
    scopes: List<SearchScope>,
    activeScope: String,
    onScopeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.base),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        items(
            items = scopes,
            key = { it.id },
        ) { scope ->
            FilterChip(
                selected = activeScope == scope.id,
                onClick = { onScopeChange(scope.id) },
                label = { Text(scope.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

// ── Idle state ────────────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.base, vertical = MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        Text(
            text = "Recent searches",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf("HomeViewModel", "Coroutines Flow", "Navigation graph").forEach { recent ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DevOSSpacing.touchTarget)
                    .semantics {
                        contentDescription = "Recent search: $recent"
                        role = Role.Button
                    },
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = recent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ── Search results ────────────────────────────────────────────────────────────────

@Composable
private fun SearchResults(
    state: SearchUiState.Success,
    onResultTap: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.spacing.base,
            vertical = MaterialTheme.spacing.sm,
        ),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
    ) {
        // ── AI Semantic match card ─────────────────────────────────────────
        state.semanticMatch?.let { match ->
            item(key = "semantic_header") {
                DevOSSectionHeader(
                    title = "AI Semantic Match",
                    modifier = Modifier.padding(vertical = DevOSSpacing.xs),
                )
            }
            item(key = "semantic_match") {
                SemanticMatchCard(match = match)
                Spacer(modifier = Modifier.height(DevOSSpacing.sm))
            }
        }

        // ── Keyword results grouped by type ───────────────────────────────
        state.results.forEach { (type, results) ->
            if (results.isNotEmpty()) {
                item(key = "header_${type.name}") {
                    DevOSSectionHeader(
                        title = type.displayName,
                        modifier = Modifier.padding(vertical = DevOSSpacing.xs),
                    )
                }
                items(
                    items = results,
                    key = { it.id },
                ) { result ->
                    SearchResultItem(
                        result = result,
                        onClick = { onResultTap(result) },
                    )
                }
            }
        }
    }
}

// ── Semantic match card ───────────────────────────────────────────────────────────

@Composable
private fun SemanticMatchCard(
    match: SemanticMatch,
    modifier: Modifier = Modifier,
) {
    DevOSCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            Text(
                text = "AI Semantic Match",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.W600,
                    fontSize = 11.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = match.text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// ── Result item ───────────────────────────────────────────────────────────────────

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DevOSCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${result.title}. ${result.subtitle}" },
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = resultTypeIcon(result.type),
                contentDescription = null,
                modifier = Modifier.size(DevOSSpacing.iconSize),
                tint = when (result.type) {
                    SearchResultType.CODE     -> MaterialTheme.colorScheme.primary
                    SearchResultType.ISSUE    -> MaterialTheme.colorScheme.error
                    SearchResultType.LEARNING -> MaterialTheme.colorScheme.tertiary
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs),
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = when (result.type) {
                        SearchResultType.CODE -> MaterialTheme.colorScheme.primary
                        else                  -> MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = result.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun resultTypeIcon(type: SearchResultType): ImageVector = when (type) {
    SearchResultType.CODE     -> Icons.Outlined.Code
    SearchResultType.ISSUE    -> Icons.Outlined.BugReport
    SearchResultType.LEARNING -> Icons.Outlined.Book
}
