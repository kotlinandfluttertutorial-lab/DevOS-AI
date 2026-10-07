package com.devos.ai.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.spacing
import com.devos.ai.feature.auth.R
import kotlinx.coroutines.flow.SharedFlow

/**
 * Login screen — entry point for GitHub and GitLab OAuth authentication.
 *
 * Stateless composable: receives [uiState] from [AuthViewModel] and delegates
 * all user actions via callbacks (no direct ViewModel reference in the tree).
 *
 * Layout:
 *  - [DevOSTopBar] with "Sign In"
 *  - Centered column: welcome text + two OAuth buttons + optional error state
 *
 * @param uiState Current state from [AuthViewModel].
 * @param navEvent SharedFlow from [AuthViewModel]; collected here for one-shot navigation.
 * @param onLoginGitHub Triggers [AuthViewModel.loginWithGitHub].
 * @param onLoginGitLab Triggers [AuthViewModel.loginWithGitLab].
 * @param onNavigateToHome Navigates to Home screen (called on [AuthNavEvent.NavigateToHome]).
 * @param modifier Optional modifier.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    navEvent: SharedFlow<AuthNavEvent>,
    onLoginGitHub: () -> Unit,
    onLoginGitLab: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Collect one-shot navigation events
    LaunchedEffect(Unit) {
        navEvent.collect { event ->
            when (event) {
                AuthNavEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DevOSTopBar(title = "Sign In")
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Welcome to DevOS AI",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Text(
                text = "Connect your repository host to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            // Primary: GitHub
            DevOSButton(
                text = "Continue with GitHub",
                onClick = onLoginGitHub,
                style = DevOSButtonStyle.Primary,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is LoginUiState.Loading,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            // Secondary: GitLab
            DevOSButton(
                text = "Continue with GitLab",
                onClick = onLoginGitLab,
                style = DevOSButtonStyle.Secondary,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_gitlab),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is LoginUiState.Loading,
            )

            // Error state — shown only when authentication fails
            if (uiState is LoginUiState.Error) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                DevOSErrorState(
                    description = uiState.message,
                    onRetry = null,
                )
            }
        }
    }
}
