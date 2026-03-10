package com.openplaato.keg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openplaato.keg.ui.navigation.BottomNavBar
import com.openplaato.keg.ui.navigation.Screen
import com.openplaato.keg.ui.screens.airlocks.AirlocksScreen
import com.openplaato.keg.ui.screens.beverages.BeverageEditScreen
import com.openplaato.keg.ui.screens.beverages.BeveragesScreen
import com.openplaato.keg.ui.screens.settings.SettingsScreen
import com.openplaato.keg.ui.screens.tapedit.TapEditScreen
import com.openplaato.keg.ui.screens.taplist.TapListScreen
import com.openplaato.keg.ui.screens.taplist.TapListViewModel
import com.openplaato.keg.ui.theme.OpenPlaatoKegTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenPlaatoKegTheme {
                val navController = rememberNavController()
                // Shared ViewModel that manages WebSocket connection for the whole app
                val tapListVm: TapListViewModel = hiltViewModel()
                val serverUrl by tapListVm.serverUrl.collectAsState(initial = "")

                LaunchedEffect(serverUrl) {
                    if (serverUrl.isNotBlank()) tapListVm.connectWebSocket(serverUrl)
                }

                Scaffold(
                    bottomBar = { BottomNavBar(navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.TapList.route,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(Screen.TapList.route) {
                            TapListScreen(
                                viewModel = tapListVm,
                                onEditTap = { tap ->
                                    navController.navigate(Screen.TapEdit.route(tap.id))
                                },
                                onNewTap = { navController.navigate(Screen.NewTap.route) },
                            )
                        }
                        composable(Screen.Airlocks.route) {
                            AirlocksScreen(wsEvents = tapListVm.wsEvents)
                        }
                        composable(Screen.Beverages.route) {
                            BeveragesScreen(
                                onEdit = { bev ->
                                    navController.navigate(Screen.BeverageEdit.route(bev.id))
                                },
                                onNew = { navController.navigate(Screen.NewBeverage.route) },
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen()
                        }
                        composable(
                            route = Screen.TapEdit.route,
                            arguments = listOf(
                                androidx.navigation.navArgument(Screen.TapEdit.ARG) {
                                    type = androidx.navigation.NavType.StringType
                                }
                            )
                        ) { backStack ->
                            val tapId = backStack.arguments?.getString(Screen.TapEdit.ARG) ?: return@composable
                            TapEditScreen(
                                tapId = tapId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(Screen.NewTap.route) {
                            TapEditScreen(
                                tapId = "new",
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(
                            route = Screen.BeverageEdit.route,
                            arguments = listOf(
                                androidx.navigation.navArgument(Screen.BeverageEdit.ARG) {
                                    type = androidx.navigation.NavType.StringType
                                }
                            )
                        ) { backStack ->
                            val bevId = backStack.arguments?.getString(Screen.BeverageEdit.ARG) ?: return@composable
                            BeverageEditScreen(
                                bevId = bevId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(Screen.NewBeverage.route) {
                            BeverageEditScreen(
                                bevId = "new",
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
