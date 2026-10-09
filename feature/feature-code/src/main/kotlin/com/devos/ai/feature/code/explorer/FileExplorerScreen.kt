package com.devos.ai.feature.code.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.code.model.FileItem
import com.devos.ai.feature.code.model.FileItemType

/**
 * File Explorer screen — DEVOS-018 / FIGMA-10.
 *
 * Shows a navigable file tree with breadcrumb, type filter chips, and
 * file/folder rows with AI action button on Kotlin files.
 */
@Composable
fun FileExplorerScreen(
    uiState: FileExplorerUiState,
    onFilterChange: (FileTypeFilter) -> Unit,
    onFolderTap: (FileItem) -> Unit,
    onFileTap: (FileItem) -> Unit,
    onBreadcrumbTap: (Int) -> Unit,
    onSearchTap: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                DevOSTopBar(
                    title = "File Explorer",
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
                    actions = {
                        IconButton(
                            onClick = onSearchTap,
                            modifier = Modifier
                                .size(DevOSSpacing.touchTarget)
                                .semantics { contentDescription = "Search files" },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )

                // Breadcrumb row
                if (uiState is FileExplorerUiState.Success) {
                    BreadcrumbRow(
                        breadcrumb = uiState.breadcrumb,
                        onSegmentTap = onBreadcrumbTap,
                    )
                    Spacer(modifier = Modifier.height(DevOSSpacing.xs))
                    // Filter chips
                    FilterChipsRow(
                        activeFilter = uiState.activeFilter,
                        onFilterChange = onFilterChange,
                    )
                }
            }
        },
    ) { innerPadding ->
        when (val s = uiState) {
            is FileExplorerUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )
            is FileExplorerUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.Folder,
                title = "No files found",
                description = "This directory is empty",
                modifier = Modifier.padding(innerPadding),
            )
            is FileExplorerUiState.Error -> DevOSErrorState(
                description = s.message,
                onRetry = if (s.retryable) ({}) else null,
                modifier = Modifier.padding(innerPadding),
            )
            is FileExplorerUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    itemsIndexed(s.items, key = { _, item -> item.id }) { index, item ->
                        FileItemRow(
                            item = item,
                            onFolderTap = { onFolderTap(item) },
                            onFileTap = { onFileTap(item) },
                        )
                        if (index < s.items.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                thickness = DevOSSpacing.dividerThickness,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Breadcrumb ────────────────────────────────────────────────────────────────

@Composable
private fun BreadcrumbRow(
    breadcrumb: List<String>,
    onSegmentTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.padding(horizontal = DevOSSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs),
    ) {
        itemsIndexed(breadcrumb) { index, segment ->
            val isLast = index == breadcrumb.lastIndex
            Text(
                text = segment,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isLast)
                    MaterialTheme.colorScheme.onBackground
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(enabled = !isLast) { onSegmentTap(index) }
                    .semantics {
                        contentDescription = "Navigate to $segment"
                        role = Role.Button
                    },
            )
            if (!isLast) {
                Text(
                    text = "/",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Filter chips ──────────────────────────────────────────────────────────────

@Composable
private fun FilterChipsRow(
    activeFilter: FileTypeFilter,
    onFilterChange: (FileTypeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = DevOSSpacing.base),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
    ) {
        FileTypeFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == activeFilter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

// ── File item row ─────────────────────────────────────────────────────────────

@Composable
private fun FileItemRow(
    item: FileItem,
    onFolderTap: () -> Unit,
    onFileTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = if (item.type == FileItemType.FOLDER) onFolderTap else onFileTap,
            )
            .padding(
                horizontal = DevOSSpacing.base,
                vertical = DevOSSpacing.sm,
            )
            .semantics {
                contentDescription = "${item.name}, ${if (item.type == FileItemType.FOLDER) "folder" else "file"}"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        // File type icon badge
        FileTypeBadge(type = item.type)

        // Name + meta
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.type == FileItemType.FOLDER && item.fileCount != null) {
                Text(
                    text = "${item.fileCount} files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (item.sizeLabel.isNotBlank()) {
                Text(
                    text = "${item.sizeLabel} · ${item.lastModified}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // AI button on Kotlin files
        if (item.type == FileItemType.KOTLIN) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onFileTap)
                    .semantics { contentDescription = "AI actions for ${item.name}" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                )
            }
            Spacer(modifier = Modifier.width(DevOSSpacing.xs))
        }

        // Chevron for folders
        if (item.type == FileItemType.FOLDER) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.NavigateNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DevOSSpacing.iconSize),
            )
        }
    }
}

@Composable
private fun FileTypeBadge(
    type: FileItemType,
    modifier: Modifier = Modifier,
) {
    val (bgColor, label, textColor) = when (type) {
        FileItemType.FOLDER -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            "📁",
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        FileItemType.KOTLIN -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "kt",
            MaterialTheme.colorScheme.primary,
        )
        FileItemType.XML -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "xml",
            MaterialTheme.colorScheme.tertiary,
        )
        FileItemType.GRADLE -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            "gr",
            MaterialTheme.colorScheme.secondary,
        )
        FileItemType.OTHER -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "?",
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Box(
        modifier = modifier
            .size(DevOSSpacing.iconSizeLarge)
            .clip(MaterialTheme.shapes.small)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}
