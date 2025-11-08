package com.myvillagebus.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myvillagebus.BusScheduleApplication
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
    // Pobierz wersję aplikacji
    val context = LocalContext.current
    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    val schedulesCount by viewModel.allSchedules.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    
    // Pobierz updateInfo z ViewModel
    val updateInfo by viewModel.updateInfo.collectAsState()
    val isCheckingVersion by viewModel.isCheckingVersion.collectAsState()

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

                    // W Card "Informacje", po "Wersja danych":
                    val carrierVersions = remember {
                        val app = context.applicationContext as BusScheduleApplication
                        app.carrierVersionManager.getAllVersions()
                    }

                    if (carrierVersions.isNotEmpty()) {
                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Wersje przewoźników:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            carrierVersions.forEach { (carrier, version) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = carrier,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "v$version",  // ZMIENIONE: Dodano "v" przed liczbą
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
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

            // Synchronizacja
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
                val isError = status.contains("Błąd", ignoreCase = true) ||
                        status.contains("Brak połączenia", ignoreCase = true)

                val isSuccess = status.contains("Zsynchronizowano", ignoreCase = true) ||
                        status.contains("Wszystkie dane są aktualne", ignoreCase = true)

                val isInProgress = status.contains("Synchronizacja...", ignoreCase = true) ||
                        status.contains("Sprawdzanie", ignoreCase = true)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isSuccess -> MaterialTheme.colorScheme.primaryContainer
                            isInProgress -> MaterialTheme.colorScheme.tertiaryContainer
                            isError -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer  // Neutralny kolor
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when {
                                isSuccess -> "Sukces"
                                isInProgress -> "Trwa synchronizacja..."
                                isError -> "Błąd"
                                else -> "Informacja"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
                                isInProgress -> MaterialTheme.colorScheme.onTertiaryContainer
                                isError -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )

                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
                                isInProgress -> MaterialTheme.colorScheme.onTertiaryContainer
                                isError -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }
            }

            Text(
                text = "Ustawienia aktualizacji",
                style = MaterialTheme.typography.titleLarge
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatyczne sprawdzanie aktualizacji",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sprawdza dostępność nowych wersji przy każdym uruchomieniu aplikacji",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    var autoCheckEnabled by remember { mutableStateOf(viewModel.versionManager.isAutoCheckEnabled()) }

                    Switch(
                        checked = autoCheckEnabled,
                        onCheckedChange = { enabled ->
                            autoCheckEnabled = enabled
                            viewModel.versionManager.setAutoCheckEnabled(enabled)
                        }
                    )
                }
            }

            var hasManuallyChecked by remember { mutableStateOf(false) }

            // Sprawdź aktualizacje
            OutlinedButton(
                onClick = {
                    hasManuallyChecked = true  // ← DODAJ
                    viewModel.checkAppVersion(configUrl, manualCheck = true)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCheckingVersion
            ) {
                if (isCheckingVersion) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sprawdzanie...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sprawdź aktualizacje")
                }
            }

            // Wynik sprawdzenia (TYLKO jeśli użytkownik kliknął)
            val updateCheckResult = remember(updateInfo, isCheckingVersion, hasManuallyChecked) {
                // Pokaż TYLKO jeśli użytkownik kliknął przycisk I sprawdzanie zakończone
                if (!hasManuallyChecked || isCheckingVersion) {
                    null  // ← Nie pokazuj jeśli nie kliknął lub trwa sprawdzanie
                } else {
                    val info = updateInfo
                    when {
                        info == null -> "✅ Aplikacja jest aktualna"
                        info.isUpdateRequired -> "⚠️ Wymagana aktualizacja do wersji ${info.latestVersion}"
                        info.isUpdateAvailable -> "🔔 Dostępna wersja ${info.latestVersion}"
                        else -> null
                    }
                }
            }

            updateCheckResult?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            message.contains("✅") -> MaterialTheme.colorScheme.primaryContainer
                            message.contains("⚠️") -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    )
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        color = when {
                            message.contains("✅") -> MaterialTheme.colorScheme.onPrimaryContainer
                            message.contains("⚠️") -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        }
                    )
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
                }
            }

            HorizontalDivider()

            // NOWE: Karta Credits
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nagłówek
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
                            text = "O aplikacji",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    HorizontalDivider()

                    // Wersja aplikacji
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wersja:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = appVersion,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider()

                    // Opis projektu
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡 O projekcie",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Amatorski projekt stworzony w celu rozwijania umiejętności programowania " +
                                    "i jednocześnie pomagający załapać się na autobus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f
                        )
                    }

                    HorizontalDivider()

                    // Link do strony
                    /*
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/NotM3e/myVillageBus")
                                )
                                context.startActivity(intent)
                            },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🔗 GitHub",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "github.com/NotM3e/myVillageBus",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Otwórz stronę",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                     */
                    /*
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://twojastrona.pl")
                                )
                                context.startActivity(intent)
                            },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "🌐 Odwiedź moją stronę",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "twojastrona.pl",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Otwórz stronę",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    */

                    HorizontalDivider()

                    // Disclaimer
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⚠️ Zastrzeżenie",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Aplikacja nie jest odpowiedzialna za opóźnienia rozkładów. " +
                                    "Ma ona wyłącznie na celu przedstawienie rozkładów w jednym miejscu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "© 2025 - Projekt niekomercyjny",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                        )
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