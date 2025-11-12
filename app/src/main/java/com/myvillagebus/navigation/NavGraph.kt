package com.myvillagebus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val schedules by viewModel.allSchedules.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.ScheduleList.route
    ) {
        composable(Screen.ScheduleList.route) {
            ScheduleListScreen(
                schedules = schedules,
                viewModel = viewModel,
                onScheduleClick = { schedule ->
                    navController.navigate(Screen.ScheduleDetail.createRoute(schedule.id))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToBrowser = {
                    navController.navigate(Screen.CarrierBrowser.route)
                },
                onNavigateToProfileManagement = {
                    navController.navigate(Screen.ProfileManagement.route)
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
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.CarrierBrowser.route) {
            CarrierBrowserScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.ProfileManagement.route) {
            ProfileManagementScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToBrowser = { navController.navigate(Screen.CarrierBrowser.route) }
            )
        }
    }
}