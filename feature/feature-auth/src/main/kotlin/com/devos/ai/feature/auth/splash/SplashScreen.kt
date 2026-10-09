package com.devos.ai.feature.auth.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.theme.DevOSAmber300
import com.devos.ai.designsystem.theme.DevOSBlue300
import com.devos.ai.designsystem.theme.DevOSCyan300
import com.devos.ai.designsystem.theme.DevOSNavy400
import com.devos.ai.designsystem.theme.DevOSNavy600
import com.devos.ai.designsystem.theme.DevOSNavy800
import com.devos.ai.designsystem.theme.spacing
import com.devos.ai.feature.auth.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

/**
 * Splash screen — shown on every app launch while [SplashViewModel] determines
 * the correct destination.
 *
 * Matches `#s-splash` in devos-ai-mockups.html exactly:
 * - 80×80dp gradient logo box (20dp radius, shadow)
 * - "DevOS AI" title (32sp 800w, #E2E8F0)
 * - Tagline (13sp, #A8B3CF)
 * - 48×4dp gradient loading bar with infinite shimmer animation
 * - "Initializing…" caption (12sp, #546E7A)
 * - Version string pinned to bottom (11sp, #3A3F58)
 */
@Composable
fun SplashScreen(
    navEvent: SharedFlow<SplashNavEvent>,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        navEvent.collect { event ->
            when (event) {
                SplashNavEvent.ToOnboarding -> onNavigateToOnboarding()
                SplashNavEvent.ToHome       -> onNavigateToHome()
                SplashNavEvent.ToLogin      -> onNavigateToLogin()
            }
        }
    }

    // Entrance animation — scale + fade from 0.7 → 1.0 over 600 ms
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue  = if (visible) 1f else 0.7f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label        = "splash_scale",
    )
    val alpha by animateFloatAsState(
        targetValue  = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label        = "splash_alpha",
    )

    // Infinite shimmer for the loading bar: translate X from -48dp → +48dp
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue   = -1f,
        targetValue    = 2f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // ── Centre content ────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                .padding(horizontal = 32.dp),
        ) {
            // Logo box — 80×80dp, 20dp radius, gradient bg, elevation shadow
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(DevOSBlue300, DevOSCyan300),
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter           = painterResource(id = R.drawable.ic_devos_logo),
                    contentDescription = "DevOS AI logo",
                    modifier          = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name — 32sp 800w as in mockup
            Text(
                text      = "DevOS AI",
                color     = MaterialTheme.colorScheme.onBackground,
                fontSize  = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline — 13sp #A8B3CF
            Text(
                text      = "Understand your code.\nLearn faster. Build smarter.",
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize  = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading bar — 48×4dp track, gradient shimmer fill
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DevOSNavy800),
            ) {
                // Animated gradient fill — slides across the track
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors     = listOf(
                                    Color.Transparent,
                                    DevOSBlue300,
                                    DevOSCyan300,
                                    Color.Transparent,
                                ),
                                startX     = shimmerOffset * 96f - 96f,
                                endX       = shimmerOffset * 96f,
                            ),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // "Initializing…" caption
            Text(
                text     = "Initializing\u2026",
                color    = DevOSNavy600,
                fontSize = 12.sp,
            )
        }

        // ── Version string pinned to bottom ───────────────────────────────────
        Text(
            text     = "v1.0.0 · Build 2026.10.08",
            color    = Color(0xFF3A3F58),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
        )
    }
}
