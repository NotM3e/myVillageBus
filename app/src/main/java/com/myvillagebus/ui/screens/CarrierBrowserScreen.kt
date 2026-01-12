package com.myvillagebus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myvillagebus.ui.model.CarrierUiModel
import com.myvillagebus.ui.viewmodel.BusViewModel
import com.myvillagebus.ui.components.CarrierCardPlaceholder
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import kotlinx.coroutines.delay

enum class BrowserTab {
    ALL, DOWNLOADED, AVAILABLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarrierBrowserScreen(
    viewModel: BusViewModel,
    onBackClick: () -> Unit
) {
    // States
    val availableCarriers by viewModel.availableCarriers.collectAsState()
    val downloadedCarriers by viewModel.downloadedCarriers.collectAsState()
    val isLoading by viewModel.isLoadingCarriers.collectAsState()
    val operationStatus by viewModel.carrierOperationStatus.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(BrowserTab.ALL) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Load carriers on first composition
    LaunchedEffect(Unit) {
        viewModel.loadAvailableCarriers()
    }

    // Show operation status in snackbar
    LaunchedEffect(operationStatus) {
        operationStatus?.let { status ->
            snackbarHostState.showSnackbar(
                message = status,
                duration = SnackbarDuration.Short
            )
            viewModel.clearCarrierStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Przeglądarka rozkładów") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAvailableCarriers() }) {
                        Icon(Icons.Default.Refresh, "Odśwież")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (downloadedCarriers.isNotEmpty()) {
                var showFabMenu by remember { mutableStateOf(false) }

                Box {
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.MoreVert, "Więcej")
                    }

                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        // Aktualizuj wszystkie
                        val hasUpdates = downloadedCarriers.any { it.hasUpdate }

                        DropdownMenuItem(
                            text = {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            null,
                                            Modifier.size(20.dp),
                                            tint = if (hasUpdates)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                        Text("Aktualizuj wszystkie")
                                    }
                                    if (!hasUpdates) {
                                        Text(
                                            text = "(Brak dostępnych)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 28.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.updateAllCarriers()
                                showFabMenu = false
                            },
                            enabled = hasUpdates
                        )

                        // Usuń wszystkie
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        "Usuń wszystkie",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            onClick = {
                                showDeleteAllDialog = true
                                showFabMenu = false
                            }
                        )

                        HorizontalDivider()

                        // Statystyki (disabled item)
                        DropdownMenuItem(
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "📊 Statystyki",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Przewoźnicy: ${downloadedCarriers.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Rozkłady: ${downloadedCarriers.sumOf { it.scheduleCount }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val updatesCount = downloadedCarriers.count { it.hasUpdate }
                                    Text(
                                        text = "Dostępne aktualizacje: $updatesCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (updatesCount > 0)
                                            MaterialTheme.colorScheme.tertiary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = { /* Nic - menu pozostaje otwarte */ },
                            enabled = false
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(16.dp)
            )

            // Offline Banner
            val isOfflineMode = remember(downloadedCarriers, availableCarriers) {
                downloadedCarriers.isNotEmpty() &&
                        downloadedCarriers.all { it.remoteVersion == null }
            }

            AnimatedVisibility(
                visible = isOfflineMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "📴 Tryb offline",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Pokazano ${downloadedCarriers.size} ${
                                    when {
                                        downloadedCarriers.size == 1 -> "pobranego przewoźnika"
                                        downloadedCarriers.size < 5 -> "pobranych przewoźników"
                                        else -> "pobranych przewoźników"
                                    }
                                }. Połącz się z internetem aby sprawdzić aktualizacje.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Tab Row
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == BrowserTab.ALL,
                    onClick = { selectedTab = BrowserTab.ALL },
                    text = { Text("Wszystkie (${availableCarriers.size})") }
                )
                Tab(
                    selected = selectedTab == BrowserTab.DOWNLOADED,
                    onClick = { selectedTab = BrowserTab.DOWNLOADED },
                    text = { Text("Pobrane (${downloadedCarriers.size})") }
                )
                Tab(
                    selected = selectedTab == BrowserTab.AVAILABLE,
                    onClick = { selectedTab = BrowserTab.AVAILABLE },
                    text = {
                        val availableCount = availableCarriers.count { !it.isDownloaded }
                        Text("Dostępne ($availableCount)")
                    }
                )
            }

            // Carrier List
            if (isLoading && availableCarriers.isEmpty()) {
                // Placeholder skeleton zamiast CircularProgressIndicator
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) {
                        CarrierCardPlaceholder()
                    }
                }
            } else {
                val filteredCarriers = when (selectedTab) {
                    BrowserTab.ALL -> availableCarriers
                    BrowserTab.DOWNLOADED -> downloadedCarriers
                    BrowserTab.AVAILABLE -> availableCarriers.filter { !it.isDownloaded }
                }.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.description?.contains(searchQuery, ignoreCase = true) == true
                }

                if (filteredCarriers.isEmpty()) {
                    EmptyState(selectedTab, searchQuery)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredCarriers, key = { it.id }) { carrier ->
                            CarrierCard(
                                carrier = carrier,
                                operationStatus = operationStatus,
                                onDownload = { viewModel.downloadCarrier(it) },
                                onUpdate = { viewModel.updateCarrier(it) },
                                onDelete = { viewModel.deleteCarrier(it) },
                                onRollback = { viewModel.rollbackCarrier(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete All Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Usuń wszystkich przewoźników?") },
            text = { Text("Ta operacja usunie wszystkie pobrane rozkłady. Będziesz musiał pobrać je ponownie.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllCarriers()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Szukaj przewoźnika...") },
        leadingIcon = {
            Icon(Icons.Default.Search, "Szukaj")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Wyczyść")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun EmptyState(
    selectedTab: BrowserTab,
    searchQuery: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = when {
                    searchQuery.isNotEmpty() -> "🔍"
                    selectedTab == BrowserTab.DOWNLOADED -> "📦"
                    else -> "🚌"
                },
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = when {
                    searchQuery.isNotEmpty() -> "Brak wyników"
                    selectedTab == BrowserTab.DOWNLOADED -> "Brak pobranych przewoźników"
                    selectedTab == BrowserTab.AVAILABLE -> "Brak dostępnych przewoźników"
                    else -> "Brak przewoźników"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = when {
                    searchQuery.isNotEmpty() -> "Spróbuj innej frazy"
                    selectedTab == BrowserTab.DOWNLOADED -> "Pobierz rozkłady z zakładki 'Dostępne'"
                    else -> "Sprawdź połączenie z internetem"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarrierCard(
    carrier: CarrierUiModel,
    operationStatus: String?,
    onDownload: (String) -> Unit,
    onUpdate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRollback: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isOperating by remember { mutableStateOf(false) }

    // Reset loading gdy pojawi się status operacji (błąd lub sukces)
    LaunchedEffect(operationStatus) {
        if (operationStatus != null &&
            (operationStatus.contains("Błąd", ignoreCase = true) ||
                    operationStatus.contains("Zaktualizowano", ignoreCase = true) ||
                    operationStatus.contains("Pobrano", ignoreCase = true) ||
                    operationStatus.contains("Usunięto", ignoreCase = true))) {
            isOperating = false
        }
    }

    // Safety timeout - auto-reset po 10s
    LaunchedEffect(isOperating) {
        if (isOperating) {
            delay(10000)
            isOperating = false
        }
    }


    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (carrier.isDownloaded)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = carrier.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    carrier.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                StatusBadge(
                    isDownloaded = carrier.isDownloaded,
                    hasUpdate = carrier.hasUpdate,
                    isOffline = carrier.isDownloaded && carrier.remoteVersion == null
                )
            }

            HorizontalDivider()

            // ← ZMIENIONY LAYOUT: Info po lewej, przyciski po prawej
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Section (lewa strona)
                Column(modifier = Modifier.weight(1f)) {
                    if (carrier.isDownloaded) {
                        InfoRow("Wersja:", "v${carrier.currentVersion}")
                        InfoRow("Rozkładów:", "${carrier.scheduleCount}")
                        carrier.downloadedAtFormatted?.let {
                            InfoRow("Pobrano:", it)
                        }
                        carrier.updatedAtFormatted?.let {
                            InfoRow("Zaktualizowano:", it)
                        }
                    } else {
                        InfoRow("Wersja:", "v${carrier.remoteVersion ?: "?"}")
                        InfoRow("Status:", "Dostępny do pobrania")
                    }

                    if (carrier.hasUpdate) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "📥 Dostępna: v${carrier.remoteVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Action Buttons (prawa strona)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!carrier.isDownloaded) {
                        // Download Button
                        Button(
                            onClick = {
                                isOperating = true
                                onDownload(carrier.id)
                            },
                            enabled = !isOperating
                        ) {
                            if (isOperating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Pobieranie...")
                            } else {
                                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Pobierz")
                            }
                        }
                    } else {
// Update Button (if available)
                        if (carrier.hasUpdate) {
                            val isOffline = carrier.remoteVersion == null

                            Column(horizontalAlignment = Alignment.End) {
                                Button(
                                    onClick = {
                                        isOperating = true
                                        onUpdate(carrier.id)
                                    },
                                    enabled = !isOperating && !isOffline,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    if (isOperating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onTertiary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Aktualizowanie...")
                                    } else {
                                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Aktualizuj")
                                    }
                                }

                                if (isOffline) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Wymaga internetu",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Rollback Button (if can rollback)
                        if (carrier.canRollback) {
                            OutlinedButton(
                                onClick = {
                                    isOperating = true
                                    onRollback(carrier.id)
                                },
                                enabled = !isOperating
                            ) {
                                Icon(Icons.Default.Undo, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("v${carrier.previousVersion}")
                            }
                        }

                        // Delete Button
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isOperating,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Usuń")
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog (bez zmian)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Usuń rozkłady?") },
            text = {
                Text("Czy na pewno chcesz usunąć rozkłady przewoźnika '${carrier.name}'? Ta operacja jest nieodwracalna.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(carrier.id)
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

@Composable
fun StatusBadge(
    isDownloaded: Boolean,
    hasUpdate: Boolean,
    isOffline: Boolean = false
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when {
            isOffline -> MaterialTheme.colorScheme.surfaceVariant
            hasUpdate -> MaterialTheme.colorScheme.tertiaryContainer
            isDownloaded -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = when {
                    isOffline -> "📴"
                    hasUpdate -> "🔔"
                    isDownloaded -> "✓"
                    else -> "📥"
                },
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = when {
                    isOffline -> "OFFLINE"
                    hasUpdate -> "AKTUALIZACJA"
                    isDownloaded -> "POBRANE"
                    else -> "DOSTĘPNE"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isOffline -> MaterialTheme.colorScheme.onSurfaceVariant
                    hasUpdate -> MaterialTheme.colorScheme.onTertiaryContainer
                    isDownloaded -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}