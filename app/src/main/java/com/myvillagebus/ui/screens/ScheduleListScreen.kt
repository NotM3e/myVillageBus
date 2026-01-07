package com.myvillagebus.ui.screens

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
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.myvillagebus.data.model.BusSchedule
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
    var fromStop by rememberSaveable { mutableStateOf<String?>(null) }
    var toStop by rememberSaveable { mutableStateOf<String?>(null) }
    var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showDayPickerDialog by remember { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }

    // Profile states
    val allProfiles by viewModel.allProfiles.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val profileStatus by viewModel.profileOperationStatus.collectAsState()

    // Drawer state
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSaveProfileDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Zapamiętaj początkowe wartości filtrów (gdy zastosowano profil)
    val initialFilters = remember(currentProfile) {
        currentProfile?.let {
            mapOf(
                "carriers" to it.selectedCarriers,
                "designations" to it.selectedDesignations,
                "fromStop" to it.fromStop,
                "toStop" to it.toStop,
                "day" to it.selectedDay
            )
        }
    }

    // Obserwuj zmiany filtrów i wyczyść currentProfile jeśli się zmieniły
    LaunchedEffect(
        selectedCarriers,
        selectedDesignations,
        fromStop,
        toStop,
        selectedDay,
        currentProfile
    ) {
        if (currentProfile != null && initialFilters != null) {
            val filtersChanged =
                selectedCarriers != initialFilters["carriers"] ||
                        selectedDesignations != initialFilters["designations"] ||
                        fromStop != initialFilters["fromStop"] ||
                        toStop != initialFilters["toStop"] ||
                        selectedDay != initialFilters["day"]

            if (filtersChanged) {
                viewModel.clearCurrentProfile()
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
            .filter { selectedCarriers.isEmpty() || selectedCarriers.contains(it.carrierName) }
            .mapNotNull { it.lineDesignation }
            .flatMap { it.split(",").map { d -> d.trim() } }
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

    // Pobierz wszystkie przystanki z tras (dla dropdownów)
    val allStops = remember(schedules, selectedCarriers) {
        schedules
            .filter {
                selectedCarriers.isEmpty() || selectedCarriers.contains(it.carrierName)
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

                // Logika fromStop / toStop
                val stops = schedule.stops.map { it.stopName }
                val fromIndex = fromStop?.let { stops.indexOf(it) } ?: -1
                val toIndex = toStop?.let { stops.indexOf(it) } ?: -1

                val matchesRoute = when {
                    fromStop == null && toStop == null -> true
                    fromStop != null && toStop == null -> fromIndex >= 0
                    fromStop == null && toStop != null -> toIndex >= 0
                    else -> fromIndex >= 0 && toIndex > fromIndex
                }

                val matchesDay = selectedDay?.let { schedule.operatesOn(it) } ?: true

                matchesCarrier && matchesDesignation && matchesRoute && matchesDay
            }.sortedBy { schedule ->
                // Sortuj po czasie na przystanku "Skąd" jeśli wybrany
                if (fromStop != null) {
                    schedule.stops.find { it.stopName == fromStop }?.arrivalTime
                        ?: schedule.departureTime
                } else {
                    schedule.departureTime
                }
            }
        }
    }

    val hasActiveFilters = selectedCarriers.isNotEmpty() ||
            selectedDesignations.isNotEmpty() ||
            fromStop != null ||
            toStop != null ||
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

            val targetIndex = filteredSchedules.indexOfFirst { schedule ->
                // Użyj czasu na przystanku "Skąd" jeśli wybrany, inaczej departureTime
                val timeToCompare = if (fromStop != null) {
                    schedule.stops.find { it.stopName == fromStop }?.arrivalTime
                        ?.takeIf { it.isNotBlank() }
                        ?: schedule.departureTime
                } else {
                    schedule.departureTime
                }

                val scheduleMinutes = parseTimeToMinutes(timeToCompare)
                scheduleMinutes >= selectedTimeMinutes
            }

            if (targetIndex != -1) {
                listState.animateScrollToItem(targetIndex)
            } else {
                if (filteredSchedules.isNotEmpty()) {
                    listState.animateScrollToItem(filteredSchedules.lastIndex)
                    snackbarHostState.showSnackbar(
                        message = "Brak rozkładów po godzinie ${String.format("%02d:%02d", hour, minute)}. Pokazano ostatni",
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
                    viewModel.applyProfile(profileId)?.let { filters ->
                        @Suppress("UNCHECKED_CAST")
                        selectedCarriers = (filters["carriers"] as? Set<*>)
                            ?.filterIsInstance<String>()
                            ?.toSet()
                            ?: emptySet()

                        @Suppress("UNCHECKED_CAST")
                        selectedDesignations = (filters["designations"] as? Set<*>)
                            ?.filterIsInstance<String>()
                            ?.toSet()
                            ?: emptySet()

                        fromStop = filters["fromStop"] as? String
                        toStop = filters["toStop"] as? String
                        selectedDay = filters["day"] as? DayOfWeek
                    }
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
                            Icon(Icons.Default.Menu, "Menu zapisanych filtrów")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToBrowser) {
                            Icon(Icons.Default.Download, "Przeglądarka rozkładów")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, "Ustawienia")
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
                FilterHeader(
                    expanded = filtersExpanded,
                    hasActiveFilters = hasActiveFilters,
                    activeFiltersCount = selectedCarriers.size + selectedDesignations.size +
                            (if (fromStop != null) 1 else 0) +
                            (if (toStop != null) 1 else 0) +
                            (if (selectedDay != null) 1 else 0),
                    onToggleExpanded = { filtersExpanded = !filtersExpanded },
                    onClearFilters = {
                        selectedCarriers = setOf()
                        selectedDesignations = setOf()
                        fromStop = null
                        toStop = null
                        selectedDay = null
                    }
                )

                AnimatedVisibility(
                    visible = filtersExpanded,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(initialAlpha = 0.3f),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    FilterSection(
                        carriers = carriers,
                        designations = designations,
                        allStops = allStops,
                        selectedCarriers = selectedCarriers,
                        selectedDesignations = selectedDesignations,
                        fromStop = fromStop,
                        toStop = toStop,
                        selectedDay = selectedDay,
                        showAdvancedFilters = showAdvancedFilters,
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
                        onFromStopSelected = { fromStop = it },
                        onToStopSelected = { toStop = it },
                        onSwapStops = {
                            val temp = fromStop
                            fromStop = toStop
                            toStop = temp
                        },
                        onTimePickerClick = { showTimePickerDialog = true },
                        onDayPickerClick = { showDayPickerDialog = true },
                        onNowClick = {
                            selectedDay = BusSchedule.getCurrentDayOfWeek()
                            val calendar = Calendar.getInstance()
                            scrollToTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
                        },
                        onToggleAdvanced = { showAdvancedFilters = !showAdvancedFilters },
                        onClearFilters = {
                            selectedCarriers = setOf()
                            selectedDesignations = setOf()
                            fromStop = null
                            toStop = null
                            selectedDay = null
                        }
                    )
                }

                AnimatedVisibility(
                    visible = !filtersExpanded && hasActiveFilters,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    ActiveFiltersChips(
                        selectedCarriers = selectedCarriers,
                        selectedDesignations = selectedDesignations,
                        fromStop = fromStop,
                        toStop = toStop,
                        selectedDay = selectedDay,
                        onRemoveCarrier = { carrier -> selectedCarriers = selectedCarriers - carrier },
                        onRemoveDesignation = { designation -> selectedDesignations = selectedDesignations - designation },
                        onRemoveFromStop = { fromStop = null },
                        onRemoveToStop = { toStop = null },
                        onRemoveDay = { selectedDay = null }
                    )
                }

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
                            Text(text = "🚌", style = MaterialTheme.typography.displayLarge)
                            Spacer(modifier = Modifier.height(16.dp))

                            if (schedules.isEmpty()) {
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
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredSchedules) { schedule ->
                            BusScheduleItem(
                                schedule = schedule,
                                highlightedStops = setOfNotNull(fromStop, toStop),
                                onClick = {
                                    viewModel.setHighlightedStops(fromStop, toStop)
                                    onScheduleClick(schedule)
                                }
                            )
                        }
                    }
                }
            }

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

            if (showTimePickerDialog) {
                TimePickerDialog(
                    onDismiss = { showTimePickerDialog = false },
                    onConfirm = { hour, minute ->
                        scrollToTime(hour, minute)
                        showTimePickerDialog = false
                    }
                )
            }

            if (showSaveProfileDialog) {
                SaveProfileDialog(
                    currentFilters = mapOf(
                        "carriers" to selectedCarriers,
                        "designations" to selectedDesignations,
                        "fromStop" to fromStop,
                        "toStop" to toStop,
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

    //  Animowana przezroczystość przycisku
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
                .height(56.dp)  //  Stała wysokość
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
                enabled = hasActiveFilters,  //  Wyłączony gdy brak filtrów
                modifier = Modifier.alpha(clearButtonAlpha)  //  Animowana przezroczystość
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
    fromStop: String?,
    toStop: String?,
    selectedDay: DayOfWeek?,
    onRemoveCarrier: (String) -> Unit,
    onRemoveDesignation: (String) -> Unit,
    onRemoveFromStop: () -> Unit,
    onRemoveToStop: () -> Unit,
    onRemoveDay: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Przewoźnicy
        items(selectedCarriers.toList()) { carrier ->
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

        // Skąd
        fromStop?.let { stop ->
            item {
                InputChip(
                    selected = true,
                    onClick = onRemoveFromStop,
                    label = { Text("📍 $stop") },
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

        // Dokąd
        toStop?.let { stop ->
            item {
                InputChip(
                    selected = true,
                    onClick = onRemoveToStop,
                    label = { Text("🎯 $stop") },
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

        // Dzień
        selectedDay?.let { day ->
            item {
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

        // Oznaczenia
        items(selectedDesignations.toList()) { designation ->
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSection(
    carriers: List<String>,
    designations: List<String>,
    allStops: List<String>,
    selectedCarriers: Set<String>,
    selectedDesignations: Set<String>,
    fromStop: String?,
    toStop: String?,
    selectedDay: DayOfWeek?,
    showAdvancedFilters: Boolean,
    onCarrierToggle: (String) -> Unit,
    onDesignationToggle: (String) -> Unit,
    onFromStopSelected: (String?) -> Unit,
    onToStopSelected: (String?) -> Unit,
    onSwapStops: () -> Unit,
    onTimePickerClick: () -> Unit,
    onDayPickerClick: () -> Unit,
    onNowClick: () -> Unit,
    onToggleAdvanced: () -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ═══════════ SZYBKIE AKCJE ═══════════
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Przycisk "Teraz"
            FilterChip(
                selected = false,
                onClick = onNowClick,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("⚡")
                        Text("Teraz")
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

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

        // ═══════════ SKĄD -> DOKĄD ═══════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dropdowny
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StopDropdown(
                    label = "Skąd",
                    selectedStop = fromStop,
                    availableStops = allStops,
                    onStopSelected = onFromStopSelected
                )

                StopDropdown(
                    label = "Dokąd",
                    selectedStop = toStop,
                    availableStops = allStops,
                    onStopSelected = onToStopSelected
                )
            }

            // Przycisk zamiany
            IconButton(
                onClick = onSwapStops,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp),
                enabled = fromStop != null || toStop != null
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Zamień przystanki",
                    tint = if (fromStop != null || toStop != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // ═══════════ ZAAWANSOWANE ═══════════
        val advancedFiltersCount = selectedCarriers.size + selectedDesignations.size

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clickable { onToggleAdvanced() },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zaawansowane",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (advancedFiltersCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = advancedFiltersCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (showAdvancedFilters) "▲" else "▼",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Zawartość zaawansowanych filtrów
        AnimatedVisibility(
            visible = showAdvancedFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Przewoźnicy
                if (carriers.isNotEmpty()) {
                    Text(
                        text = "Przewoźnik:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    LazyRow(
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

                // Oznaczenia linii
                if (designations.isNotEmpty()) {
                    Text(
                        text = "Oznaczenia linii:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(designations) { designation ->
                            FilterChip(
                                selected = selectedDesignations.contains(designation),
                                onClick = { onDesignationToggle(designation) },
                                label = { Text(designation) },
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
    highlightedStops: Set<String> = emptySet(),  //  MULTI
    onClick: () -> Unit
) {
    val hasHighlightedStop = highlightedStops.isNotEmpty() &&
            highlightedStops.any { stop ->
                schedule.stops.any { it.stopName == stop }
            }
    val operatesToday = schedule.operatesToday()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                hasHighlightedStop -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                !operatesToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)  // przygaszony jeśli nie dziś
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

                // Dni kursu
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
                // Wyświetl godzinę z wybranego przystanku "Skąd" jeśli dostępna
                val displayTime = if (highlightedStops.isNotEmpty()) {
                    // Znajdź pierwszy highlighted stop (fromStop) i jego czas
                    val fromStopName = highlightedStops.firstOrNull()
                    fromStopName?.let { stopName ->
                        schedule.stops.find { it.stopName == stopName }?.arrivalTime
                            ?.takeIf { it.isNotBlank() }
                    } ?: schedule.departureTime
                } else {
                    schedule.departureTime
                }

                val minutesUntil = calculateMinutesUntil(displayTime)

                Text(
                    text = displayTime,
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

// Kolory dla różnych oznaczeń
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