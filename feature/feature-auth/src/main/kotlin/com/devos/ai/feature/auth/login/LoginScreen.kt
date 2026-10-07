package com.devos.ai.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing
import com.devos.ai.feature.auth.R
import kotlinx.coroutines.flow.SharedFlow

/**
 * Login screen for DevOS AI.
 *
 * Stateless composable — all state is owned by AuthViewModel and hoisted by the
 * NavGraphBuilder entry in AuthNavigation.
 *
 * UI states handled:
 * - Idle: buttons enabled, no indicator
 * - Loading: buttons disabled, progress indicator shown
 * - Error: buttons re-enabled, error message shown via DevOSErrorState
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    navEvent: SharedFlow<LoginNavEvent>,
    onLoginGitHub: () -> Unit,
    onLoginGitLab: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        navEvent.collect { event ->
            when (event) {
                is LoginNavEvent.ToHome -> onNavigateToHome()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { DevOSTopBar(title = "Sign In") },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Welcome to DevOS AI",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Text(
                text = "Connect your repository host to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
            DevOSButton(
                text = "Continue with GitHub",
                onClick = onLoginGitHub,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = null,
                        modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is LoginUiState.Loading,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
            DevOSButton(
                text = "Continue with GitLab",
                onClick = onLoginGitLab,
                style = DevOSButtonStyle.Secondary,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_gitlab),
                        contentDescription = null,
                        modifier = Modifier.size(DevOSSpacing.iconSizeSmall),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is LoginUiState.Loading,
            )
            if (uiState is LoginUiState.Loading) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                CircularProgressIndicator()
            }
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
