package com.myvillagebus.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.myvillagebus.ui.screens.CarrierBrowserScreen
import com.myvillagebus.ui.screens.ProfileManagementScreen
import com.myvillagebus.ui.screens.ScheduleDetailScreen
import com.myvillagebus.ui.screens.ScheduleListScreen
import com.myvillagebus.ui.screens.SettingsScreen
import com.myvillagebus.ui.viewmodel.BusViewModel
import kotlinx.coroutines.runBlocking

sealed class Screen(val route: String) {
    object ScheduleList : Screen("schedule_list")
    object ScheduleDetail : Screen("schedule_detail/{scheduleId}") {
        fun createRoute(scheduleId: Int) = "schedule_detail/$scheduleId"
    }
    object Settings : Screen("settings")
    object CarrierBrowser : Screen("carrier_browser")
    object ProfileManagement : Screen("profile_management")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: BusViewModel
) {
    // ✅ GLOBAL navigation lock (shared across all screens)
    var lastNavigationTime by remember { mutableStateOf(0L) }
    val navigationDebounceMs = 500L

    // Safe navigation wrapper
    val safeNavigate: (String) -> Unit = remember {
        { route ->
            val currentTime = System.currentTimeMillis()
            val timeSinceLastNav = currentTime - lastNavigationTime

            if (timeSinceLastNav >= navigationDebounceMs) {
                Log.d("NavGraph", "✅ Navigation allowed to: $route (${timeSinceLastNav}ms since last)")
                lastNavigationTime = currentTime
                navController.navigate(route)
            } else {
                Log.d("NavGraph", "⚠️ Navigation BLOCKED to: $route (only ${timeSinceLastNav}ms since last, need ${navigationDebounceMs}ms)")
            }
        }
    }

    // Safe back navigation wrapper
    val safePopBackStack: () -> Unit = remember {
        {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastNav = currentTime - lastNavigationTime

            if (timeSinceLastNav >= navigationDebounceMs) {
                Log.d("NavGraph", "✅ Pop back allowed (${timeSinceLastNav}ms since last)")
                lastNavigationTime = currentTime

                // Additional safety: check if can pop
                if (navController.currentBackStackEntry != null &&
                    navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    Log.d("NavGraph", "⚠️ Cannot pop - already at start destination")
                }
            } else {
                Log.d("NavGraph", "⚠️ Pop back BLOCKED (only ${timeSinceLastNav}ms since last, need ${navigationDebounceMs}ms)")
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.ScheduleList.route
    ) {
        composable(Screen.ScheduleList.route) {
            ScheduleListScreen(
                viewModel = viewModel,
                onScheduleClick = { schedule ->
                    safeNavigate(Screen.ScheduleDetail.createRoute(schedule.id))
                },
                onSettingsClick = {
                    safeNavigate(Screen.Settings.route)
                },
                onNavigateToBrowser = {
                    safeNavigate(Screen.CarrierBrowser.route)
                },
                onNavigateToProfileManagement = {
                    safeNavigate(Screen.ProfileManagement.route)
                }
            )
        }

        composable(
            route = Screen.ScheduleDetail.route,
            arguments = listOf(navArgument("scheduleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getInt("scheduleId")
            val schedule = scheduleId?.let { id ->
                runBlocking {
                    viewModel.getScheduleById(id)
                }
            }

            schedule?.let {
                ScheduleDetailScreen(
                    schedule = it,
                    highlightedStops = viewModel.currentHighlightedStops.value,
                    selectedDay = viewModel.currentSelectedDay.value,
                    onBackClick = safePopBackStack
                )
            }
        }

        composable(Screen.CarrierBrowser.route) {
            CarrierBrowserScreen(
                viewModel = viewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.ProfileManagement.route) {
            ProfileManagementScreen(
                viewModel = viewModel,
                onBackClick = safePopBackStack
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = safePopBackStack,
                onNavigateToBrowser = {
                    safeNavigate(Screen.CarrierBrowser.route)
                }
            )
        }
    }
}