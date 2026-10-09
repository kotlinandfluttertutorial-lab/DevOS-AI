package com.devos.ai.feature.repository.list

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSHealthIndicator
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSAmber300
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.repository.model.HealthStatus
import com.devos.ai.feature.repository.model.ProjectSummary
import com.devos.ai.feature.repository.model.SortOrder
import com.devos.ai.feature.repository.model.SyncStatus
import com.devos.ai.feature.repository.model.stubLanguageTags

/**
 * Project / Repository List screen — stateless composable matching the `#s-project-list` mockup.
 *
 * Layout: [DevOSTopBar] (no back arrow — primary tab) + search bar + filter/sort chips +
 * [LazyColumn] of [ProjectCard]s + FAB (import).
 *
 * Bottom navigation is NOT rendered here — [com.devos.ai.MainActivity]'s Scaffold owns it.
 * All state and callbacks are supplied by the navigation function.
 */
@Composable
fun ProjectListScreen(
    uiState: ProjectListUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterLanguage: (String?) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onProjectTap: (String) -> Unit,
    onImportTap: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Projects",
                actions = {
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(DevOSSpacing.touchTarget)
                            .semantics { contentDescription = "Search projects" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = onImportTap,
                        modifier = Modifier
                            .size(DevOSSpacing.touchTarget)
                            .semantics { contentDescription = "Import repository" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onImportTap,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.semantics { contentDescription = "Import repository" },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(DevOSSpacing.iconSize),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val s = uiState) {
                is ProjectListUiState.Loading -> DevOSLoadingState()

                is ProjectListUiState.Empty -> DevOSEmptyState(
                    icon = Icons.Outlined.FolderOff,
                    title = "No repositories yet",
                    description = "Import a GitHub or GitLab repository to get started",
                    action = {
                        DevOSButton(
                            text = "Import Repository",
                            onClick = onImportTap,
                        )
                    },
                )

                is ProjectListUiState.Error -> DevOSErrorState(
                    description = s.message,
                    onRetry = if (s.retryable) onRetry else null,
                )

                is ProjectListUiState.Success -> ProjectListContent(
                    state = s,
                    onSearchQueryChange = onSearchQueryChange,
                    onFilterLanguage = onFilterLanguage,
                    onSortChange = onSortChange,
                    onProjectTap = onProjectTap,
                )
            }
        }
    }
}

// ── Success content ──────────────────────────────────────────────────────────

@Composable
private fun ProjectListContent(
    state: ProjectListUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onFilterLanguage: (String?) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onProjectTap: (String) -> Unit,
) {
    val allLanguages = stubLanguageTags()
    val totalCount = state.repositories.size

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ── Search bar ───────────────────────────────────────────────────────
        item(key = "search_bar") {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
                placeholder = {
                    Text(
                        text = "Search projects…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                    )
                },
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = {}),
            )
        }

        // ── Filter chips (language) ──────────────────────────────────────────
        item(key = "filter_chips") {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                // "All" chip
                item(key = "chip_all") {
                    FilterChip(
                        selected = state.filterLanguage == null,
                        onClick = { onFilterLanguage(null) },
                        label = { Text("All ($totalCount)", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.semantics { contentDescription = "All projects filter" },
                    )
                }
                items(allLanguages, key = { "chip_$it" }) { language ->
                    FilterChip(
                        selected = state.filterLanguage == language,
                        onClick = { onFilterLanguage(language) },
                        label = { Text(language, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.semantics { contentDescription = "$language filter" },
                    )
                }
            }
        }

        // ── Sort row ─────────────────────────────────────────────────────────
        item(key = "sort_row") {
            SortRow(
                sortOrder = state.sortOrder,
                onSortChange = onSortChange,
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.xs,
                ),
            )
        }

        // ── Project cards ────────────────────────────────────────────────────
        items(state.repositories, key = { it.id }) { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectTap(project.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.xs),
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(DevOSSpacing.xxl))
        }
    }
}

// ── Sort row ──────────────────────────────────────────────────────────────────

@Composable
private fun SortRow(
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Sort:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SortOrder.entries.forEach { order ->
            FilterChip(
                selected = sortOrder == order,
                onClick = { onSortChange(order) },
                label = { Text(order.label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.semantics { contentDescription = "Sort by ${order.label}" },
            )
        }
    }
}

// ── Project card ──────────────────────────────────────────────────────────────

/**
 * Project card matching the `#s-project-list` mockup.
 *
 * Layout:
 * - Row: 48dp icon box + Column(name + URL + language chips)
 * - Divider
 * - Footer row: sync time + warnings count + PR count
 */
@Composable
private fun ProjectCard(
    project: ProjectSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${project.name} project card"
                role = Role.Button
            },
    ) {
        Column(modifier = Modifier.padding(DevOSSpacing.base)) {
            // ── Top row: icon + info + health badge ───────────────────────────
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.md),
            ) {
                // Project icon box
                Box(
                    modifier = Modifier
                        .size(DevOSSpacing.iconSizeXLarge)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    // Health ring fills the icon box
                    DevOSHealthIndicator(
                        percentage = project.healthScore,
                        color = healthColor(project.healthStatus),
                        modifier = Modifier.size(DevOSSpacing.iconSizeXLarge),
                    )
                }

                // Name + URL + tags + health badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(DevOSSpacing.sm))
                        HealthBadge(project.healthStatus, project.warningCount)
                    }

                    Spacer(modifier = Modifier.height(DevOSSpacing.xxs))

                    Text(
                        text = project.fullPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(DevOSSpacing.sm))

                    // Language chips
                    if (project.languageTags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
                        ) {
                            items(project.languageTags, key = { it }) { tag ->
                                LanguageChip(tag)
                            }
                        }
                    }
                }
            }

            // ── Footer divider + sync info + counts ───────────────────────────
            Spacer(modifier = Modifier.height(DevOSSpacing.md))
            HorizontalDivider(
                thickness = DevOSSpacing.dividerThickness,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val syncLabel = when (project.syncStatus) {
                    SyncStatus.SYNCED  -> "Synced ${project.lastSync}"
                    SyncStatus.SYNCING -> "Syncing…"
                    SyncStatus.ERROR   -> "Sync error"
                    SyncStatus.IDLE    -> "Not synced"
                }
                Text(
                    text = syncLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.md)) {
                    if (project.warningCount > 0) {
                        Text(
                            text = "⚠ ${project.warningCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (project.healthStatus) {
                                HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error
                                HealthStatus.WARNING  -> DevOSAmber300
                                HealthStatus.HEALTHY  -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (project.prCount > 0) {
                        Text(
                            text = "${project.prCount} PRs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

@Composable
private fun HealthBadge(status: HealthStatus, warningCount: Int) {
    val (badgeStatus, label) = when (status) {
        HealthStatus.HEALTHY  -> Pair(DevOSBadgeStatus.SUCCESS, "Healthy")
        HealthStatus.WARNING  -> Pair(DevOSBadgeStatus.WARNING, "$warningCount Warnings")
        HealthStatus.CRITICAL -> Pair(DevOSBadgeStatus.ERROR, "Critical")
    }
    DevOSStatusBadge(status = badgeStatus, label = label)
}

@Composable
private fun LanguageChip(tag: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .border(
                width = DevOSSpacing.dividerThickness,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = DevOSSpacing.sm, vertical = DevOSSpacing.xxs),
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun healthColor(status: HealthStatus) = when (status) {
    HealthStatus.HEALTHY  -> MaterialTheme.colorScheme.tertiary
    HealthStatus.WARNING  -> DevOSAmber300
    HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error
}
