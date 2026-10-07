package com.devos.ai.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Standard top app bar for DevOS AI screens.
 *
 * Wraps [CenterAlignedTopAppBar] with DevOS typography tokens.
 *
 * @param title Screen title — displayed in titleLarge typography
 * @param modifier Optional modifier
 * @param subtitle Optional subtitle below the title — displayed in bodySmall / onSurfaceVariant
 * @param navigationIcon Optional back/drawer icon composable (slot)
 * @param actions Optional row of action icons (slot)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevOSTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = { navigationIcon?.invoke() },
        actions = { actions?.invoke() },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
