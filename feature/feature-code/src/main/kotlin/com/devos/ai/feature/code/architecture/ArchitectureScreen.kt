package com.devos.ai.feature.code.architecture

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSErrorState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Architecture Overview screen — DEVOS-025 / FIGMA-15.
 *
 * Currently a placeholder stub showing "Interactive graph coming soon".
 * Full architecture layer diagram and AI summary streaming is a future enhancement.
 */
@Composable
fun ArchitectureScreen(
    uiState: ArchitectureUiState,
    onNavigateBack: () -> Unit,
    onAskAiTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Architecture Overview",
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(DevOSSpacing.touchTarget)
                            .semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is ArchitectureUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )
            is ArchitectureUiState.ComingSoon -> DevOSEmptyState(
                icon = Icons.Outlined.Hub,
                title = "Interactive graph coming soon",
                description = "AI-powered architecture detection and layer visualization will be available in a future update",
                modifier = Modifier.padding(innerPadding),
            )
            is ArchitectureUiState.Error -> DevOSErrorState(
                description = uiState.message,
                onRetry = if (uiState.retryable) ({}) else null,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
