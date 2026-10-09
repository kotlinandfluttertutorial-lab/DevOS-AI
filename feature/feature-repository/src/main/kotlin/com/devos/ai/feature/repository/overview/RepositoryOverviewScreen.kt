package com.devos.ai.feature.repository.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSSectionHeader
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSAmber300
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.feature.repository.model.CommitSummary
import com.devos.ai.feature.repository.model.LanguageBarColor
import com.devos.ai.feature.repository.model.OverviewTab
import com.devos.ai.feature.repository.model.RepoOverview

/**
 * Repository Overview screen — stateless composable matching the `#s-repo-overview` mockup.
 *
 * Layout: [DevOSTopBar] + repo-info panel + [ScrollableTabRow] (7 tabs) +
 * [HorizontalPager] (Overview tab = real content; tabs 1–6 = "Coming soon" placeholders).
 *
 * All state and callbacks are supplied by [com.devos.ai.feature.repository.navigation.RepositoryNavigation].
 * Bottom navigation is NOT rendered here — [com.devos.ai.MainActivity]'s Scaffold owns it.
 */
@Composable
fun RepositoryOverviewScreen(
    uiState: OverviewUiState,
    onTabSelect: (OverviewTab) -> Unit,
    onNavigateBack: () -> Unit,
    onGitTap: () -> Unit,
    onAITap: () -> Unit,
    onFilesTap: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Resolve top-bar title: repo name when loaded, or "Repository" fallback.
    val title = (uiState as? OverviewUiState.Success)?.overview?.name ?: "Repository"

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = title,
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
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(DevOSSpacing.touchTarget)
                            .semantics { contentDescription = "Refresh" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(DevOSSpacing.touchTarget)
                            .semantics { contentDescription = "More options" },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = uiState) {
                is OverviewUiState.Loading -> DevOSLoadingState()
                is OverviewUiState.Error   -> DevOSErrorState(
                    description = s.message,
                    onRetry = if (s.retryable) onRetry else null,
                )
                is OverviewUiState.Success -> OverviewContent(
                    overview = s.overview,
                    selectedTab = s.selectedTab,
                    onTabSelect = onTabSelect,
                    onGitTap = onGitTap,
                    onAITap = onAITap,
                    onFilesTap = onFilesTap,
                )
            }
        }
    }
}

// ── Overview content (Success state) ────────────────────────────────────────

@Composable
private fun OverviewContent(
    overview: RepoOverview,
    selectedTab: OverviewTab,
    onTabSelect: (OverviewTab) -> Unit,
    onGitTap: () -> Unit,
    onAITap: () -> Unit,
    onFilesTap: () -> Unit,
) {
    val pagerState = rememberPagerState { OverviewTab.entries.size }

    // Sync ViewModel → pager
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) {
            pagerState.animateScrollToPage(selectedTab.ordinal)
        }
    }

    // Sync pager swipe → ViewModel
    LaunchedEffect(pagerState.currentPage) {
        val swipedTab = OverviewTab.entries[pagerState.currentPage]
        if (swipedTab != selectedTab) {
            onTabSelect(swipedTab)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Repo info panel ──────────────────────────────────────────────────
        RepoInfoPanel(overview = overview)

        // ── Scrollable tab row (7 tabs) ──────────────────────────────────────
        OverviewTabRow(selectedTab = selectedTab, onTabSelect = onTabSelect)

        // ── Pager body ───────────────────────────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (OverviewTab.entries[page]) {
                OverviewTab.OVERVIEW -> OverviewTabContent(
                    overview = overview,
                    onGitTap = onGitTap,
                )
                OverviewTab.FILES -> ComingSoonTab(
                    tab = OverviewTab.FILES,
                    icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                    onAction = onFilesTap,
                )
                OverviewTab.SEARCH -> ComingSoonTab(
                    tab = OverviewTab.SEARCH,
                    icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    onAction = null,
                )
                OverviewTab.SYMBOLS -> ComingSoonTab(
                    tab = OverviewTab.SYMBOLS,
                    icon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                    onAction = null,
                )
                OverviewTab.GRAPH -> ComingSoonTab(
                    tab = OverviewTab.GRAPH,
                    icon = { Icon(Icons.Outlined.AccountTree, contentDescription = null) },
                    onAction = null,
                )
                OverviewTab.GIT -> ComingSoonTab(
                    tab = OverviewTab.GIT,
                    icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                    onAction = onGitTap,
                )
                OverviewTab.AI -> ComingSoonTab(
                    tab = OverviewTab.AI,
                    icon = { Icon(Icons.Outlined.Psychology, contentDescription = null) },
                    onAction = onAITap,
                )
            }
        }
    }
}

// ── Repo info panel ──────────────────────────────────────────────────────────

@Composable
private fun RepoInfoPanel(overview: RepoOverview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
    ) {
        // Branch + sync status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        ) {
            DevOSStatusBadge(status = DevOSBadgeStatus.INFO, label = overview.branch)
            Text(
                text = "Last sync: ${overview.lastSync}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (overview.isIndexed) {
                DevOSStatusBadge(status = DevOSBadgeStatus.SUCCESS, label = "✓ Indexed")
            }
        }

        Spacer(modifier = Modifier.height(DevOSSpacing.sm))

        // Language breakdown bar (6dp tall, matching the mockup's 6px; no equivalent token)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DevOSSpacing.sm) // 8dp — nearest token above 6dp; acceptable as a
                // geometric component-internal value (no 6dp token in DevOSSpacing)
                .clip(RoundedCornerShape(DevOSSpacing.xs)), // xs = 4dp matches 3px radius
            horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.xxs),
        ) {
            overview.languageBreakdown.forEach { segment ->
                Box(
                    modifier = Modifier
                        .weight(segment.fraction)
                        .fillMaxHeight()
                        .background(segment.color.toColor()),
                )
            }
        }

        Spacer(modifier = Modifier.height(DevOSSpacing.md))

        // Stats row: Stars | Files | Open Issues | Open PRs | CI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatCell(
                value = formatStars(overview.stars),
                label = "Stars",
                valueColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = overview.fileCount.toString(),
                label = "Files",
                valueColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = overview.openIssues.toString(),
                label = "Open Issues",
                valueColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = overview.openPRs.toString(),
                label = "Open PRs",
                valueColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = "${overview.ciStatus}%",
                label = "CI",
                valueColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Tab row ──────────────────────────────────────────────────────────────────

@Composable
private fun OverviewTabRow(
    selectedTab: OverviewTab,
    onTabSelect: (OverviewTab) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = DevOSSpacing.sm,
        indicator = { tabPositions ->
            if (selectedTab.ordinal < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        OverviewTab.entries.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelect(tab) },
                text = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = tab.label },
            )
        }
    }
}

// ── Overview tab content ─────────────────────────────────────────────────────

@Composable
private fun OverviewTabContent(
    overview: RepoOverview,
    onGitTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        // AI Insights section
        item(key = "ai_insights_header") {
            DevOSSectionHeader(
                title = "AI Insights",
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical = DevOSSpacing.base,
                ),
            )
        }
        item(key = "ai_insight_card") {
            AiInsightCard(
                text = overview.aiInsight,
                highlight = overview.aiInsightHighlight,
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
            )
        }

        // Recent Commits section
        item(key = "recent_commits_header") {
            DevOSSectionHeader(
                title = "Recent Commits",
                modifier = Modifier.padding(
                    start = DevOSSpacing.base,
                    top = DevOSSpacing.xl,
                    end = DevOSSpacing.base,
                    bottom = DevOSSpacing.sm,
                ),
                action = { GitSeeAllLink(onGitTap = onGitTap) },
            )
        }
        items(
            items = overview.recentCommits,
            key = { commit -> commit.sha },
        ) { commit ->
            CommitRow(commit = commit)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = DevOSSpacing.base),
                thickness = DevOSSpacing.dividerThickness,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(DevOSSpacing.xl))
        }
    }
}

// ── Coming-soon placeholder ──────────────────────────────────────────────────

@Composable
private fun ComingSoonTab(
    tab: OverviewTab,
    icon: @Composable () -> Unit,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Wrap icon in a known vector for DevOSEmptyState
    val (emptyIcon, tabIcon) = when (tab) {
        OverviewTab.FILES   -> Pair(Icons.Outlined.FolderOpen, Icons.Outlined.FolderOpen)
        OverviewTab.SEARCH  -> Pair(Icons.Outlined.Search, Icons.Outlined.Search)
        OverviewTab.SYMBOLS -> Pair(Icons.Outlined.Code, Icons.Outlined.Code)
        OverviewTab.GRAPH   -> Pair(Icons.Outlined.AccountTree, Icons.Outlined.AccountTree)
        OverviewTab.GIT     -> Pair(Icons.Outlined.History, Icons.Outlined.History)
        OverviewTab.AI      -> Pair(Icons.Outlined.Psychology, Icons.Outlined.Psychology)
        OverviewTab.OVERVIEW -> Pair(Icons.Outlined.FolderOpen, Icons.Outlined.FolderOpen)
    }

    if (onAction != null) {
        DevOSEmptyState(
            icon = emptyIcon,
            title = "Coming soon",
            description = "${tab.label} view is coming soon.",
            modifier = modifier,
            action = {
                DevOSButton(
                    text = "Open ${tab.label}",
                    onClick = onAction,
                    style = DevOSButtonStyle.Ghost,
                )
            },
        )
    } else {
        DevOSEmptyState(
            icon = emptyIcon,
            title = "Coming soon",
            description = "${tab.label} view is coming soon.",
            modifier = modifier,
        )
    }
}

// ── Private composable helpers ────────────────────────────────────────────────

/** Resolves a [LanguageBarColor] enum to a Compose [Color] using MaterialTheme tokens. */
@Composable
private fun LanguageBarColor.toColor(): Color = when (this) {
    LanguageBarColor.PRIMARY  -> MaterialTheme.colorScheme.primary
    LanguageBarColor.TERTIARY -> MaterialTheme.colorScheme.tertiary
    LanguageBarColor.WARNING  -> DevOSAmber300
}

/**
 * AI-insight card with a 4dp ([DevOSSpacing.xs]) primary left border strip
 * and a bold-highlighted phrase in the body text.
 */
@Composable
private fun AiInsightCard(
    text: String,
    highlight: String,
    modifier: Modifier = Modifier,
) {
    DevOSCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 4dp primary left border
            Box(
                modifier = Modifier
                    .width(DevOSSpacing.xs)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
            Column(modifier = Modifier.padding(DevOSSpacing.base)) {
                Text(
                    text = buildAnnotatedString {
                        val highlightStart = text.indexOf(highlight)
                        if (highlightStart >= 0) {
                            append(text.substring(0, highlightStart))
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(highlight)
                            }
                            append(text.substring(highlightStart + highlight.length))
                        } else {
                            append(text)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** A single recent-commit row. */
@Composable
private fun CommitRow(
    commit: CommitSummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DevOSSpacing.touchTarget)
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Author avatar circle
        Box(
            modifier = Modifier
                .size(DevOSSpacing.iconSizeLarge)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = commit.authorInitial,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        // Commit message + sha/time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = commit.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${commit.sha} · ${commit.relativeTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Additions / deletions
        Text(
            text = "+${commit.additions} -${commit.deletions}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A stat cell showing a value + label, centered. */
@Composable
private fun StatCell(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * "Git →" see-all link in the Recent Commits section header.
 *
 * 48dp minimum touch target with accessibility semantics.
 */
@Composable
private fun GitSeeAllLink(onGitTap: () -> Unit) {
    Text(
        text = "Git →",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .heightIn(min = DevOSSpacing.touchTarget)
            .clickable(onClick = onGitTap)
            .padding(horizontal = DevOSSpacing.xs)
            .semantics {
                contentDescription = "See Git history"
                role = Role.Button
            },
    )
}

// ── Formatting helpers ────────────────────────────────────────────────────────

private fun formatStars(stars: Int): String = when {
    stars >= 1_000 -> "${stars / 1_000}.${(stars % 1_000) / 100}k"
    else           -> stars.toString()
}
