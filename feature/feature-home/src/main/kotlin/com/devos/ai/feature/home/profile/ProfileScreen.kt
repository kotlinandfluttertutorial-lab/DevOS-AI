package com.devos.ai.feature.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing
import androidx.compose.foundation.clickable

/**
 * Profile screen — DEVOS-061 / FIGMA-38.
 *
 * Stateless composable. All state comes from [ProfileUiState]; all events go up
 * via callback parameters.
 */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            DevOSTopBar(
                title = "Profile",
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
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
                        onClick = { /* TODO: edit profile */ },
                        modifier = Modifier.semantics { contentDescription = "Edit profile" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { DevOSLoadingState() }

            is ProfileUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                DevOSErrorState(
                    description = state.message,
                    onRetry = if (state.retryable) onRetry else null,
                )
            }

            is ProfileUiState.Empty -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                DevOSEmptyState(
                    icon = Icons.Outlined.Edit,
                    title = "No profile yet",
                    description = "Sign in to see your profile",
                )
            }

            is ProfileUiState.Success -> ProfileContent(
                profile = state.profile,
                onSignOutRequest = { showSignOutDialog = true },
                modifier = Modifier.padding(innerPadding),
            )
        }

        // ── Sign-out confirmation dialog ──────────────────────────────────
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign out of DevOS AI?") },
                text = { Text("You will need to sign in again to access your projects and AI features.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSignOutDialog = false
                            onSignOut()
                        },
                    ) {
                        Text(
                            text = "Sign out",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

// ── Success content ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileContent(
    profile: UserProfile,
    onSignOutRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = DevOSSpacing.xxl,
        ),
    ) {
        // ── Avatar section ────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(DevOSSpacing.xl))
            AvatarSection(profile = profile)
        }

        // ── Stats row ─────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(DevOSSpacing.xl))
            StatsRow(
                aiSessions = profile.aiSessions,
                streak = profile.streak,
                projects = profile.projects,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base),
            )
        }

        // ── Settings rows ─────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(DevOSSpacing.xl))
            ProfileSettingsSection(
                profile = profile,
            )
        }

        // ── Sign out ──────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(DevOSSpacing.xl))
            OutlinedButton(
                onClick = onSignOutRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DevOSSpacing.touchTarget)
                    .padding(horizontal = MaterialTheme.spacing.base)
                    .semantics { contentDescription = "Sign out" },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = DevOSSpacing.xxs,
                    color = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                )
                Spacer(modifier = Modifier.width(DevOSSpacing.sm))
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── Avatar section ────────────────────────────────────────────────────────────────

@Composable
private fun AvatarSection(
    profile: UserProfile,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        // 80dp gradient circle with initials
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF82AAFF), // primary-like
                            Color(0xFFC792EA), // secondary-like
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = profile.name.take(1).uppercase(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.W700,
                    fontSize = 32.sp,
                ),
                color = Color.White,
            )
        }

        Text(
            text = profile.name,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.W700,
                fontSize = 20.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = profile.email,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Connected account badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        ) {
            if (profile.githubHandle != null) {
                DevOSStatusBadge(status = DevOSBadgeStatus.INFO, label = "GitHub")
            }
            if (profile.gitlabHandle != null) {
                DevOSStatusBadge(status = DevOSBadgeStatus.INFO, label = "GitLab")
            }
        }
    }
}

// ── Stats row ─────────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(
    aiSessions: Int,
    streak: Int,
    projects: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = DevOSSpacing.xxs,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium,
            )
            .height(IntrinsicSize.Min),
    ) {
        StatCell(
            value = aiSessions.toString(),
            label = "AI Sessions",
            valueColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(DevOSSpacing.xxs),
            color = MaterialTheme.colorScheme.outline,
        )
        StatCell(
            value = streak.toString(),
            label = "Day Streak",
            valueColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(DevOSSpacing.xxs),
            color = MaterialTheme.colorScheme.outline,
        )
        StatCell(
            value = projects.toString(),
            label = "Projects",
            valueColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = DevOSSpacing.base),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.W700,
                fontSize = 20.sp,
            ),
            color = valueColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Settings section ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileSettingsSection(
    profile: UserProfile,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Plan row
        ProfileNavRow(
            icon = Icons.Outlined.EmojiEvents,
            iconDescription = "Plan",
            label = "Plan",
            subtitle = profile.planRenewal,
            trailingContent = {
                DevOSStatusBadge(status = DevOSBadgeStatus.INFO, label = profile.plan.displayLabel)
            },
            onClick = { /* TODO: navigate to plan management */ },
        )
        ProfileNavRow(
            icon = Icons.Outlined.Link,
            iconDescription = "Connected Accounts",
            label = "Connected Accounts",
            subtitle = buildConnectedAccounts(profile),
            onClick = { /* TODO: navigate to accounts */ },
        )
        ProfileNavRow(
            icon = Icons.Outlined.BarChart,
            iconDescription = "Usage Stats",
            label = "Usage Stats",
            subtitle = "${profile.aiSessions} total AI sessions",
            onClick = { /* TODO: navigate to usage stats */ },
        )
    }
}

private fun buildConnectedAccounts(profile: UserProfile): String {
    val accounts = mutableListOf<String>()
    if (profile.githubHandle != null) accounts.add("GitHub @${profile.githubHandle}")
    if (profile.gitlabHandle != null) accounts.add("GitLab @${profile.gitlabHandle}")
    return if (accounts.isEmpty()) "No accounts connected" else accounts.joinToString(", ")
}

@Composable
private fun ProfileNavRow(
    icon: ImageVector,
    iconDescription: String,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DevOSSpacing.touchTarget)
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = MaterialTheme.spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconDescription,
            modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
