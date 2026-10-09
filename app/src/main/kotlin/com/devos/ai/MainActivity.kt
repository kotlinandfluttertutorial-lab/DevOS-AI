package com.devos.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devos.ai.designsystem.components.DevOSBottomBar
import com.devos.ai.designsystem.theme.DevOSTheme
import com.devos.ai.navigation.DevOSNavGraph
import com.devos.ai.navigation.DevOSRoutes
import dagger.hilt.android.AndroidEntryPoint

/** Routes that should NOT show bottom/rail navigation. */
private val NAV_HIDDEN_ROUTES = setOf(
    DevOSRoutes.SPLASH,
    DevOSRoutes.ONBOARDING,
    DevOSRoutes.LOGIN,
)

/** Primary tab destinations — same for both bottom bar and navigation rail. */
private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
)

private val PRIMARY_NAV_ITEMS = listOf(
    NavItem(DevOSRoutes.HOME,               "Home",     Icons.Outlined.Home,      "Home"),
    NavItem(DevOSRoutes.PROJECT_LIST,        "Projects", Icons.Outlined.FolderOpen,"Projects"),
    NavItem(DevOSRoutes.AI_CHAT,             "AI",       Icons.Outlined.SmartToy,  "AI Chat"),
    NavItem(DevOSRoutes.LEARNING_DASHBOARD,  "Learn",    Icons.Outlined.School,    "Learning"),
    NavItem(DevOSRoutes.MORE,                "More",     Icons.Outlined.MoreHoriz, "More"),
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DevOSTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

                DevOSApp(isCompact = isCompact)
            }
        }
    }

    /**
     * Called when a new intent arrives while the activity is already running (e.g. via singleTop
     * launch mode). Updating the intent ensures the NavHost deep link handler processes the
     * OAuth callback (devos://auth/callback?code=XXX&provider=github).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

/**
 * Root application composable that adapts its navigation chrome to the window size.
 *
 * - Compact width (phones): [DevOSBottomBar] shown below content.
 * - Medium/Expanded width (tablets): [NavigationRail] shown to the left of content.
 *
 * @param isCompact True when the screen width is in the Compact size class (<600dp).
 */
@Composable
fun DevOSApp(
    isCompact: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: DevOSRoutes.SPLASH
    val showNav = currentRoute !in NAV_HIDDEN_ROUTES

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(DevOSRoutes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (isCompact) {
        // ── Phone layout: bottom bar ──────────────────────────────────────────
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showNav) {
                    DevOSBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = ::navigateTo,
                    )
                }
            },
        ) { innerPadding ->
            DevOSNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    } else {
        // ── Tablet layout: navigation rail + content ──────────────────────────
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Row(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                if (showNav) {
                    DevOSNavigationRail(
                        currentRoute = currentRoute,
                        onNavigate = ::navigateTo,
                    )
                }
                DevOSNavGraph(
                    navController = navController,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * M3 [NavigationRail] with the same 5 primary tabs as [DevOSBottomBar].
 * Shown on medium/expanded screens (tablets ≥ 600dp).
 */
@Composable
private fun DevOSNavigationRail(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier.semantics { contentDescription = "Main navigation rail" },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        PRIMARY_NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.contentDescription,
                    )
                },
                // Icon-only NavigationRail — label omitted per spec (DEVOS-066)
                label = null,
                modifier = Modifier.semantics { contentDescription = item.contentDescription },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
