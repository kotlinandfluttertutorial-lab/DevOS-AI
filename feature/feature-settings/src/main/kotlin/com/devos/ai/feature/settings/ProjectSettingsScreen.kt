package com.devos.ai.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing

/**
 * Project Settings screen (DEVOS-063 / DA-75).
 *
 * Stateless composable — all state comes from [ProjectSettingsUiState],
 * all events go up via callback parameters.
 *
 * Matches mockup #s-project-settings.
 */
@Composable
fun ProjectSettingsScreen(
    uiState: ProjectSettingsUiState,
    onNavigateBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateAutoInclude: (Boolean) -> Unit,
    onShowDeleteConfirmation: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DevOSTopBar(
                title = "Project Settings",
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {

            // ── PROJECT NAME & DESCRIPTION ────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.base),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    // PROJECT NAME label
                    Text(
                        text = "PROJECT NAME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.W700,
                            letterSpacing = 0.5.sp,
                            fontSize = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = onUpdateName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Project name input" },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.W500,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    // DESCRIPTION label
                    Text(
                        text = "DESCRIPTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.W700,
                            letterSpacing = 0.5.sp,
                            fontSize = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = onUpdateDescription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .semantics { contentDescription = "Project description input" },
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = DevOSSpacing.dividerThickness,
                )
            }

            // ── REPOSITORY SECTION ────────────────────────────────────────────
            item {
                SettingsSectionHeader(title = "REPOSITORY")
                SettingsNavRow(
                    icon = Icons.Outlined.AccountTree,
                    iconContentDescription = "GitHub Repository",
                    label = "GitHub Repository",
                    subtitle = uiState.githubRepo,
                    onClick = {},
                )
                SettingsNavRow(
                    icon = Icons.Outlined.FolderOpen,
                    iconContentDescription = "Default Branch",
                    label = "Default Branch",
                    subtitle = uiState.branch,
                    onClick = {},
                )
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = DevOSSpacing.dividerThickness,
                )
            }

            // ── AI CONTEXT SECTION ────────────────────────────────────────────
            item {
                SettingsSectionHeader(title = "AI CONTEXT")
                SettingsToggleRow(
                    icon = Icons.Outlined.FolderOpen,
                    iconContentDescription = "Auto-include in AI context",
                    label = "Auto-include in AI context",
                    subtitle = "Load repo summary for every AI session",
                    checked = uiState.autoInclude,
                    onCheckedChange = onUpdateAutoInclude,
                )
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = DevOSSpacing.dividerThickness,
                )
            }

            // ── DANGER ZONE SECTION ───────────────────────────────────────────
            item {
                // Section header in error color
                Text(
                    text = "DANGER ZONE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.5.sp,
                        fontSize = 12.sp,
                    ),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.base,
                        vertical = MaterialTheme.spacing.sm,
                    ),
                )
                Button(
                    onClick = onShowDeleteConfirmation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.base)
                        .height(46.dp)
                        .semantics { contentDescription = "Delete project" },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(
                        text = "Delete Project",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.W600,
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = {
                Text(
                    text = "Delete Project",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${uiState.name}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}
