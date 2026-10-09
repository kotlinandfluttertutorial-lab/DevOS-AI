package com.devos.ai.feature.settings.provider

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devos.ai.designsystem.components.DevOSBadgeStatus
import com.devos.ai.designsystem.components.DevOSButton
import com.devos.ai.designsystem.components.DevOSButtonStyle
import com.devos.ai.designsystem.components.DevOSCard
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSStatusBadge
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.domain.ai.model.AIProvider

/**
 * Provider Settings screen — DEVOS-034 / FIGMA-35.
 *
 * Matches the `#s-provider-settings` mockup exactly:
 * - Amber security banner at the top
 * - One card per provider showing connection status, masked key, model info
 * - Inline key entry (AnimatedVisibility expand/collapse) on tap of Edit/Configure
 * - "Test connection" while saving
 *
 * 4 UI states: Loading | Success | Empty | Error.
 */
@Composable
fun ProviderSettingsScreen(
    uiState: ProviderSettingsUiState,
    onNavigateBack: () -> Unit,
    onEditKey: (AIProvider) -> Unit,
    onConfigureProvider: (AIProvider) -> Unit,
    onSaveKey: (AIProvider, String) -> Unit,
    onCancelEdit: (AIProvider) -> Unit,
    onTestConnection: (AIProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier        = modifier,
        containerColor  = MaterialTheme.colorScheme.background,
        topBar          = {
            DevOSTopBar(
                title    = "AI Providers",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        when (val s = uiState) {
            is ProviderSettingsUiState.Loading ->
                DevOSLoadingState(modifier = Modifier.padding(innerPadding))

            is ProviderSettingsUiState.Empty ->
                DevOSEmptyState(
                    icon        = Icons.Outlined.Lock,
                    title       = "No providers",
                    description = "Add an API key to start using AI features",
                    modifier    = Modifier.padding(innerPadding),
                )

            is ProviderSettingsUiState.Error ->
                DevOSErrorState(
                    description = s.message,
                    onRetry     = null,
                    modifier    = Modifier.padding(innerPadding),
                )

            is ProviderSettingsUiState.Success ->
                ProviderList(
                    providers          = s.providers,
                    onEditKey          = onEditKey,
                    onConfigureProvider = onConfigureProvider,
                    onSaveKey          = onSaveKey,
                    onCancelEdit       = onCancelEdit,
                    onTestConnection   = onTestConnection,
                    modifier           = Modifier.padding(innerPadding),
                )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Success content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProviderList(
    providers: List<ProviderCardState>,
    onEditKey: (AIProvider) -> Unit,
    onConfigureProvider: (AIProvider) -> Unit,
    onSaveKey: (AIProvider, String) -> Unit,
    onCancelEdit: (AIProvider) -> Unit,
    onTestConnection: (AIProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier        = modifier.fillMaxSize(),
        contentPadding  = androidx.compose.foundation.layout.PaddingValues(
            bottom = DevOSSpacing.xl,
        ),
    ) {
        // ── Security banner ───────────────────────────────────────────────────
        item(key = "security_banner") {
            SecurityBanner(
                modifier = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical   = DevOSSpacing.sm,
                ),
            )
        }

        // ── Provider cards ────────────────────────────────────────────────────
        items(items = providers, key = { it.provider.name }) { card ->
            ProviderCard(
                state              = card,
                onEditKey          = onEditKey,
                onConfigureProvider = onConfigureProvider,
                onSaveKey          = onSaveKey,
                onCancelEdit       = onCancelEdit,
                onTestConnection   = onTestConnection,
                modifier           = Modifier.padding(
                    horizontal = DevOSSpacing.base,
                    vertical   = DevOSSpacing.xs,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Security banner
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Amber warning banner matching the mockup:
 * - Background: `#3d2c00` (dark amber)
 * - Border: 1dp `#FFCB6B`
 * - 8dp corner radius
 * - 🔒 icon + 12sp amber text
 */
@Composable
private fun SecurityBanner(modifier: Modifier = Modifier) {
    val amberColor   = Color(0xFFFFCB6B)
    val amberBg      = Color(0xFF3D2C00)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(amberBg, RoundedCornerShape(8.dp))
            .border(1.dp, amberColor, RoundedCornerShape(8.dp))
            .padding(horizontal = DevOSSpacing.base, vertical = DevOSSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        verticalAlignment     = Alignment.Top,
    ) {
        Icon(
            imageVector        = Icons.Outlined.Lock,
            contentDescription = null,
            tint               = amberColor,
            modifier           = Modifier.size(DevOSSpacing.iconSizeSmall),
        )
        Text(
            text  = "API keys are stored in the system EncryptedSharedPreferences keystore " +
                "and never transmitted to third parties.",
            color = amberColor,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Provider card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProviderCard(
    state: ProviderCardState,
    onEditKey: (AIProvider) -> Unit,
    onConfigureProvider: (AIProvider) -> Unit,
    onSaveKey: (AIProvider, String) -> Unit,
    onCancelEdit: (AIProvider) -> Unit,
    onTestConnection: (AIProvider) -> Unit,
    modifier: Modifier = Modifier,
) {
    DevOSCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier            = Modifier.padding(DevOSSpacing.base),
            verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
        ) {
            // ── Header row: icon + name + badge ───────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
            ) {
                // Provider icon box — 36×36dp, 8dp radius
                androidx.compose.foundation.layout.Box(
                    modifier            = Modifier
                        .size(36.dp)
                        .background(
                            color = if (state.isConfigured)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        ),
                    contentAlignment    = Alignment.Center,
                ) {
                    Text(
                        text     = state.provider.emoji(),
                        fontSize = 16.sp,
                    )
                }

                // Provider name
                Text(
                    text     = state.provider.displayName,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = if (state.isConfigured)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                // Status badge
                if (state.isConfigured) {
                    DevOSStatusBadge(
                        status = DevOSBadgeStatus.SUCCESS,
                        label  = "Connected",
                    )
                } else {
                    DevOSStatusBadge(
                        status = DevOSBadgeStatus.PENDING,
                        label  = "Not configured",
                    )
                }
            }

            // ── Configured state: masked key + model info ─────────────────────
            if (state.isConfigured && !state.isEditingKey) {
                // Masked key row
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(
                        text      = state.maskedKey ?: "",
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize  = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier  = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Masked API key" },
                    )
                    TextButton(
                        onClick  = { onEditKey(state.provider) },
                        modifier = Modifier.semantics {
                            contentDescription = "Edit API key for ${state.provider.displayName}"
                        },
                    ) {
                        Text(
                            text  = "Edit",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                // Model + capabilities subtitle
                Text(
                    text  = "Model: ${state.selectedModel} · ${state.capabilities}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Not configured: single Configure button ───────────────────────
            if (!state.isConfigured && !state.isEditingKey) {
                DevOSButton(
                    text     = "Configure ${state.provider.displayName}",
                    onClick  = { onConfigureProvider(state.provider) },
                    style    = DevOSButtonStyle.Outlined,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "Configure ${state.provider.displayName}"
                        },
                )
            }

            // ── Key input (animated expand) ───────────────────────────────────
            AnimatedVisibility(
                visible = state.isEditingKey,
                enter   = expandVertically(),
                exit    = shrinkVertically(),
            ) {
                KeyInputSection(
                    provider         = state.provider,
                    isTesting        = state.isTestingConnection,
                    connectionError  = state.connectionError,
                    onSave           = { key -> onSaveKey(state.provider, key) },
                    onCancel         = { onCancelEdit(state.provider) },
                )
            }

            // ── Test connection in-progress indicator ─────────────────────────
            if (state.isTestingConnection && !state.isEditingKey) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier     = Modifier.size(16.dp),
                        strokeWidth  = 2.dp,
                        color        = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text  = "Testing connection…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Connection error ──────────────────────────────────────────────
            if (state.connectionError != null) {
                Text(
                    text  = state.connectionError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Key input section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun KeyInputSection(
    provider: AIProvider,
    isTesting: Boolean,
    connectionError: String?,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var key by rememberSaveable { mutableStateOf("") }

    val isOllama     = provider == AIProvider.OLLAMA
    val placeholder  = if (isOllama) "http://192.168.1.10:11434" else "Paste your API key…"
    val label        = if (isOllama) "BASE URL" else "API KEY"

    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DevOSSpacing.sm),
    ) {
        OutlinedTextField(
            value         = key,
            onValueChange = { key = it },
            label         = {
                Text(label, style = MaterialTheme.typography.labelSmall)
            },
            placeholder   = {
                Text(placeholder, style = MaterialTheme.typography.bodySmall)
            },
            // Mask API key input; show plain text for Ollama URLs
            visualTransformation = if (isOllama)
                androidx.compose.ui.text.input.VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isOllama) KeyboardType.Uri else KeyboardType.Password,
                imeAction    = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (key.isNotBlank()) onSave(key) }),
            singleLine      = true,
            modifier        = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Enter $label for ${provider.displayName}" },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(DevOSSpacing.sm)) {
            DevOSButton(
                text     = if (isTesting) "Testing…" else "Save & Test",
                onClick  = { if (key.isNotBlank()) onSave(key) },
                enabled  = key.isNotBlank() && !isTesting,
                modifier = Modifier.weight(1f),
            )
            DevOSButton(
                text    = "Cancel",
                onClick = onCancel,
                style   = DevOSButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
        }

        if (connectionError != null) {
            Text(
                text  = connectionError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Returns an appropriate emoji icon for each provider (matches mockup icon boxes). */
private fun AIProvider.emoji(): String = when (this) {
    AIProvider.ANTHROPIC -> "🤖"
    AIProvider.OPENAI    -> "⚡"
    AIProvider.GEMINI    -> "✨"
    AIProvider.OLLAMA    -> "🦙"
}
