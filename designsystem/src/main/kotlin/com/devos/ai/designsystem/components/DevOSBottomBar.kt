package com.devos.ai.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Bottom navigation bar with 5 primary tabs.
 *
 * Tabs: Home | Projects | AI | Learn | More
 *
 * @param currentRoute The active route string (matches DevOSRoutes constants)
 * @param onNavigate Called with route string when a tab is tapped
 * @param modifier Optional modifier
 */
@Composable
fun DevOSBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(text = item.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.semantics { contentDescription = item.label },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

// Route constants mirrored from DevOSRoutes to avoid cross-module dependency
private const val ROUTE_HOME               = "home"
private const val ROUTE_PROJECT_LIST       = "project_list"
private const val ROUTE_AI_CHAT            = "ai_chat"
private const val ROUTE_LEARNING_DASHBOARD = "learning_dashboard"
private const val ROUTE_MORE               = "more"

private val BottomNavItems = listOf(
    BottomNavItem(route = ROUTE_HOME,               label = "Home",     icon = Icons.Outlined.Home),
    BottomNavItem(route = ROUTE_PROJECT_LIST,        label = "Projects", icon = Icons.Outlined.FolderOpen),
    BottomNavItem(route = ROUTE_AI_CHAT,             label = "AI",       icon = Icons.Outlined.SmartToy),
    BottomNavItem(route = ROUTE_LEARNING_DASHBOARD,  label = "Learn",    icon = Icons.Outlined.School),
    BottomNavItem(route = ROUTE_MORE,                label = "More",     icon = Icons.Outlined.MoreHoriz),
)
