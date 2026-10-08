package com.devos.ai.feature.repository.importing

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.repository.model.RepositoryPreview
import com.devos.ai.feature.repository.model.RepositoryProvider

/**
 * Repository Import screen — stateless composable matching the #s-repo-import mockup.
 *
 * Layout: DevOSTopBar + scrollable content with source selector, URL input,
 * optional preview card, import options, and import button.
 *
 * All state and callbacks are supplied by [RepositoryNavigation].
 */
@Composable
fun RepositoryImportScreen(
    uiState: ImportUiState,
    url: String,
    selectedProvider: RepositoryProvider,
    branch: String,
    buildAiIndex: Boolean,
    onUrlChange: (String) -> Unit,
    onProviderSelect: (RepositoryProvider) -> Unit,
    onValidate: () -> Unit,
    onBranchChange: (String) -> Unit,
    onBuildAiIndexToggle: (Boolean) -> Unit,
    onImport: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Import Repository",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── SELECT SOURCE section ──────────────────────────────────────────
            Text(
                text = "SELECT SOURCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                RepositoryProvider.entries.forEach { provider ->
                    SourceSelectorCard(
                        provider = provider,
                        isSelected = provider == selectedProvider,
                        onClick = { onProviderSelect(provider) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(DevOSSpacing.base))

            // ── REPOSITORY URL section ─────────────────────────────────────────
            Text(
                text = "REPOSITORY URL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
                placeholder = {
                    Text(
                        text = "https://github.com/owner/repo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(DevOSSpacing.sm))

            // ── Validate button (visible when idle or error and URL not blank) ─
            AnimatedVisibility(
                visible = url.isNotBlank() &&
                    (uiState is ImportUiState.Idle || uiState is ImportUiState.Error),
            ) {
                TextButton(
                    onClick = onValidate,
                    modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                ) {
                    Text(
                        text = "Validate URL",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // ── Validating progress indicator ──────────────────────────────────
            AnimatedVisibility(visible = uiState is ImportUiState.Validating) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DevOSSpacing.base),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // ── Error message ──────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState is ImportUiState.Error) {
                if (uiState is ImportUiState.Error) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(
                            horizontal = DevOSSpacing.base,
                            vertical = DevOSSpacing.xs,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(DevOSSpacing.sm))

            // ── Repository preview card ────────────────────────────────────────
            AnimatedVisibility(visible = uiState is ImportUiState.Preview) {
                if (uiState is ImportUiState.Preview) {
                    RepositoryPreviewCard(
                        preview = uiState.preview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DevOSSpacing.base),
                    )
                }
            }

            Spacer(modifier = Modifier.height(DevOSSpacing.base))

            // ── IMPORT OPTIONS section ─────────────────────────────────────────
            Text(
                text = "IMPORT OPTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
            )

            BranchRow(
                branch = branch,
                onChangeTap = { onBranchChange(branch) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
            )

            BuildAiIndexRow(
                enabled = buildAiIndex,
                onToggle = onBuildAiIndexToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
            )

            Spacer(modifier = Modifier.height(DevOSSpacing.xl))

            // ── Import button ──────────────────────────────────────────────────
            DevOSButton(
                text = "Import Repository",
                onClick = onImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DevOSSpacing.base),
                enabled = uiState is ImportUiState.Preview || uiState is ImportUiState.Success,
            )

            Spacer(modifier = Modifier.height(DevOSSpacing.base))
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

/**
 * A single source provider card (GitHub / GitLab / Local).
 *
 * Selected state: primaryContainer background + primary border (1.5dp).
 * Unselected state: surface background + outline border (1dp).
 */
@Composable
private fun SourceSelectorCard(
    provider: RepositoryProvider,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val borderWidth = if (isSelected) DevOSSpacing.xxs else DevOSSpacing.dividerThickness
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val providerLabel = when (provider) {
        RepositoryProvider.GITHUB -> "GitHub"
        RepositoryProvider.GITLAB -> "GitLab"
        RepositoryProvider.LOCAL -> "Local"
    }
    val providerIcon = when (provider) {
        RepositoryProvider.GITHUB -> Icons.Outlined.Cloud
        RepositoryProvider.GITLAB -> Icons.Outlined.Code
        RepositoryProvider.LOCAL -> Icons.Outlined.Storage
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(DevOSSpacing.md)
            .semantics {
                contentDescription = "$providerLabel source selector${if (isSelected) ", selected" else ""}"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        ) {
            Icon(
                imageVector = providerIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
            )
            Text(
                text = providerLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
        }
    }
}

/**
 * Preview card shown after successful URL validation.
 *
 * Shows repo name, owner/contributors, a "Valid" badge, and stat cells.
 */
@Composable
private fun RepositoryPreviewCard(
    preview: RepositoryPreview,
    modifier: Modifier = Modifier,
) {
    DevOSCard(modifier = modifier) {
        Column(modifier = Modifier.padding(DevOSSpacing.base)) {
            // Header row: icon + name/owner + valid badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(DevOSSpacing.iconSizeXLarge)
                        .clip(MaterialTheme.shapes.small)
                        .border(
                            width = DevOSSpacing.dividerThickness,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preview.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${preview.owner} / 24 contributors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DevOSStatusBadge(
                    status = DevOSBadgeStatus.SUCCESS,
                    label = "Valid",
                )
            }

            Spacer(modifier = Modifier.height(DevOSSpacing.sm))

            // Stats row: STARS / FILES / SIZE / BRANCH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.base),
            ) {
                StatCell(label = "STARS", value = formatStars(preview.stars), modifier = Modifier.weight(1f))
                StatCell(label = "FILES", value = preview.fileCount.toString(), modifier = Modifier.weight(1f))
                StatCell(label = "SIZE", value = preview.size, modifier = Modifier.weight(1f))
                StatCell(label = "BRANCH", value = preview.branch, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Branch selector row — shows current branch with a "Change" text button.
 */
@Composable
private fun BranchRow(
    branch: String,
    onChangeTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(DevOSSpacing.touchTarget)
            .padding(vertical = DevOSSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountTree,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Branch",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = branch,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onChangeTap,
            modifier = Modifier.semantics { contentDescription = "Change branch" },
        ) {
            Text(
                text = "Change",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Build AI Index toggle row.
 */
@Composable
private fun BuildAiIndexRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(DevOSSpacing.touchTarget)
            .padding(vertical = DevOSSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.Psychology,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Build AI Index",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Symbol table + RAG embeddings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics {
                contentDescription = if (enabled) "Build AI index enabled" else "Build AI index disabled"
            },
        )
    }
}

// ── Formatting helpers ───────────────────────────────────────────────────────

private fun formatStars(stars: Int): String = when {
    stars >= 1_000 -> "${stars / 1_000}.${(stars % 1_000) / 100}k"
    else -> stars.toString()
}
