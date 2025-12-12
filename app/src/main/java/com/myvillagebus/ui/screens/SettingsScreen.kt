package com.myvillagebus.ui.screens

import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myvillagebus.ui.viewmodel.BusViewModel
import com.myvillagebus.utils.AppConstants

/**
 * Ekran ustawień aplikacji
 * Zawiera: przeglądarkę rozkładów, sprawdzanie aktualizacji, zarządzanie danymi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BusViewModel,
    onBackClick: () -> Unit,
    onNavigateToBrowser: () -> Unit = {}  // Callback do nawigacji do przeglądarki
) {
    // Odśwież metadane synchronizacji przy wejściu na ekran
    LaunchedEffect(Unit) {
        viewModel.refreshSyncInfo()
    }

    val context = LocalContext.current

    // Pobierz wersję aplikacji z manifestu
    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    // State flows z ViewModel
    val updateInfo by viewModel.updateInfo.collectAsState()
    val isCheckingVersion by viewModel.isCheckingVersion.collectAsState()

    // Lokalne stany dialogów
    var showDeleteDialog by remember { mutableStateOf(false) }
    var hasManuallyChecked by remember { mutableStateOf(false) }

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
            // ========== SEKCJA: Synchronizacja ==========
            Text(
                text = "Synchronizacja",
                style = MaterialTheme.typography.titleLarge
            )

            // Przycisk: Przeglądarka rozkładów
            Button(
                onClick = onNavigateToBrowser,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Przeglądarka rozkładów")
            }

            // ========== SEKCJA: Ustawienia aktualizacji ==========
            Text(
                text = "Ustawienia aktualizacji",
                style = MaterialTheme.typography.titleLarge
            )

            // Karta: Auto-check toggle
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
                            text = "Sprawdza dostępność nowych wersji co 24h przy uruchomieniu aplikacji",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    // Switch kontrolujący auto-check
                    var autoCheckEnabled by remember {
                        mutableStateOf(viewModel.versionManager.isAutoCheckEnabled())
                    }

                    Switch(
                        checked = autoCheckEnabled,
                        onCheckedChange = { enabled ->
                            autoCheckEnabled = enabled
                            viewModel.versionManager.setAutoCheckEnabled(enabled)
                        }
                    )
                }
            }

            // Przycisk: Ręczne sprawdzenie aktualizacji
            OutlinedButton(
                onClick = {
                    hasManuallyChecked = true
                    viewModel.checkAppVersion(AppConstants.CONFIG_URL, manualCheck = true)
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

            // Karta: Wynik sprawdzenia (pokazuj TYLKO po ręcznym kliknięciu)
            val updateCheckResult = remember(updateInfo, isCheckingVersion, hasManuallyChecked) {
                if (!hasManuallyChecked || isCheckingVersion) {
                    null
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

            // ========== SEKCJA: Zarządzanie danymi ==========
            Text(
                text = "Zarządzanie danymi",
                style = MaterialTheme.typography.titleLarge
            )

            // Przycisk: Usuń wszystkie rozkłady
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

            HorizontalDivider()

            // ========== SEKCJA: O aplikacji ==========
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                    // Link do GitHub
                    // *
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
                    // */

                    //* Linki
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://wsiobus.pl")
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
                                    text = "🌐 Strona internetowa projektu",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "wsiobus.pl",
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
                    // */

                    // Disclaimer
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

    // ========== DIALOGI ==========

    // Dialog: Potwierdzenie usunięcia wszystkich rozkładów
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Usuń wszystkie rozkłady?") },
            text = {
                Text(
                    "Ta operacja jest nieodwracalna. Wszystkie dane zostaną usunięte z urządzenia. " +
                            "Możesz je ponownie pobrać przez Przeglądarkę rozkładów."
                )
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
}