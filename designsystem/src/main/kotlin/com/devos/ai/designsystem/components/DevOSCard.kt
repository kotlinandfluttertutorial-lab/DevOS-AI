package com.devos.ai.designsystem.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Clickable card container.
 *
 * Uses MaterialTheme.shapes.medium (12dp corners) and surface color fill.
 *
 * @param onClick Click callback
 * @param modifier Optional modifier
 * @param content Card content
 */
@Composable
fun DevOSCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        content = content,
    )
}

/**
 * Static (non-clickable) card container.
 *
 * Uses MaterialTheme.shapes.medium (12dp corners) and surface color fill.
 *
 * @param modifier Optional modifier
 * @param content Card content
 */
@Composable
fun DevOSCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        content = content,
    )
}
