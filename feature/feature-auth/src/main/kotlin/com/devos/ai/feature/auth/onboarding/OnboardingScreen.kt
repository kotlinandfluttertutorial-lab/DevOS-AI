package com.devos.ai.feature.auth.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.theme.spacing
import kotlinx.coroutines.flow.SharedFlow

/**
 * Onboarding carousel — shown only on first launch.
 *
 * Matches `#s-onboarding` in devos-ai-mockups.html:
 * - Each slide has a 200×180dp illustration card (surface bg, 24dp radius, 1dp outline border)
 *   with a 72sp icon centred inside.
 * - Step dots: active = 24×8dp pill (#82AAFF / primary), inactive = 8×8dp circle (#252840 / surfaceVariant).
 * - "Skip" ghost button top-right.
 * - "Next →" / "Get Started" primary full-width button.
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
    LaunchedEffect(Unit) {
        navEvent.collect { event ->
            when (event) {
                OnboardingNavEvent.ToLogin -> onNavigateToLogin()
            }
        }
    }

    if (uiState !is OnboardingUiState.Success) return

    val pages       = uiState.pages
    val currentPage = uiState.currentPage

    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount   = { pages.size },
    )

    // Keep pager in sync when ViewModel increments currentPage (Next button)
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    // Keep ViewModel in sync when user swipes manually
    LaunchedEffect(pagerState.currentPage) {
        onSetPage(pagerState.currentPage)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.base),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick  = onSkip,
                    modifier = Modifier.semantics { contentDescription = "Skip onboarding" },
                ) {
                    Text(
                        text  = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier            = Modifier.padding(MaterialTheme.spacing.base),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Step indicator dots ────────────────────────────────────────
                // Active dot: 24×8dp pill. Inactive dot: 8×8dp circle.
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    pages.forEachIndexed { index, _ ->
                        StepDot(active = index == pagerState.currentPage)
                        if (index < pages.lastIndex) {
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                if (pagerState.currentPage == pages.lastIndex) {
                    DevOSButton(
                        text     = "Get Started",
                        onClick  = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    DevOSButton(
                        text     = "Next →",
                        onClick  = onNextPage,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.padding(padding),
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page content
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Content for a single onboarding slide.
 *
 * Matches `#s-onboarding` slide layout:
 * - 200×180dp illustration card: surface background, 24dp rounded corners, 1dp outline border
 * - 72sp icon centred inside the card
 * - Title (24sp 700w, onBackground, 1.3 line-height)
 * - Description (14sp, onSurfaceVariant, 1.6 line-height)
 */
@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Illustration card — 200×180dp, surface bg, 24dp radius, outline border
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = page.icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(72.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text       = page.title,
            style      = MaterialTheme.typography.titleLarge.copy(
                fontWeight  = FontWeight.Bold,
                lineHeight  = MaterialTheme.typography.titleLarge.fontSize * 1.3f,
            ),
            color      = MaterialTheme.colorScheme.onBackground,
            textAlign  = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text      = page.description,
            style     = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.6f,
            ),
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step dot
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pager step-indicator dot.
 *
 * Matches the mockup exactly:
 * - Active  → 24×8dp pill, primary color (#82AAFF), 4dp rounded corners
 * - Inactive → 8×8dp circle, surfaceVariant color (#252840), fully rounded
 */
@Composable
fun StepDot(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val width  = if (active) 24.dp else 8.dp
    val height = 8.dp
    val radius = 4.dp
    val color  = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(radius))
            .background(color)
            .semantics {
                contentDescription = if (active) "Current page" else "Page indicator"
            },
    )
}
