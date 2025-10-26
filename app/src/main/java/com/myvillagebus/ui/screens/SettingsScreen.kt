package com.myvillagebus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myvillagebus.ui.viewmodel.BusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BusViewModel,
    onBackClick: () -> Unit
) {
    // Odśwież dane przy wejściu na ekran
    LaunchedEffect(Unit) {
        viewModel.refreshSyncInfo()
    }

    val schedulesCount by viewModel.allSchedules.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showForceSyncDialog by remember { mutableStateOf(false) }

    val configUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vSUpEKaD5spMbQ0e_VVj2XI1pxlTbGz6QV5AEvD0HQIM-xDk1yzhWA3yo7zwPjJ8yq9anAJrixPn4WI/pub?gid=0&single=true&output=tsv"

    val lastSyncVersion by viewModel.lastSyncVersion.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val hoursSinceLastSync by viewModel.hoursSinceLastSync.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Informacje",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Liczba rozkładów:")
                        Text(
                            text = schedulesCount.size.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Przewoźnicy:")
                        Text(
                            text = schedulesCount.map { it.carrierName }.distinct().size.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    lastSyncVersion?.let { version ->
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Wersja danych:")
                            Text(
                                text = version,  // OK: zmienna lokalna
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    lastSyncTime?.let { time ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ostatnia synchronizacja:")
                            Text(
                                text = time,  // OK: zmienna lokalna
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (hoursSinceLastSync < Long.MAX_VALUE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Czas od synchronizacji:")
                                Text(
                                    text = when {
                                        hoursSinceLastSync < 1 -> "Mniej niż godzinę temu"
                                        hoursSinceLastSync < 24 -> "$hoursSinceLastSync godz. temu"
                                        else -> "${hoursSinceLastSync / 24} dni temu"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        hoursSinceLastSync > 168 -> MaterialTheme.colorScheme.error
                                        hoursSinceLastSync > 72 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Synchronizacja",
                style = MaterialTheme.typography.titleLarge
            )

            Button(
                onClick = { viewModel.syncWithGoogleSheets(configUrl, forceSync = false) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Synchronizacja...")
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Synchronizuj rozkłady")
                }
            }

            // Pokaż przycisk tylko gdy są dane w bazie I jest wersja
            if (schedulesCount.isNotEmpty() && lastSyncVersion != null) {
                OutlinedButton(
                    onClick = { showForceSyncDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncing
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Wymuś ponowne pobranie")
                }
            }

            syncStatus?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            status.contains("Zsynchronizowano") -> MaterialTheme.colorScheme.primaryContainer
                            status.contains("Dane są już aktualne") -> MaterialTheme.colorScheme.secondaryContainer
                            status.contains("Synchronizacja...") || status.contains("Sprawdzanie") -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when {
                                status.contains("Zsynchronizowano") -> "Sukces"
                                status.contains("Dane są już aktualne") -> "Informacja"
                                status.contains("Synchronizacja...") || status.contains("Sprawdzanie") -> "Trwa synchronizacja..."
                                else -> "Błąd"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                status.contains("Zsynchronizowano") -> MaterialTheme.colorScheme.onPrimaryContainer
                                status.contains("Dane są już aktualne") -> MaterialTheme.colorScheme.onSecondaryContainer
                                status.contains("Synchronizacja...") || status.contains("Sprawdzanie") -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onErrorContainer
                            }
                        )

                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                status.contains("Zsynchronizowano") -> MaterialTheme.colorScheme.onPrimaryContainer
                                status.contains("Dane są już aktualne") -> MaterialTheme.colorScheme.onSecondaryContainer
                                status.contains("Synchronizacja...") || status.contains("Sprawdzanie") -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Zarządzanie danymi",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Usuń wszystkie rozkłady")
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Jak synchronizować?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "1. Upewnij się że masz połączenie z internetem\n" +
                                "2. Kliknij 'Synchronizuj rozkłady'\n" +
                                "3. Poczekaj na pobranie danych\n" +
                                "4. Gotowe! Rozkłady są aktualne",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Pokaż sekcję synchronizacji tylko gdy są dane
                    if (lastSyncVersion != null || lastSyncTime != null) {
                        HorizontalDivider()

                        lastSyncVersion?.let { version ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Wersja danych:")
                                Text(
                                    text = version,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        lastSyncTime?.let { time ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ostatnia synchronizacja:")
                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (hoursSinceLastSync < Long.MAX_VALUE) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Czas od synchronizacji:")
                                    Text(
                                        text = when {
                                            hoursSinceLastSync < 1 -> "Mniej niż godzinę temu"
                                            hoursSinceLastSync < 24 -> "$hoursSinceLastSync godz. temu"
                                            else -> "${hoursSinceLastSync / 24} dni temu"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            hoursSinceLastSync > 168 -> MaterialTheme.colorScheme.error
                                            hoursSinceLastSync > 72 -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Usuń wszystkie rozkłady?") },
            text = {
                Text("Ta operacja jest nieodwracalna. Wszystkie dane zostaną usunięte z urządzenia. " +
                        "Możesz je ponownie pobrać przez synchronizację.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllSchedules()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    if (showForceSyncDialog) {
        AlertDialog(
            onDismissRequest = { showForceSyncDialog = false },
            title = { Text("Wymusić synchronizację?") },
            text = {
                Text("Dane zostaną pobrane ponownie nawet jeśli są aktualne. " +
                        "Ta opcja jest przydatna gdy wystąpił błąd w poprzedniej synchronizacji.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.syncWithGoogleSheets(configUrl, forceSync = true)
                        showForceSyncDialog = false
                    }
                ) {
                    Text("Wymuś pobranie")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForceSyncDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}