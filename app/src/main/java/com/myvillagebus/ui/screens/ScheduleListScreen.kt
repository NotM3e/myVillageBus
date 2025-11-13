package com.myvillagebus.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.model.Profile
import com.myvillagebus.ui.viewmodel.BusViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.myvillagebus.utils.calculateMinutesUntil
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Download
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    viewModel: BusViewModel,
    onScheduleClick: (BusSchedule) -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToProfileManagement: () -> Unit
) {
    val schedules by viewModel.allSchedules.collectAsState()

    // Stany filtrów
    var selectedCarriers by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedDesignations by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedDirection by rememberSaveable { mutableStateOf<String?>(null) }  // Single-select, others is Multi-select
    var selectedStops by rememberSaveable { mutableStateOf(setOf<String>()) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }

    // Stan dialogu wyboru godziny i dnia
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showDayPickerDialog by remember { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }

    // ========================================
    // PROFILE STATES
    // ========================================

    val allProfiles by viewModel.allProfiles.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val profileStatus by viewModel.profileOperationStatus.collectAsState()

    // Drawer state
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showSaveProfileDialog by remember { mutableStateOf(false) }

    // LazyListState do kontroli scrollowania
    val listState = rememberLazyListState()

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // ========================================
    // NOWE: Wyczyść currentProfile po zmianie filtrów
    // ========================================

    // Zapamiętaj początkowe wartości filtrów (gdy zastosowano profil)
    val initialFilters = remember(currentProfile) {
        currentProfile?.let {
            mapOf(
                "carriers" to it.selectedCarriers,
                "designations" to it.selectedDesignations,
                "stops" to it.selectedStops,
                "direction" to it.selectedDirection,
                "day" to it.selectedDay
            )
        }
    }

    // Obserwuj zmiany filtrów i wyczyść currentProfile jeśli się zmieniły
    LaunchedEffect(
        selectedCarriers,
        selectedDesignations,
        selectedStops,
        selectedDirection,
        selectedDay,
        currentProfile
    ) {
        // Sprawdź tylko jeśli jest aktywny profil
        if (currentProfile != null && initialFilters != null) {
            val filtersChanged =
                selectedCarriers != initialFilters["carriers"] ||
                        selectedDesignations != initialFilters["designations"] ||
                        selectedStops != initialFilters["stops"] ||
                        selectedDirection != initialFilters["direction"] ||
                        selectedDay != initialFilters["day"]

            if (filtersChanged) {
                // Wyczyść aktywny profil
                viewModel.clearCurrentProfile()
                Log.d("ScheduleListScreen", "Filtry zmienione - wyczyszczono aktywny profil")
            }
        }
    }

    // Pokaż Snackbar po operacji na profilu
    LaunchedEffect(profileStatus) {
        profileStatus?.let { status ->
            snackbarHostState.showSnackbar(
                message = status,
                duration = SnackbarDuration.Short
            )
            viewModel.clearProfileStatus()
        }
    }

    // Pobierz unikalnych przewoźników
    val carriers = remember(schedules) {
        schedules.map { it.carrierName }.distinct().sorted()
    }

    // Pobierz unikalne oznaczenia linii
    val designations = remember(schedules, selectedCarriers) {
        schedules
            .filter { selectedCarriers.isEmpty() || selectedCarriers.contains(it.carrierName) }  // ← OR logic
            .mapNotNull { it.lineDesignation }
            .flatMap { it.split(",").map { d -> d.trim() } }  // ← Rozdziel wielokrotne oznaczenia
            .distinct()
            .sorted()
    }

    // Pobierz unikalne kierunki
    val directions = remember(schedules, selectedCarriers, selectedDesignations) {
        schedules
            .filter {
                val matchesCarrier = selectedCarriers.isEmpty() || selectedCarriers.contains(it.carrierName)
                val matchesDesignation = selectedDesignations.isEmpty() ||
                        selectedDesignations.all { designation ->
                            it.lineDesignation?.split(",")?.map { d -> d.trim() }?.contains(designation) == true
                        }
                matchesCarrier && matchesDesignation
            }
            .map { it.direction }
            .distinct()
            .sorted()
    }

    // Pobierz wszystkie przystanki z tras
    val allStops = remember(schedules, selectedCarriers, selectedDesignations) {
        schedules
            .filter {
                val matchesCarrier = selectedCarriers.isEmpty() || selectedCarriers.contains(it.carrierName)
                val matchesDesignation = selectedDesignations.isEmpty() ||
                        selectedDesignations.all { designation ->
                            it.lineDesignation?.split(",")?.map { d -> d.trim() }?.contains(designation) == true
                        }
                matchesCarrier && matchesDesignation
            }
            .flatMap { schedule -> schedule.stops.map { it.stopName } }
            .distinct()
            .sorted()
    }

    // Filtrowanie rozkładów
    val filteredSchedules by remember {
        derivedStateOf {
            schedules.filter { schedule ->
                val matchesCarrier = selectedCarriers.isEmpty() || selectedCarriers.contains(schedule.carrierName)

                val matchesDesignation = selectedDesignations.isEmpty() ||
                        selectedDesignations.all { designation ->
                            schedule.lineDesignation?.split(",")?.map { it.trim() }?.contains(designation) == true
                        }

                val matchesDirection = selectedDirection == null || schedule.direction == selectedDirection

                val matchesStop = selectedStops.isEmpty() ||
                        selectedStops.any { stop ->
                            schedule.stops.any { it.stopName == stop }
                        }

                val matchesDay = selectedDay?.let { schedule.operatesOn(it) } ?: true

                matchesCarrier && matchesDesignation && matchesDirection && matchesStop && matchesDay
            }.sortedBy { it.departureTime }
        }
    }

    val hasActiveFilters = selectedCarriers.isNotEmpty() || selectedDesignations.isNotEmpty() ||
            selectedDirection != null || selectedStops.isNotEmpty() ||
            selectedDay != null

    // Funkcja pomocnicza - konwersja "HH:MM" na minuty
    fun parseTimeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            hours * 60 + minutes
        } catch (e: Exception) {
            0
        }
    }

    // Funkcja scrollowania do wybranej godziny
    fun scrollToTime(hour: Int, minute: Int) {
        scope.launch {
            val selectedTimeMinutes = hour * 60 + minute

            // Znajdź pierwszy rozkład >= wybranej godziny
            val targetIndex = filteredSchedules.indexOfFirst { schedule ->
                val scheduleMinutes = parseTimeToMinutes(schedule.departureTime)  // ← Teraz działa!
                scheduleMinutes >= selectedTimeMinutes
            }

            if (targetIndex != -1) {
                // Znaleziono - scroll do rozkładu
                listState.animateScrollToItem(targetIndex)
            } else {
                // Brak rozkładów po wybranej godzinie - scroll do ostatniego
                if (filteredSchedules.isNotEmpty()) {
                    listState.animateScrollToItem(filteredSchedules.lastIndex)
                    snackbarHostState.showSnackbar(
                        message = "Brak rozkładów po godzinie ${String.format("%02d:%02d", hour, minute)}. Pokazano ostatni",
                        duration = SnackbarDuration.Short
                    )
                } else {
                    snackbarHostState.showSnackbar(
                        message = "Brak rozkładów do wyświetlenia",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileDrawerContent(
                profiles = allProfiles,
                allSchedules = schedules,
                currentProfileId = currentProfile?.id,
                onProfileClick = { profileId ->
                    // Zastosuj profil
                    viewModel.applyProfile(profileId)?.let { filters ->
                        selectedCarriers = filters["carriers"] as? Set<String> ?: emptySet()
                        selectedDesignations = filters["designations"] as? Set<String> ?: emptySet()
                        selectedStops = filters["stops"] as? Set<String> ?: emptySet()
                        selectedDirection = filters["direction"] as? String
                        selectedDay = filters["day"] as? DayOfWeek
                    }

                    // Zamknij drawer
                    scope.launch { drawerState.close() }
                },
                onCreateNewClick = {
                    showSaveProfileDialog = true
                    scope.launch { drawerState.close() }
                },
                onManageClick = {
                    onNavigateToProfileManagement()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Rozkład jazdy") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu profili")
                        }
                    },
                    actions = {
                        // Przycisk do przeglądarki rozkładów
                        IconButton(onClick = onNavigateToBrowser) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Przeglądarka rozkładów"
                            )
                        }
                        // Istniejący: Przycisk do ustawień
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ustawienia"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                // FAB - pokazuj jeśli są aktywne filtry
                if (hasActiveFilters) {
                    FloatingActionButton(
                        onClick = { showSaveProfileDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(Icons.Default.Add, "Zapisz jako profil")
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Kompaktowy nagłówek filtrów
                FilterHeader(
                    expanded = filtersExpanded,
                    hasActiveFilters = hasActiveFilters,
                    activeFiltersCount = selectedCarriers.size + selectedDesignations.size +
                            (if (selectedDirection != null) 1 else 0) + selectedStops.size +
                            (if (selectedDay != null) 1 else 0),
                    onToggleExpanded = { filtersExpanded = !filtersExpanded },
                    onClearFilters = {
                        selectedCarriers = setOf()
                        selectedDesignations = setOf()
                        selectedDirection = null
                        selectedStops = setOf()
                        searchQuery = ""
                        selectedDay = null
                    }
                )

                AnimatedVisibility(
                    visible = filtersExpanded,
                    enter = expandVertically(
                        expandFrom = androidx.compose.ui.Alignment.Top
                    ) + fadeIn(
                        initialAlpha = 0.3f
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = androidx.compose.ui.Alignment.Top
                    ) + fadeOut()
                ) {
                    FilterSection(
                        carriers = carriers,
                        designations = designations,
                        directions = directions,
                        allStops = allStops,
                        selectedCarriers = selectedCarriers,
                        selectedDesignations = selectedDesignations,
                        selectedDirection = selectedDirection,
                        selectedStops = selectedStops,
                        searchQuery = searchQuery,
                        selectedDay = selectedDay,
                        onSearchQueryChange = { searchQuery = it },
                        onCarrierToggle = { carrier ->
                            selectedCarriers = if (selectedCarriers.contains(carrier)) {
                                selectedCarriers - carrier
                            } else {
                                selectedCarriers + carrier
                            }
                        },
                        onDesignationToggle = { designation ->
                            selectedDesignations = if (selectedDesignations.contains(designation)) {
                                selectedDesignations - designation
                            } else {
                                selectedDesignations + designation
                            }
                        },
                        onDirectionSelected = { direction ->
                            selectedDirection =
                                if (selectedDirection == direction) null else direction
                        },
                        onStopToggle = { stop ->
                            selectedStops = if (selectedStops.contains(stop)) {
                                selectedStops - stop
                            } else {
                                selectedStops + stop
                            }
                        },
                        onTimePickerClick = { showTimePickerDialog = true },
                        onDayPickerClick = { showDayPickerDialog = true },
                        onClearFilters = {
                            selectedCarriers = setOf()
                            selectedDesignations = setOf()
                            selectedDirection = null
                            selectedStops = setOf()
                            searchQuery = ""
                            selectedDay = null
                        }
                    )
                }

                AnimatedVisibility(
                    visible = !filtersExpanded && hasActiveFilters,
                    enter = expandVertically(
                        expandFrom = androidx.compose.ui.Alignment.Top
                    ) + fadeIn(),
                    exit = shrinkVertically(
                        shrinkTowards = androidx.compose.ui.Alignment.Top
                    ) + fadeOut()
                ) {
                    ActiveFiltersChips(
                        selectedCarriers = selectedCarriers,
                        selectedDesignations = selectedDesignations,
                        selectedDirection = selectedDirection,
                        selectedStops = selectedStops,
                        selectedDay = selectedDay,
                        onRemoveCarrier = { carrier ->
                            selectedCarriers = selectedCarriers - carrier
                        },
                        onRemoveDesignation = { designation ->
                            selectedDesignations = selectedDesignations - designation
                        },
                        onRemoveDirection = { selectedDirection = null },
                        onRemoveStop = { stop ->
                            selectedStops = selectedStops - stop
                        },
                        onRemoveDay = { selectedDay = null }
                    )
                }
                // Informacja o liczbie wyników
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (filteredSchedules.isNotEmpty()) {
                        Text(
                            text = "Znaleziono: ${filteredSchedules.size} ${
                                when {
                                    filteredSchedules.size == 1 -> "odjazd"
                                    filteredSchedules.size < 5 -> "odjazdy"
                                    else -> "odjazdów"
                                }
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Brak wyników",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Lista rozkładów
                if (filteredSchedules.isEmpty()) {
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
                                text = "🚌",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Sprawdź czy baza jest w ogóle pusta
                            if (schedules.isEmpty()) {
                                // Brak danych w bazie
                                Text(
                                    text = "Brak rozkładów",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pobierz rozkłady w Ustawieniach",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onSettingsClick) {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Przejdź do Ustawień")
                                }
                            } else {
                                // Są dane, ale filtry je ukrywają
                                Text(
                                    text = "Brak odjazdów",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Spróbuj zmienić filtry",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,  // ← DODAJ
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredSchedules) { schedule ->
                            BusScheduleItem(
                                schedule = schedule,
                                highlightedStops = selectedStops,
                                onClick = { onScheduleClick(schedule) }
                            )
                        }
                    }
                }
            }
            // Dialog wyboru dnia
            if (showDayPickerDialog) {
                DayPickerDialog(
                    currentSelection = selectedDay,
                    onDismiss = { showDayPickerDialog = false },
                    onDaySelected = { day ->
                        selectedDay = day
                        showDayPickerDialog = false
                    },
                    onClear = { selectedDay = null }
                )
            }

            // Dialog wyboru godziny
            if (showTimePickerDialog) {
                TimePickerDialog(
                    onDismiss = { showTimePickerDialog = false },
                    onConfirm = { hour, minute ->
                        scrollToTime(hour, minute)
                        showTimePickerDialog = false
                    }
                )
            }

            // Dialog zapisywania profilu
            if (showSaveProfileDialog) {
                SaveProfileDialog(
                    currentFilters = mapOf(
                        "carriers" to selectedCarriers,
                        "designations" to selectedDesignations,
                        "stops" to selectedStops,
                        "direction" to selectedDirection,
                        "day" to selectedDay
                    ),
                    viewModel = viewModel,
                    onDismiss = { showSaveProfileDialog = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FilterHeader(
    expanded: Boolean,
    hasActiveFilters: Boolean,
    activeFiltersCount: Int,
    onToggleExpanded: () -> Unit,
    onClearFilters: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrow_rotation"
    )

    // ← Animowana przezroczystość przycisku
    val clearButtonAlpha by animateFloatAsState(
        targetValue = if (hasActiveFilters) 1f else 0f,
        label = "clear_button_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (hasActiveFilters)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)  // ← Stała wysokość
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "▼",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(rotationAngle)
                )

                Text(
                    text = "Filtry",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (hasActiveFilters && !expanded) {
                    AnimatedContent(
                        targetState = activeFiltersCount,
                        label = "filter_count"
                    ) { count ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // ✅ Przycisk zawsze istnieje, zmienia tylko przezroczystość
            TextButton(
                onClick = onClearFilters,
                contentPadding = PaddingValues(horizontal = 8.dp),
                enabled = hasActiveFilters,  // ← Wyłączony gdy brak filtrów
                modifier = Modifier.alpha(clearButtonAlpha)  // ← Animowana przezroczystość
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Wyczyść",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Wyczyść")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFiltersChips(
    selectedCarriers: Set<String>,
    selectedDesignations: Set<String>,
    selectedDirection: String?,
    selectedStops: Set<String>,
    selectedDay: DayOfWeek?,
    onRemoveCarrier: (String) -> Unit,
    onRemoveDesignation: (String) -> Unit,
    onRemoveDirection: () -> Unit,
    onRemoveStop: (String) -> Unit,
    onRemoveDay: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ← MULTI: Przewoźnicy
        items(selectedCarriers.toList()) { carrier ->
            AnimatedVisibility(
                visible = true,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                InputChip(
                    selected = true,
                    onClick = { onRemoveCarrier(carrier) },
                    label = { Text(carrier) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Usuń",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // ← MULTI: Oznaczenia
        items(selectedDesignations.toList()) { designation ->
            AnimatedVisibility(
                visible = true,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                InputChip(
                    selected = true,
                    onClick = { onRemoveDesignation(designation) },
                    label = { Text(designation) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Usuń",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = getDesignationColor(designation)
                    )
                )
            }
        }

        // ← MULTI: Przystanki
        items(selectedStops.toList()) { stop ->
            AnimatedVisibility(
                visible = true,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                InputChip(
                    selected = true,
                    onClick = { onRemoveStop(stop) },
                    label = { Text(stop) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Usuń",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // Wybrany dzień
        selectedDay?.let { day ->
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    InputChip(
                        selected = true,
                        onClick = onRemoveDay,
                        label = { Text("📆 ${BusSchedule.getDayNameInPolish(day)}") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Usuń",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        // ← SINGLE: Kierunek
        selectedDirection?.let { direction ->
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    InputChip(
                        selected = true,
                        onClick = onRemoveDirection,
                        label = { Text("→ $direction") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Usuń",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSection(
    carriers: List<String>,
    designations: List<String>,
    directions: List<String>,
    allStops: List<String>,
    selectedCarriers: Set<String>,
    selectedDesignations: Set<String>,
    selectedDirection: String?,
    selectedStops: Set<String>,
    searchQuery: String,
    selectedDay: DayOfWeek?,
    onSearchQueryChange: (String) -> Unit,
    onCarrierToggle: (String) -> Unit,
    onDesignationToggle: (String) -> Unit,
    onDirectionSelected: (String) -> Unit,
    onStopToggle: (String) -> Unit,
    onTimePickerClick: () -> Unit,
    onDayPickerClick: () -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
// ← FlowRow z przyciskami
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Przycisk "Wybór Dnia"
            FilterChip(
                selected = selectedDay != null,
                onClick = onDayPickerClick,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📆")
                        Text(
                            if (selectedDay != null)
                                BusSchedule.getDayNameInPolish(selectedDay)
                            else
                                "Wybór Dnia"
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Przycisk "Wybór godziny"
            FilterChip(
                selected = false,
                onClick = onTimePickerClick,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🕒")
                        Text("Wybór godziny")
                    }
                }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Filtry przewoźników (MULTI-SELECT OR)
        if (carriers.isNotEmpty()) {
            Text(
                text = "Przewoźnik:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(carriers) { carrier ->
                    FilterChip(
                        selected = selectedCarriers.contains(carrier),
                        onClick = { onCarrierToggle(carrier) },
                        label = { Text(carrier) },
                        leadingIcon = if (selectedCarriers.contains(carrier)) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,  // ← Checkmark zamiast X
                                    contentDescription = "Wybrano",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        // Filtry oznaczeń linii (MULTI-SELECT AND)
        if (designations.isNotEmpty()) {
            Text(
                text = "Oznaczenie linii:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(designations) { designation ->
                    FilterChip(
                        selected = selectedDesignations.contains(designation),
                        onClick = { onDesignationToggle(designation) },
                        label = {
                            Text(
                                text = designation,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getDesignationColor(designation),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        leadingIcon = if (selectedDesignations.contains(designation)) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Wybrano",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        // Filtry przystanków (MULTI-SELECT OR)
        if (allStops.isNotEmpty()) {
            Text(
                text = "Przystanek na trasie:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            // Pole wyszukiwania przystanków
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Szukaj przystanku...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, "Szukaj")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Wyczyść wyszukiwanie"
                            )
                        }
                    }
                },
                singleLine = true
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val filteredStops = allStops.filter {
                    it.contains(searchQuery, ignoreCase = true)
                }

                if (filteredStops.isEmpty() && searchQuery.isNotEmpty()) {
                    item {
                        Text(
                            text = "Brak przystanków pasujących do \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(filteredStops) { stop ->
                        FilterChip(
                            selected = selectedStops.contains(stop),
                            onClick = { onStopToggle(stop) },
                            label = { Text(stop) },
                            leadingIcon = if (selectedStops.contains(stop)) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Wybrano",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // Filtry kierunków (końcowy przystanek)
        if (directions.isNotEmpty()) {
            Text(
                text = "Kierunek końcowy:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(directions) { direction ->
                    FilterChip(
                        selected = selectedDirection == direction,
                        onClick = { onDirectionSelected(direction) },
                        label = { Text(direction) },
                        leadingIcon = if (selectedDirection == direction) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Usuń filtr",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusScheduleItem(
    schedule: BusSchedule,
    highlightedStops: Set<String> = emptySet(),  // ← MULTI
    onClick: () -> Unit
) {
    val minutesUntil = calculateMinutesUntil(schedule.departureTime)
    val hasHighlightedStop = highlightedStops.isNotEmpty() &&
            highlightedStops.any { stop ->
                schedule.stops.any { it.stopName == stop }
            }
    val operatesToday = schedule.operatesToday()  // ← NOWE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                hasHighlightedStop -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                !operatesToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)  // ← NOWE: przygaszony jeśli nie dziś
                else -> MaterialTheme.colorScheme.surface
            }
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
                // Nazwa przewoźnika + oznaczenie
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = schedule.carrierName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (operatesToday)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    schedule.lineDesignation?.let { designation ->
                        // Rozdziel po przecinku i wyświetl jako osobne chipy
                        val designations = designation.split(",").map { it.trim() }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            designations.forEach { singleDesignation ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = getDesignationColor(singleDesignation)
                                ) {
                                    Text(
                                        text = singleDesignation,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Trasa
                Text(
                    text = schedule.busLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                /* <-> UKRYTE: Kierunek i przystanek początkowy (duplikacja z trasy busLine)
                // Kierunek
                Text(
                    text = "→ ${schedule.direction}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Przystanek początkowy
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📍 ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = schedule.stopName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                */

                // ← NOWE: Dni kursu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "📅 ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = schedule.getOperatingDaysShort(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (operatesToday)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Badge "Dziś kursuje"
                    if (operatesToday) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Dziś ✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Wybrane przystanki
                if (hasHighlightedStop) {
                    val stopsOnRoute = schedule.stops
                        .filter { highlightedStops.contains(it.stopName) && it.stopName != schedule.stopName }
                        .map { it.stopName }

                    if (stopsOnRoute.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "🎯 ",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stopsOnRoute.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Godzina i czas do odjazdu
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = schedule.departureTime,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (operatesToday)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (minutesUntil != null && minutesUntil >= 0 && operatesToday) {
                    Text(
                        text = when {
                            minutesUntil == 0 -> "Teraz"
                            minutesUntil < 60 -> "za $minutesUntil min"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            minutesUntil <= 5 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Zobacz szczegóły",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ← NOWA FUNKCJA: Kolory dla różnych oznaczeń
@Composable
fun getDesignationColor(designation: String): androidx.compose.ui.graphics.Color {
    // Specjalne przypadki
    when {
        designation.contains("Express", ignoreCase = true) ->
            return MaterialTheme.colorScheme.tertiary
        designation.contains("Nocny", ignoreCase = true) ->
            return androidx.compose.ui.graphics.Color(0xFF1565C0)
    }

    // Generuj kolor na podstawie hash'a nazwy
    val hash = designation.hashCode()
    val hue = (hash % 360).toFloat().let { if (it < 0) it + 360 else it }

    // Paleta kolorów Material Design
    val colors = listOf(
        androidx.compose.ui.graphics.Color(0xFF1976D2), // Niebieski
        androidx.compose.ui.graphics.Color(0xFF388E3C), // Zielony
        androidx.compose.ui.graphics.Color(0xFFD32F2F), // Czerwony
        androidx.compose.ui.graphics.Color(0xFF7B1FA2), // Fioletowy
        androidx.compose.ui.graphics.Color(0xFFF57C00), // Pomarańczowy
        androidx.compose.ui.graphics.Color(0xFF0097A7), // Cyjan
        androidx.compose.ui.graphics.Color(0xFFC2185B), // Różowy
        androidx.compose.ui.graphics.Color(0xFF5D4037), // Brązowy
        androidx.compose.ui.graphics.Color(0xFF455A64), // Niebieski-szary
        androidx.compose.ui.graphics.Color(0xFF689F38), // Jasnozielony
        androidx.compose.ui.graphics.Color(0xFFE64A19), // Głęboka pomarańcz
        androidx.compose.ui.graphics.Color(0xFF512DA8), // Głęboki fiolet
    )

    // Wybierz kolor na podstawie hash'a
    return colors[hash.mod(colors.size)]
}

/**
 * Dialog z Material 3 TimePickerem
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    // Pobierz aktualną godzinę jako domyślną
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)

    val timePickerState = rememberTimePickerState(
        initialHour = currentHour,
        initialMinute = currentMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("OK")
            }
        },
        text = {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(16.dp)
            )
        }
    )
}

/**
 * Dialog z listą dni tygodnia do wyboru
 */
@Composable
fun DayPickerDialog(
    currentSelection: DayOfWeek?,
    onDismiss: () -> Unit,
    onDaySelected: (DayOfWeek) -> Unit,
    onClear: () -> Unit
) {
    val days = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz dzień tygodnia") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEach { day ->
                    val isToday = day == BusSchedule.getCurrentDayOfWeek()
                    val isSelected = day == currentSelection

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDaySelected(day) },
                        shape = MaterialTheme.shapes.medium,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (isToday && !isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = BusSchedule.getDayNameInPolish(day),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                if (isToday) {
                                    Text(
                                        text = "Dzisiaj",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Wybrano",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
        dismissButton = {
            if (currentSelection != null) {
                TextButton(
                    onClick = {
                        onClear()
                        onDismiss()
                    }
                ) {
                    Text("Wyczyść")
                }
            }
        }

    )
}