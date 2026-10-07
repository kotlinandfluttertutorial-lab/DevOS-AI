package com.devos.ai.feature.auth.onboarding

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.theme.spacing
import kotlinx.coroutines.flow.SharedFlow

/**
 * Onboarding carousel — shown only on first launch.
 *
 * Uses [HorizontalPager] with [rememberPagerState] for 4 pages. The ViewModel
 * is kept in sync via [LaunchedEffect] when the pager page changes.
 *
 * @param uiState Current state from [OnboardingViewModel.uiState].
 * @param navEvent One-shot events from [OnboardingViewModel.navEvent].
 * @param onNavigateToLogin Navigates to the Login screen.
 * @param onNextPage Delegates to [OnboardingViewModel.nextPage].
 * @param onSetPage Delegates to [OnboardingViewModel.setPage] when pager scrolls.
 * @param onSkip Delegates to [OnboardingViewModel.skipOnboarding].
 * @param onComplete Delegates to [OnboardingViewModel.completeOnboarding].
 * @param modifier Optional modifier.
 */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    navEvent: SharedFlow<OnboardingNavEvent>,
    onNavigateToLogin: () -> Unit,
    onNextPage: () -> Unit,
    onSetPage: (Int) -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Collect one-shot nav events
    LaunchedEffect(Unit) {
        navEvent.collect { event ->
            when (event) {
                OnboardingNavEvent.ToLogin -> onNavigateToLogin()
            }
        }
    }

    if (uiState !is OnboardingUiState.Success) return

    val pages = uiState.pages
    val currentPage = uiState.currentPage

    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount = { pages.size },
    )

    // Keep pager in sync with ViewModel when ViewModel updates (e.g. Next button tap)
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    // Keep ViewModel in sync when user manually swipes
    LaunchedEffect(pagerState.currentPage) {
        onSetPage(pagerState.currentPage)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.semantics { contentDescription = "Skip onboarding" },
                ) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.base),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Step indicator dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    pages.forEachIndexed { index, _ ->
                        StepDot(active = index == pagerState.currentPage)
                        if (index < pages.lastIndex) {
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                if (pagerState.currentPage == pages.lastIndex) {
                    DevOSButton(
                        text = "Get Started",
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    DevOSButton(
                        text = "Next",
                        onClick = onNextPage,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding),
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }
    }
}

/**
 * Content for a single onboarding page — icon, title, and description.
 */
@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        Text(
            text = page.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Pager step indicator dot.
 *
 * @param active Whether this dot represents the current page.
 */
@Composable
fun StepDot(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
            .semantics { contentDescription = if (active) "Current page" else "Page indicator" },
    )
}
