package com.devos.ai.feature.code.graph

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.devos.ai.designsystem.components.DevOSEmptyState
import com.devos.ai.designsystem.components.DevOSLoadingState
import com.devos.ai.designsystem.components.DevOSTopBar
import com.devos.ai.designsystem.theme.DevOSSpacing

/**
 * Dependency Graph screen — DEVOS-024 / FIGMA-14.
 *
 * Currently a placeholder stub showing "Interactive graph coming soon".
 * Full Canvas-based force-directed graph implementation is a future enhancement.
 */
@Composable
fun DependencyGraphScreen(
    uiState: DependencyGraphUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DevOSTopBar(
                title = "Dependency Graph",
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
            is DependencyGraphUiState.Loading -> DevOSLoadingState(
                modifier = Modifier.padding(innerPadding),
            )
            is DependencyGraphUiState.ComingSoon -> DevOSEmptyState(
                icon = Icons.Outlined.AccountTree,
                title = "Interactive graph coming soon",
                description = "Force-directed dependency visualization will be available in a future update",
                modifier = Modifier.padding(innerPadding),
            )
            is DependencyGraphUiState.Error -> com.devos.ai.designsystem.components.DevOSErrorState(
                description = uiState.message,
                onRetry = if (uiState.retryable) ({}) else null,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
