package com.myvillagebus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.myvillagebus.navigation.NavGraph
import com.myvillagebus.ui.theme.BusScheduleTheme
import com.myvillagebus.ui.viewmodel.BusViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BusScheduleTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}