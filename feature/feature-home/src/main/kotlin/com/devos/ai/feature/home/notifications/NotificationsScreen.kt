package com.devos.ai.feature.home.notifications

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing

/**
 * Notifications screen — DEVOS-059 / FIGMA-37.
 *
 * Stateless composable. All state comes from [NotificationsUiState]; all events flow
 * up via callback parameters. No ViewModel reference flows through the tree.
 */
@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onFilterChange: (NotificationType?) -> Unit,
    onNotificationTap: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DevOSTopBar(
                title = "Notifications",
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onMarkAllRead) {
                        Text(
                            text = "Mark all read",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Filter chips ─────────────────────────────────────────────────
            val activeFilter = if (uiState is NotificationsUiState.Success) uiState.activeFilter else null
            NotificationFilterRow(
                activeFilter = activeFilter,
                onFilterChange = onFilterChange,
            )

            Spacer(modifier = Modifier.height(DevOSSpacing.xs))

            // ── Content ──────────────────────────────────────────────────────
            when (val state = uiState) {
                is NotificationsUiState.Loading -> DevOSLoadingState()

                is NotificationsUiState.Empty -> DevOSEmptyState(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "All caught up",
                    description = "No notifications right now",
                )

                is NotificationsUiState.Error -> DevOSErrorState(
                    description = state.message,
                    onRetry = if (state.retryable) onRetry else null,
                )

                is NotificationsUiState.Success -> NotificationsList(
                    notifications = state.filteredNotifications,
                    onNotificationTap = onNotificationTap,
                )
            }
        }
    }
}

// ── Filter chips row ─────────────────────────────────────────────────────────────

@Composable
private fun NotificationFilterRow(
    activeFilter: NotificationType?,
    onFilterChange: (NotificationType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.base),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        item(key = "all") {
            FilterChip(
                selected = activeFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
        items(
            items = NotificationType.entries,
            key = { it.name },
        ) { type ->
            FilterChip(
                selected = activeFilter == type,
                onClick = { onFilterChange(type) },
                label = { Text(type.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

// ── Notifications list ────────────────────────────────────────────────────────────

@Composable
private fun NotificationsList(
    notifications: List<DevOSNotification>,
    onNotificationTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (notifications.isEmpty()) {
        DevOSEmptyState(
            icon = Icons.Outlined.NotificationsNone,
            title = "No notifications",
            description = "Nothing in this category right now",
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xs),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = MaterialTheme.spacing.base,
                vertical = MaterialTheme.spacing.sm,
            ),
        ) {
            items(
                items = notifications,
                key = { it.id },
            ) { notification ->
                NotificationCard(
                    notification = notification,
                    onClick = { onNotificationTap(notification.id) },
                )
            }
        }
    }
}

// ── Notification card ─────────────────────────────────────────────────────────────

@Composable
private fun NotificationCard(
    notification: DevOSNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DevOSCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (notification.isRead) 0.65f else 1f)
            .semantics {
                contentDescription = "${notification.title}. ${notification.subtitle}. ${notification.relativeTime}"
            },
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            // Unread dot or spacer
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = DevOSSpacing.xs)
                        .size(DevOSSpacing.sm)
                        .background(
                            color = notificationDotColor(notification.type),
                            shape = CircleShape,
                        ),
                )
            } else {
                Spacer(modifier = Modifier.width(DevOSSpacing.sm))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (!notification.isRead) FontWeight.W600 else FontWeight.W400,
                            fontSize = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(DevOSSpacing.sm))
                    Text(
                        text = notification.relativeTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = notification.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun notificationDotColor(type: NotificationType): Color = when (type) {
    NotificationType.SECURITY -> MaterialTheme.colorScheme.error
    NotificationType.AI       -> MaterialTheme.colorScheme.primary
    NotificationType.LEARNING -> MaterialTheme.colorScheme.tertiary
    NotificationType.PR       -> MaterialTheme.colorScheme.secondary
    NotificationType.TESTING  -> MaterialTheme.colorScheme.tertiary
}
