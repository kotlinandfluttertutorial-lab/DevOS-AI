package com.devos.ai.feature.auth.login

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.theme.spacing
import com.devos.ai.feature.auth.R
import kotlinx.coroutines.flow.SharedFlow

/**
 * Login screen for DevOS AI.
 *
 * Full-screen no-chrome layout (no TopBar) matching the `#s-login` mockup.
 *
 * UI states handled:
 * - Idle: all interactive elements enabled
 * - Loading: OAuth buttons disabled, inline progress shown
 * - Error: inline error text shown in error color (NOT full-screen DevOSErrorState)
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    navEvent: SharedFlow<LoginNavEvent>,
    onLoginGitHub: () -> Unit,
    onLoginGitLab: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        navEvent.collect { event ->
            when (event) {
                is LoginNavEvent.ToHome -> onNavigateToHome()
            }
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = uiState is LoginUiState.Loading

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.base),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // ── Logo ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .semantics { contentDescription = "DevOS AI logo" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_devos_logo),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text = "Sign in to DevOS AI",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // ── Subtitle ──────────────────────────────────────────────────────────
        Text(
            text = "Connect your developer workspace",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))

        // ── GitHub button ─────────────────────────────────────────────────────
        DevOSButton(
            text = "Continue with GitHub",
            onClick = onLoginGitHub,
            style = DevOSButtonStyle.Secondary,
            enabled = !isLoading,
            leadingIcon = {
                if (isLoading) {
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
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── GitLab button ─────────────────────────────────────────────────────
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
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // ── Error state (inline) ──────────────────────────────────────────────
        if (uiState is LoginUiState.Error) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        // ── OR divider ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = "or",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        // ── Email input ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(MaterialTheme.spacing.sm),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(MaterialTheme.spacing.sm),
                )
                .padding(horizontal = MaterialTheme.spacing.base, vertical = 14.dp),
        ) {
            Text(
                text = "EMAIL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        text = "dev@example.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                singleLine = true,
                enabled = !isLoading,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Password input ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(MaterialTheme.spacing.sm),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(MaterialTheme.spacing.sm),
                )
                .padding(horizontal = MaterialTheme.spacing.base, vertical = 14.dp),
        ) {
            Text(
                text = "PASSWORD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                singleLine = true,
                enabled = !isLoading,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Sign In primary button ────────────────────────────────────────────
        DevOSButton(
            text = "Sign In",
            onClick = { /* Email/password sign-in — placeholder for DEVOS-013 */ },
            style = DevOSButtonStyle.Primary,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        // ── Footer ────────────────────────────────────────────────────────────
        val footerText = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            ) {
                append("By continuing you agree to our ")
            }
            pushStringAnnotation(tag = "TERMS", annotation = "terms")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                ),
            ) {
                append("Terms")
            }
            pop()
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            ) {
                append(" & ")
            }
            pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                ),
            ) {
                append("Privacy Policy")
            }
            pop()
        }

        ClickableText(
            text = footerText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.spacing.base),
            style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
            onClick = { offset ->
                footerText.getStringAnnotations("TERMS", offset, offset)
                    .firstOrNull()?.let { onTermsClick() }
                footerText.getStringAnnotations("PRIVACY", offset, offset)
                    .firstOrNull()?.let { onPrivacyClick() }
            },
        )
    }
}
