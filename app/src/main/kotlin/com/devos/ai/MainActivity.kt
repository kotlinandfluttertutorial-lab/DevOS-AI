package com.devos.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devos.ai.designsystem.components.DevOSBottomBar
import com.devos.ai.designsystem.theme.DevOSTheme
import com.devos.ai.navigation.DevOSNavGraph
import com.devos.ai.navigation.DevOSRoutes
import dagger.hilt.android.AndroidEntryPoint

/** Routes that should NOT show the bottom navigation bar. */
private val BOTTOM_BAR_HIDDEN_ROUTES = setOf(
    DevOSRoutes.SPLASH,
    DevOSRoutes.ONBOARDING,
    DevOSRoutes.LOGIN,
)

/** Routes that ARE primary tab destinations shown in the bottom bar. */
private val PRIMARY_TAB_ROUTES = setOf(
    DevOSRoutes.HOME,
    DevOSRoutes.PROJECT_LIST,
    DevOSRoutes.AI_CHAT,
    DevOSRoutes.LEARNING_DASHBOARD,
    DevOSRoutes.MORE,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DevOSTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: DevOSRoutes.SPLASH

                val showBottomBar = currentRoute !in BOTTOM_BAR_HIDDEN_ROUTES

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            DevOSBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        // Pop up to HOME to avoid building up a large back stack
                                        popUpTo(DevOSRoutes.HOME) { saveState = true }
                                        // Avoid multiple copies of the same destination
                                        launchSingleTop = true
                                        // Restore state when re-selecting a previously selected tab
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    },
                ) { innerPadding ->
                    DevOSNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
