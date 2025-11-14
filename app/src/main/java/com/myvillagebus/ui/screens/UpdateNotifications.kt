// 📁 ui/screens/UpdateNotifications.kt

package com.myvillagebus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myvillagebus.utils.UpdateInfo
import kotlinx.coroutines.delay

/**
 * Komponent wyświetlający powiadomienia o aktualizacji
 */
@Composable
fun UpdateNotifications(
    updateInfo: UpdateInfo,
    onDownload: () -> Unit
) {
    when {
        updateInfo.isUpdateRequired -> {
            ForceUpdateDialog(
                updateInfo = updateInfo,
                onDownload = onDownload
            )
        }
        updateInfo.isUpdateAvailable -> {
            UpdateAvailableSnackbar(
                updateInfo = updateInfo,
                onDownload = onDownload
            )
        }
    }
}

/**
 * Dialog wymagający aktualizacji (można zamknąć, ale pokazuje się przy każdym starcie)
 */
@Composable
fun ForceUpdateDialog(
    updateInfo: UpdateInfo,
    onDownload: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("⚠️ Nieaktualna wersja")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Twoja wersja: ${updateInfo.currentVersion}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Minimalna wymagana: ${updateInfo.minVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Najnowsza dostępna: ${updateInfo.latestVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider()

                    Text(
                        text = updateInfo.updateMessage
                            ?: "Zaktualizuj aplikację aby móc pobierać nowe rozkłady.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDownload()
                        showDialog = false
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pobierz aktualizację")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text("Zamknij")
                }
            }
        )
    }
}

/**
 * Snackbar z informacją o dostępnej aktualizacji (auto-hide, swipeable)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableSnackbar(
    updateInfo: UpdateInfo,
    onDownload: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    //Użyj timestamp jako klucz (każdy check = nowy timestamp = nowy Snackbar)
    LaunchedEffect(updateInfo.timestamp) {
        val result = snackbarHostState.showSnackbar(
            message = "🔔 Dostępna wersja ${updateInfo.latestVersion}",
            actionLabel = "Pobierz",
            duration = SnackbarDuration.Long,
            withDismissAction = true
        )

        when (result) {
            SnackbarResult.ActionPerformed -> {
                onDownload()
            }
            SnackbarResult.Dismissed -> {
                // Swipe = ukryj na tę sesję
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(hostState = snackbarHostState)
    }
}
