package com.openplaato.keg

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openplaato.keg.data.preferences.AppPreferences
import com.openplaato.keg.ui.navigation.BottomNavBar
import com.openplaato.keg.ui.navigation.Screen
import com.openplaato.keg.ui.screens.airlocks.AirlocksScreen
import com.openplaato.keg.ui.screens.transfer.TransferScreen
import com.openplaato.keg.ui.screens.transfer.TransferScaleConfigScreen
import com.openplaato.keg.ui.screens.airlocks.AirlockSetupScreen
import com.openplaato.keg.ui.screens.beverages.BeverageEditScreen
import com.openplaato.keg.ui.screens.beverages.BeveragesScreen
import com.openplaato.keg.ui.screens.onboarding.OnboardingScreen
import com.openplaato.keg.ui.screens.history.HistoryScreen
import com.openplaato.keg.ui.screens.scales.ScaleConfigScreen
import com.openplaato.keg.ui.screens.scales.ScalesScreen
import com.openplaato.keg.ui.screens.settings.BrewfatherBatchScreen
import com.openplaato.keg.ui.screens.settings.SettingsScreen
import com.openplaato.keg.ui.screens.tapedit.TapEditScreen
import com.openplaato.keg.ui.screens.taplist.TapListScreen
import com.openplaato.keg.ui.screens.taplist.TapListViewModel
import com.openplaato.keg.ui.theme.OpenPlaatoKegTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appPrefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_OpenPlaatoKeg)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenPlaatoKegTheme {
                val navController = rememberNavController()
                val tapListVm: TapListViewModel = hiltViewModel()
                val serverUrl by tapListVm.serverUrl.collectAsState(initial = "")
                val hasSeenOnboarding by appPrefs.hasSeenOnboarding.collectAsState(initial = null)

                val notificationPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) {}
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                LaunchedEffect(serverUrl) {
                    if (serverUrl.isNotBlank()) tapListVm.connectWebSocket(serverUrl)
                }

                if (hasSeenOnboarding == null) {
                    Box(modifier = Modifier.fillMaxSize())
                    return@OpenPlaatoKegTheme
                }

                val startDestination = if (hasSeenOnboarding == true) Screen.TapList.route else Screen.Onboarding.route

                Scaffold(
                    bottomBar = { BottomNavBar(navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(onFinish = {
                                navController.navigate(Screen.TapList.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            })
                        }
                        composable(Screen.TapList.route) {
                            TapListScreen(
                                viewModel = tapListVm,
                                onEditTap = { tap -> navController.navigate(Screen.TapEdit.route(tap.id)) },
                                onNewTap = { navController.navigate(Screen.NewTap.route) },
                            )
                        }
                        composable(Screen.Scales.route) {
                            ScalesScreen(
                                onConfigureScale = { kegId ->
                                    navController.navigate(Screen.ScaleConfig.route(kegId))
                                },
                                onShowHistory = { id, title ->
                                    navController.navigate(Screen.History.route(id, "keg", title))
                                }
                            )
                        }
                        composable(Screen.Airlocks.route) {
                            AirlocksScreen(
                                wsEvents = tapListVm.wsEvents,
                                onShowHistory = { id, title ->
                                    navController.navigate(Screen.History.route(id, "airlock", title))
                                }
                            )
                        }
                        composable(Screen.Transfer.route) {
                            TransferScreen(
                                onConfigureTransferScale = { scaleId ->
                                    navController.navigate(Screen.TransferScaleConfig.route(scaleId))
                                },
                            )
                        }
                        composable(Screen.Beverages.route) {
                            BeveragesScreen(
                                onEdit = { bev -> navController.navigate(Screen.BeverageEdit.route(bev.id)) },
                                onNew = { navController.navigate(Screen.NewBeverage.route) },
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onSetupAirlocks = { navController.navigate(Screen.AirlockSetup.route) },
                                onBrowseBrewfatherBatches = { navController.navigate(Screen.BrewfatherBatches.route) },
                                onOpenGuide = { navController.navigate(Screen.Onboarding.route) },
                            )
                        }

                        // ── Detail screens ──────────────────────────────
                        composable(
                            route = Screen.TapEdit.route,
                            arguments = listOf(navArgument(Screen.TapEdit.ARG) { type = NavType.StringType }),
                        ) { backStack ->
                            val tapId = backStack.arguments?.getString(Screen.TapEdit.ARG) ?: return@composable
                            TapEditScreen(tapId = tapId, onBack = { navController.popBackStack() })
                        }
                        composable(Screen.NewTap.route) {
                            TapEditScreen(tapId = "new", onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = Screen.BeverageEdit.route,
                            arguments = listOf(navArgument(Screen.BeverageEdit.ARG) { type = NavType.StringType }),
                        ) { backStack ->
                            val bevId = backStack.arguments?.getString(Screen.BeverageEdit.ARG) ?: return@composable
                            BeverageEditScreen(bevId = bevId, onBack = { navController.popBackStack() })
                        }
                        composable(Screen.NewBeverage.route) {
                            BeverageEditScreen(bevId = "new", onBack = { navController.popBackStack() })
                        }
                        composable(Screen.AirlockSetup.route) {
                            AirlockSetupScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.BrewfatherBatches.route) {
                            BrewfatherBatchScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = Screen.ScaleConfig.route,
                            arguments = listOf(navArgument(Screen.ScaleConfig.ARG) { type = NavType.StringType }),
                        ) {
                            ScaleConfigScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = Screen.TransferScaleConfig.route,
                            arguments = listOf(navArgument(Screen.TransferScaleConfig.ARG) { type = NavType.StringType }),
                        ) {
                            TransferScaleConfigScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = Screen.History.route,
                            arguments = listOf(
                                navArgument(Screen.History.ID_ARG) { type = NavType.StringType },
                                navArgument(Screen.History.TYPE_ARG) { type = NavType.StringType },
                                navArgument(Screen.History.TITLE_ARG) { type = NavType.StringType },
                            ),
                        ) { backStack ->
                            val id = backStack.arguments?.getString(Screen.History.ID_ARG) ?: ""
                            val type = backStack.arguments?.getString(Screen.History.TYPE_ARG) ?: ""
                            val title = backStack.arguments?.getString(Screen.History.TITLE_ARG) ?: ""
                            HistoryScreen(title = title, id = id, type = type, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
