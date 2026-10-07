package com.devos.ai.feature.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing
import com.devos.ai.feature.auth.R
import kotlinx.coroutines.flow.SharedFlow

/**
 * Login screen — FIGMA-03
 *
 * Full-screen no-chrome layout (no TopBar). Visual spec from
 * docs/mockups/devos-ai-mockups.html #s-login.
 *
 * Layout (top → bottom):
 *   - Status bar inset
 *   - 48dp top padding
 *   - DevOS AI gradient logo (64×64dp, 16dp radius)
 *   - "Sign in to DevOS AI" title
 *   - "Connect your developer workspace" subtitle
 *   - GitHub OAuth button (outlined, left-aligned icon)
 *   - GitLab OAuth button (outlined, left-aligned icon)
 *   - OR divider
 *   - Email + Password outlined inputs
 *   - "Sign In" primary button
 *   - Inline error text (not full-screen DevOSErrorState)
 *   - Terms & Privacy footer
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
    // Collect nav events
    LaunchedEffect(Unit) {
        navEvent.collect { event ->
            when (event) {
                is LoginNavEvent.ToHome -> onNavigateToHome()
            }
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val isLoading = uiState is LoginUiState.Loading

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = MaterialTheme.spacing.base),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Logo ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF82AAFF), // primary
                                Color(0xFF89DDFF), // secondary
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_devos_logo),
                    contentDescription = "DevOS AI logo",
                    modifier = Modifier.size(36.dp),
                    tint = Color(0xFF001E6E), // onPrimary
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.base))

            // ── Headline ──────────────────────────────────────────────
            Text(
                text = "Sign in to DevOS AI",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(MaterialTheme.spacing.xs))

            Text(
                text = "Connect your developer workspace",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(MaterialTheme.spacing.xxl))

            // ── GitHub button ─────────────────────────────────────────
            DevOSButton(
                text = "Continue with GitHub",
                onClick = onLoginGitHub,
                style = DevOSButtonStyle.Secondary,
                enabled = !isLoading,
                leadingIcon = {
                    if (isLoading && uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(MaterialTheme.spacing.sm))
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(MaterialTheme.spacing.sm))

            // ── GitLab button ─────────────────────────────────────────
            DevOSButton(
                text = "Continue with GitLab",
                onClick = onLoginGitLab,
                style = DevOSButtonStyle.Secondary,
                enabled = !isLoading,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_gitlab),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(MaterialTheme.spacing.sm))
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // ── OR divider ────────────────────────────────────────────
            Spacer(Modifier.height(MaterialTheme.spacing.base))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = "  or  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.height(MaterialTheme.spacing.base))

            // ── Email input ───────────────────────────────────────────
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "EMAIL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                enabled = !isLoading,
            )

            Spacer(Modifier.height(MaterialTheme.spacing.sm))

            // ── Password input ────────────────────────────────────────
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        "PASSWORD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff
                                          else Icons.Outlined.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                enabled = !isLoading,
            )

            Spacer(Modifier.height(MaterialTheme.spacing.sm))

            // ── Sign In button ────────────────────────────────────────
            DevOSButton(
                text = "Sign In",
                onClick = { /* email/password sign-in — future ticket */ },
                style = DevOSButtonStyle.Primary,
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Inline error (NOT DevOSErrorState — stays in layout) ──
            if (uiState is LoginUiState.Error) {
                Spacer(Modifier.height(MaterialTheme.spacing.sm))
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Footer ────────────────────────────────────────────────
            val footerText = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append("By continuing you agree to our ")
                }
                pushStringAnnotation("terms", "https://devos.ai/terms")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("Terms")
                }
                pop()
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append(" & ")
                }
                pushStringAnnotation("privacy", "https://devos.ai/privacy")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("Privacy Policy")
                }
                pop()
            }
            ClickableText(
                text = footerText,
                style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.spacing.base)
                    .padding(bottom = MaterialTheme.spacing.xl),
                onClick = { /* open terms/privacy URL — future ticket */ },
            )
        }
    }
}
