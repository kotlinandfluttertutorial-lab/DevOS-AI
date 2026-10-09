package com.devos.ai.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing
import com.devos.ai.designsystem.theme.spacing

/**
 * Settings Root screen (DEVOS-062 / DA-78).
 *
 * Stateless composable — all state comes from [SettingsRootUiState],
 * all events go up via callback parameters.
 *
 * Matches mockup #s-settings.
 */
@Composable
fun SettingsRootScreen(
    uiState: SettingsRootUiState,
    onNavigateBack: () -> Unit,
    onNavigateToAISettings: () -> Unit,
    onNavigateToProviderSettings: () -> Unit,
    onNavigateToDeveloperMemory: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DevOSTopBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
        ) {

            // ── AI GROUP ────────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(title = "AI")
                SettingsNavRow(
                    icon = Icons.Outlined.SmartToy,
                    iconContentDescription = "AI Configuration",
                    label = "AI Configuration",
                    subtitle = "Models, RAG, Agent settings",
                    onClick = onNavigateToAISettings,
                )
                SettingsNavRow(
                    icon = Icons.Outlined.Key,
                    iconContentDescription = "API Providers",
                    label = "API Providers",
                    subtitle = "OpenAI, Anthropic, Ollama",
                    onClick = onNavigateToProviderSettings,
                )
                SettingsNavRow(
                    icon = Icons.Outlined.Memory,
                    iconContentDescription = "Developer Memory",
                    label = "Developer Memory",
                    subtitle = "Manage AI preferences",
                    onClick = onNavigateToDeveloperMemory,
                )
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = DevOSSpacing.dividerThickness,
                )
            }

            // ── APPEARANCE GROUP ─────────────────────────────────────────────────
            item {
                SettingsSectionHeader(title = "APPEARANCE")
                SettingsToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    iconContentDescription = "Dark Mode",
                    label = "Dark Mode",
                    subtitle = "Always on (developer default)",
                    checked = uiState.darkMode,
                    onCheckedChange = onToggleDarkMode,
                )
                SettingsNavRow(
                    icon = Icons.Outlined.TextFields,
                    iconContentDescription = "Font Scale",
                    label = "Font Scale",
                    subtitle = "System default",
                    onClick = {},
                )
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = DevOSSpacing.dividerThickness,
                )
            }

            // ── ACCOUNT GROUP ────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(title = "ACCOUNT")
                SettingsNavRow(
                    icon = Icons.Outlined.Person,
                    iconContentDescription = "Profile",
                    label = "Profile",
                    subtitle = "dev@example.com",
                    onClick = onNavigateToProfile,
                )
                SettingsNavRow(
                    icon = Icons.Outlined.Notifications,
                    iconContentDescription = "Notifications",
                    label = "Notifications",
                    subtitle = "Enabled · 5 types",
                    onClick = onNavigateToNotifications,
                )
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    thickness = DevOSSpacing.dividerThickness,
                )
            }

            // ── ABOUT GROUP ──────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(title = "ABOUT")
                // Version row — no chevron, shows version as subtitle
                SettingsInfoRow(
                    icon = Icons.Outlined.Info,
                    iconContentDescription = "Version",
                    label = "Version",
                    subtitle = "1.0.0 (build 2026.10.07)",
                )
                SettingsNavRow(
                    icon = Icons.Outlined.Feedback,
                    iconContentDescription = "Send Feedback",
                    label = "Send Feedback",
                    subtitle = "",
                    onClick = {},
                )
                SettingsNavRow(
                    icon = Icons.Outlined.Policy,
                    iconContentDescription = "Privacy Policy",
                    label = "Privacy Policy",
                    subtitle = "",
                    onClick = {},
                )
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))
            }
        }
    }
}
