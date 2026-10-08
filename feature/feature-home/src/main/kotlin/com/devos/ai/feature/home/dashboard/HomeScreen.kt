package com.devos.ai.feature.home.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSSearchBar
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.home.dashboard.components.ChatSessionItem
import com.devos.ai.feature.home.dashboard.components.HealthCell
import com.devos.ai.feature.home.dashboard.components.ProjectCard
import com.devos.ai.feature.home.dashboard.components.RecommendationCard
import com.devos.ai.feature.home.model.ProjectHealth

/**
 * Home Dashboard screen — the primary landing screen for DevOS AI.
 *
 * Handles 4 UI states:
 * - [HomeUiState.Loading] → [DevOSLoadingState]
 * - [HomeUiState.Success] → full dashboard content
 * - [HomeUiState.Empty] → [DevOSEmptyState] with import CTA
 * - [HomeUiState.Error] → [DevOSErrorState] with optional retry
 *
 * This composable is stateless — all state and events are supplied by [HomeNavigation].
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchTap: () -> Unit,
    onProjectTap: (String) -> Unit,
    onSessionTap: (String) -> Unit,
    onNotificationTap: () -> Unit,
    onProfileTap: () -> Unit,
    onImportTap: () -> Unit,
    onSeeAllProjectsTap: () -> Unit,
    onSeeAllSessionsTap: () -> Unit,
    onDismissRecommendation: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )

            is HomeUiState.Error -> DevOSErrorState(
                description = state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = if (state.retryable) onRetry else null,
            )

            is HomeUiState.Empty -> DevOSEmptyState(
                icon = Icons.Outlined.RocketLaunch,
                title = "Welcome to DevOS AI",
                description = "Import your first repository to get started",
                modifier = Modifier.padding(innerPadding),
                action = {
                    DevOSButton(
                        text = "Import Repository",
                        onClick = onImportTap,
                    )
                },
            )

            is HomeUiState.Success -> HomeDashboardContent(
                state = state,
                onSearchTap = onSearchTap,
                onProjectTap = onProjectTap,
                onSessionTap = onSessionTap,
                onNotificationTap = onNotificationTap,
                onProfileTap = onProfileTap,
                onSeeAllProjectsTap = onSeeAllProjectsTap,
                onSeeAllSessionsTap = onSeeAllSessionsTap,
                onDismissRecommendation = onDismissRecommendation,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDashboardContent(
    state: HomeUiState.Success,
    onSearchTap: () -> Unit,
    onProjectTap: (String) -> Unit,
    onSessionTap: (String) -> Unit,
    onNotificationTap: () -> Unit,
    onProfileTap: () -> Unit,
    onSeeAllProjectsTap: () -> Unit,
    onSeeAllSessionsTap: () -> Unit,
    onDismissRecommendation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = DevOSSpacing.xl),
    ) {

        // ── Custom top bar ──────────────────────────────────────────────────────
        item(key = "top_bar") {
            HomeTopBar(
                onNotificationTap = onNotificationTap,
                onProfileTap = onProfileTap,
            )
        }

        // ── AI Search bar (tap navigates to search screen) ──────────────────────
        item(key = "search_bar") {
            Box(
                modifier = Modifier
                    .padding(
                        start = DevOSSpacing.base,
                        end = DevOSSpacing.base,
                        top = DevOSSpacing.sm,
                        bottom = DevOSSpacing.md,
                    )
                    .semantics { contentDescription = "Search bar, tap to search" },
            ) {
                DevOSSearchBar(
                    query = "",
                    onQueryChange = {},
                    placeholder = "Ask AI anything about your code…",
                    modifier = Modifier.fillMaxWidth(),
                )
                // Transparent overlay to intercept tap before BasicTextField captures focus
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onSearchTap),
                )
            }
        }

        // ── Recent Projects ─────────────────────────────────────────────────────
        item(key = "projects_header") {
            DevOSSectionHeader(
                title = "Recent Projects",
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
                action = {
                    TextButton(onClick = onSeeAllProjectsTap) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }

        item(key = "projects_row") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = DevOSSpacing.base),
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                items(items = state.recentProjects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onProjectTap(project.id) },
                    )
                }
            }
        }

        // ── AI Recommendations ──────────────────────────────────────────────────
        item(key = "recommendations_header") {
            DevOSSectionHeader(
                title = "AI Recommendations",
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
                action = {
                    Text(
                        text = "${state.recommendations.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        items(items = state.recommendations, key = { it.id }) { recommendation ->
            RecommendationCard(
                recommendation = recommendation,
                onDismiss = onDismissRecommendation,
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.xs,
                ),
            )
        }

        // ── Project Health ──────────────────────────────────────────────────────
        item(key = "health_header") {
            DevOSSectionHeader(
                title = "Project Health",
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
            )
        }

        item(key = "health_grid") {
            ProjectHealthGrid(
                health = state.health,
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
            )
        }

        // ── Recent AI Sessions ──────────────────────────────────────────────────
        item(key = "sessions_header") {
            DevOSSectionHeader(
                title = "Recent AI Sessions",
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.sm,
                ),
                action = {
                    TextButton(onClick = onSeeAllSessionsTap) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }

        itemsIndexed(
            items = state.recentSessions,
            key = { _, session -> session.id },
        ) { index, session ->
            ChatSessionItem(
                title = session.title,
                relativeTime = session.relativeTime,
                projectName = session.projectName,
                onClick = { onSessionTap(session.id) },
                showDivider = index < state.recentSessions.lastIndex,
            )
        }

        // Bottom spacing
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(DevOSSpacing.base))
        }
    }
}

/**
 * Custom top bar matching the #s-home mockup:
 * - Date label (bodySmall, onSurfaceVariant)
 * - Greeting "Good morning, Dev 👋" (titleLarge, bold)
 * - Notification bell with red badge dot
 * - Avatar circle with "D" letter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onNotificationTap: () -> Unit,
    onProfileTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MONDAY, JUNE 9",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(DevOSSpacing.xs))
            Text(
                text = "Good morning, Dev \uD83D\uDC4B",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Notification bell with red badge
        BadgedBox(
            badge = {
                Badge(containerColor = MaterialTheme.colorScheme.error)
            },
        ) {
            IconButton(
                onClick = onNotificationTap,
                modifier = Modifier.size(DevOSSpacing.touchTarget),
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.width(DevOSSpacing.xs))

        // Avatar — wrapped in 48dp touch target (WCAG minimum)
        Box(
            modifier = Modifier
                .size(DevOSSpacing.touchTarget)             // 48dp touch target
                .clickable(onClick = onProfileTap)
                .semantics { contentDescription = "Profile" },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(DevOSSpacing.iconSizeLarge)       // 32dp visual circle
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "D",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * 2×2 health grid matching the #s-home mockup health section.
 *
 * Uses a fixed Column+Row layout to avoid nested scrollable containers
 * (LazyVerticalGrid inside LazyColumn is not supported by Compose).
 */
@Composable
private fun ProjectHealthGrid(
    health: ProjectHealth,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        ) {
            HealthCell(
                label = "Security",
                value = health.securityCount.toString(),
                subLabel = health.securityLabel,
                status = com.devos.ai.feature.home.model.HealthStatus.CRITICAL,
                modifier = Modifier.weight(1f),
            )
            HealthCell(
                label = "Test Coverage",
                value = "${health.testCoverage}%",
                subLabel = if (health.testCoverage < 60) "Below target (80%)" else "Coverage",
                status = if (health.testCoverage < 60) {
                    com.devos.ai.feature.home.model.HealthStatus.WARNING
                } else {
                    com.devos.ai.feature.home.model.HealthStatus.HEALTHY
                },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        ) {
            HealthCell(
                label = "Architecture",
                value = health.architectureGrade,
                subLabel = "Grade",
                status = com.devos.ai.feature.home.model.HealthStatus.HEALTHY,
                modifier = Modifier.weight(1f),
            )
            HealthCell(
                label = "Dependencies",
                value = health.dependencyUpdates.toString(),
                subLabel = "Updates available",
                status = com.devos.ai.feature.home.model.HealthStatus.WARNING,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
