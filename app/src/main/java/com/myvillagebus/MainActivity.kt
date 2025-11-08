package com.myvillagebus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.myvillagebus.navigation.NavGraph
import com.myvillagebus.ui.screens.UpdateNotifications
import com.myvillagebus.ui.theme.BusScheduleTheme
import com.myvillagebus.ui.viewmodel.BusViewModel
import com.myvillagebus.utils.UpdateInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: BusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sprawdź wersję w tle (jeśli włączone w ustawieniach I minęło 24h)
        lifecycleScope.launch {
            delay(1000)  // Opóźnienie 1s żeby UI się załadował

            if (viewModel.versionManager.shouldCheckForUpdates()) {
                val configUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vSUpEKaD5spMbQ0e_VVj2XI1pxlTbGz6QV5AEvD0HQIM-xDk1yzhWA3yo7zwPjJ8yq9anAJrixPn4WI/pub?gid=0&single=true&output=tsv"
                viewModel.checkAppVersion(configUrl, manualCheck = false)
                Log.d("MainActivity", "Auto-check wykonany (minęło ≥24h)")
            } else {
                Log.d("MainActivity", "Auto-check pominięty (włączony throttling lub wyłączony w ustawieniach)")
            }
        }

        setContent {
            BusScheduleTheme {
                val navController = rememberNavController()
                val updateInfo: UpdateInfo? by viewModel.updateInfo.collectAsState(initial = null)

                Box {
                    NavGraph(
                        navController = navController,
                        viewModel = viewModel
                    )

                    // Pokaż dialogi/snackbary aktualizacji
                    updateInfo?.let { info ->
                        UpdateNotifications(
                            updateInfo = info,
                            onDownload = { openDownloadUrl(info.downloadUrl) }
                        )
                    }
                }
            }
        }
    }

    private fun openDownloadUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Nie można otworzyć URL: $url", e)
        }
    }
}