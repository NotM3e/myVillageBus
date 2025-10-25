package com.myvillagebus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.myvillagebus.ui.screens.ScheduleDetailScreen
import com.myvillagebus.ui.screens.ScheduleListScreen
import com.myvillagebus.ui.viewmodel.BusViewModel
import kotlinx.coroutines.runBlocking

sealed class Screen(val route: String) {
    object ScheduleList : Screen("schedule_list")
    object ScheduleDetail : Screen("schedule_detail/{scheduleId}") {
        fun createRoute(scheduleId: Int) = "schedule_detail/$scheduleId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: BusViewModel  // ← Dodany ViewModel
) {
    // Pobierz rozkłady z ViewModelu
    val schedules by viewModel.allSchedules.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.ScheduleList.route
    ) {
        composable(Screen.ScheduleList.route) {
            ScheduleListScreen(
                schedules = schedules,  // ← Z Room Database
                onScheduleClick = { schedule ->
                    navController.navigate(Screen.ScheduleDetail.createRoute(schedule.id))
                }
            )
        }

        composable(
            route = Screen.ScheduleDetail.route,
            arguments = listOf(navArgument("scheduleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getInt("scheduleId")

            // Pobierz rozkład z bazy danych
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
    }
}