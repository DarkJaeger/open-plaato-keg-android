package com.openplaato.keg.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object TapList : Screen("taplist", "Taps", Icons.Default.LocalBar)
    data object Scales : Screen("scales", "Kegs", Icons.Default.Liquor)
    data object Airlocks : Screen("airlocks", "Airlocks", Icons.Default.Opacity)
    data object Beverages : Screen("beverages", "Beers", Icons.Default.SportsBar)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Detail screens (not in bottom nav)
    data object TapEdit : Screen("tapedit/{tapId}", "Edit Tap", Icons.Default.LocalBar) {
        fun route(tapId: String) = "tapedit/$tapId"
        const val ARG = "tapId"
    }
    data object NewTap : Screen("tapedit/new", "New Tap", Icons.Default.LocalBar)
    data object BeverageEdit : Screen("beverage/{bevId}", "Edit Beverage", Icons.Default.SportsBar) {
        fun route(bevId: String) = "beverage/$bevId"
        const val ARG = "bevId"
    }
    data object NewBeverage : Screen("beverage/new", "New Beverage", Icons.Default.SportsBar)
    data object ScaleConfig : Screen("scaleconfig/{kegId}", "Scale Config", Icons.Default.Liquor) {
        fun route(kegId: String) = "scaleconfig/$kegId"
        const val ARG = "kegId"
    }
    data object AirlockSetup : Screen("airlocksetup", "Airlock Setup", Icons.Default.Opacity)
    data object BrewfatherBatches : Screen("brewfather/batches", "Brewfather Batches", Icons.Default.SportsBar)
    data object Transfer : Screen("transfer", "Transfer", Icons.Default.Scale)
    data object TransferScaleConfig : Screen("transferscaleconfig/{scaleId}", "Transfer Scale Config", Icons.Default.Scale) {
        fun route(scaleId: String) = "transferscaleconfig/$scaleId"
        const val ARG = "scaleId"
    }
    data object Onboarding : Screen("onboarding", "Setup Guide", Icons.Default.Settings)
}

val bottomNavScreens = listOf(
    Screen.TapList,
    Screen.Scales,
    Screen.Airlocks,
    Screen.Transfer,
    Screen.Beverages,
    Screen.Settings,
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        bottomNavScreens.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(Screen.TapList.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = {
                    Text(
                        text = screen.label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
