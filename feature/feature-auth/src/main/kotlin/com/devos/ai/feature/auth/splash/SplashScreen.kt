package com.devos.ai.feature.auth.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.theme.spacing
import com.devos.ai.feature.auth.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

/**
 * Splash screen — shown on every app launch while [SplashViewModel] determines
 * the correct destination.
 *
 * Animated logo (scale + fade, 600 ms) and tagline. Background matches
 * [MaterialTheme.colorScheme.background] to prevent a white flash during startup.
 *
 * @param navEvent SharedFlow from [SplashViewModel]; collected here to trigger navigation.
 * @param onNavigateToOnboarding Navigate to the Onboarding carousel.
 * @param onNavigateToHome Navigate to the Home screen.
 * @param onNavigateToLogin Navigate to the Login screen.
 * @param modifier Optional modifier.
 */
@Composable
fun SplashScreen(
    navEvent: SharedFlow<SplashNavEvent>,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Collect navigation events — routed to the caller's NavController
    LaunchedEffect(Unit) {
        navEvent.collect { event ->
            when (event) {
                SplashNavEvent.ToOnboarding -> onNavigateToOnboarding()
                SplashNavEvent.ToHome -> onNavigateToHome()
                SplashNavEvent.ToLogin -> onNavigateToLogin()
            }
        }
    }

    // Animate into view shortly after composition
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splash_scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "splash_alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = alpha,
            ),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_devos_logo),
                contentDescription = "DevOS AI logo",
                modifier = Modifier.size(96.dp),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
            Text(
                text = "Understand your code. Learn faster. Build smarter.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xl),
            )
        }
    }
}
