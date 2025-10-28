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
import androidx.compose.material.icons.filled.ArrowForward
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
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    schedules: List<BusSchedule>,
    onScheduleClick: (BusSchedule) -> Unit,
    onSettingsClick: () -> Unit  // ← DODAJ PARAMETR
) {
    // Stany filtrów
    var selectedCarrier by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDesignation by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDirection by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStop by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showOnlyToday by rememberSaveable { mutableStateOf(false) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }

    // Pobierz unikalnych przewoźników
    val carriers = remember(schedules) {
        schedules.map { it.carrierName }.distinct().sorted()
    }

    // Pobierz unikalne oznaczenia linii
    val designations = remember(schedules, selectedCarrier) {
        schedules
            .filter { selectedCarrier == null || it.carrierName == selectedCarrier }
            .mapNotNull { it.lineDesignation }
            .distinct()
            .sorted()
    }

    // Pobierz unikalne kierunki
    val directions = remember(schedules, selectedCarrier, selectedDesignation) {
        schedules
            .filter {
                (selectedCarrier == null || it.carrierName == selectedCarrier) &&
                        (selectedDesignation == null || it.lineDesignation == selectedDesignation)
            }
            .map { it.direction }
            .distinct()
            .sorted()
    }

    // Pobierz wszystkie przystanki z tras
    val allStops = remember(schedules, selectedCarrier, selectedDesignation) {
        schedules
            .filter {
                (selectedCarrier == null || it.carrierName == selectedCarrier) &&
                        (selectedDesignation == null || it.lineDesignation == selectedDesignation)
            }
            .flatMap { schedule -> schedule.stops.map { it.stopName } }
            .distinct()
            .sorted()
    }

    // Filtrowanie rozkładów
    val filteredSchedules = remember(schedules, selectedCarrier, selectedDesignation, selectedDirection, selectedStop, showOnlyToday) {
        schedules.filter { schedule ->
            val matchesCarrier = selectedCarrier == null || schedule.carrierName == selectedCarrier

            // ZMIENIONE: Sprawdź czy oznaczenie zawiera wybraną wartość
            val matchesDesignation = selectedDesignation == null ||
                    schedule.lineDesignation?.split(",")?.map { it.trim() }?.contains(selectedDesignation) == true

            val matchesDirection = selectedDirection == null || schedule.direction == selectedDirection
            val matchesStop = selectedStop == null ||
                    schedule.stops.any { it.stopName == selectedStop }

            val matchesToday = !showOnlyToday || schedule.operatesToday()

            matchesCarrier && matchesDesignation && matchesDirection && matchesStop && matchesToday
        }.sortedBy { it.departureTime }
    }

    // ← NOWE: Sprawdź czy jakieś filtry są aktywne
    val hasActiveFilters = selectedCarrier != null || selectedDesignation != null ||
            selectedDirection != null || selectedStop != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rozkład jazdy") },
                actions = {  // ← DODAJ ACTIONS
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ← NOWE: Kompaktowy nagłówek filtrów
            FilterHeader(
                expanded = filtersExpanded,
                hasActiveFilters = hasActiveFilters,
                activeFiltersCount = listOfNotNull(
                    selectedCarrier,
                    selectedDesignation,
                    selectedDirection,
                    selectedStop
                ).size,
                onToggleExpanded = { filtersExpanded = !filtersExpanded },
                onClearFilters = {
                    selectedCarrier = null
                    selectedDesignation = null
                    selectedDirection = null
                    selectedStop = null
                    searchQuery = ""
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
                    selectedCarrier = selectedCarrier,
                    selectedDesignation = selectedDesignation,
                    selectedDirection = selectedDirection,
                    selectedStop = selectedStop,
                    searchQuery = searchQuery,
                    showOnlyToday = showOnlyToday,  // ← DODAJ
                    onSearchQueryChange = { searchQuery = it },
                    onCarrierSelected = { carrier ->
                        selectedCarrier = if (selectedCarrier == carrier) null else carrier
                        selectedDesignation = null
                        selectedDirection = null
                        selectedStop = null
                    },
                    onDesignationSelected = { designation ->
                        selectedDesignation = if (selectedDesignation == designation) null else designation
                        selectedDirection = null
                        selectedStop = null
                    },
                    onDirectionSelected = { direction ->
                        selectedDirection = if (selectedDirection == direction) null else direction
                    },
                    onStopSelected = { stop ->
                        selectedStop = if (selectedStop == stop) null else stop
                    },
                    onTodayToggle = { showOnlyToday = !showOnlyToday },  // ← DODAJ
                    onClearFilters = {
                        selectedCarrier = null
                        selectedDesignation = null
                        selectedDirection = null
                        selectedStop = null
                        searchQuery = ""
                        showOnlyToday = false  // ← DODAJ
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
                    selectedCarrier = selectedCarrier,
                    selectedDesignation = selectedDesignation,
                    selectedDirection = selectedDirection,
                    selectedStop = selectedStop,
                    onRemoveCarrier = { selectedCarrier = null },
                    onRemoveDesignation = { selectedDesignation = null },
                    onRemoveDirection = { selectedDirection = null },
                    onRemoveStop = { selectedStop = null }
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

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSchedules) { schedule ->
                        BusScheduleItem(
                            schedule = schedule,
                            highlightedStop = selectedStop,
                            onClick = { onScheduleClick(schedule) }
                        )
                    }
                }
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
    selectedCarrier: String?,
    selectedDesignation: String?,
    selectedDirection: String?,
    selectedStop: String?,
    onRemoveCarrier: () -> Unit,
    onRemoveDesignation: () -> Unit,
    onRemoveDirection: () -> Unit,
    onRemoveStop: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        selectedCarrier?.let { carrier ->
            item {
                AnimatedVisibility(  // ← ANIMACJA
                    visible = true,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    InputChip(
                        selected = true,
                        onClick = onRemoveCarrier,
                        label = { Text("Przewoźnik: $carrier") },
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

        selectedDesignation?.let { designation ->
            item {
                AnimatedVisibility(  // ← ANIMACJA
                    visible = true,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    InputChip(
                        selected = true,
                        onClick = onRemoveDesignation,
                        label = { Text("Oznaczenie: $designation") },
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

        selectedStop?.let { stop ->
            item {
                AnimatedVisibility(  // ← ANIMACJA
                    visible = true,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    InputChip(
                        selected = true,
                        onClick = onRemoveStop,
                        label = { Text("Przystanek: $stop") },
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

        selectedDirection?.let { direction ->
            item {
                AnimatedVisibility(  // ← ANIMACJA
                    visible = true,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    InputChip(
                        selected = true,
                        onClick = onRemoveDirection,
                        label = { Text("Kierunek: $direction") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    carriers: List<String>,
    designations: List<String>,
    directions: List<String>,
    allStops: List<String>,
    selectedCarrier: String?,
    selectedDesignation: String?,
    selectedDirection: String?,
    selectedStop: String?,
    searchQuery: String,
    showOnlyToday: Boolean,  // ← NOWY PARAMETR
    onSearchQueryChange: (String) -> Unit,
    onCarrierSelected: (String) -> Unit,
    onDesignationSelected: (String) -> Unit,
    onDirectionSelected: (String) -> Unit,
    onStopSelected: (String) -> Unit,
    onTodayToggle: () -> Unit,  // ← NOWY PARAMETR
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // ← NOWY: Filtr "Tylko dziś" (na górze, zawsze widoczny)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showOnlyToday,
                onClick = onTodayToggle,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📅")
                        Text("Kursujące dziś")
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Informacja jaki dziś dzień
            Text(
                text = "(${BusSchedule.getDayNameInPolish(BusSchedule.getCurrentDayOfWeek())})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Filtry przewoźników
        if (carriers.isNotEmpty()) {
            Text(
                text = "Przewoźnik:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(carriers) { carrier ->
                    FilterChip(
                        selected = selectedCarrier == carrier,
                        onClick = { onCarrierSelected(carrier) },
                        label = { Text(carrier) },
                        leadingIcon = if (selectedCarrier == carrier) {
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

        // Filtry oznaczeń linii
        if (designations.isNotEmpty()) {
            Text(
                text = "Oznaczenie linii:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // NOWE: Rozbij wielokrotne oznaczenia i usuń duplikaty
            val expandedDesignations = remember(designations) {
                designations
                    .flatMap { it.split(",").map { part -> part.trim() } }
                    .distinct()
                    .sorted()
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expandedDesignations) { designation ->
                    FilterChip(
                        selected = selectedDesignation == designation,
                        onClick = { onDesignationSelected(designation) },
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
                        leadingIcon = if (selectedDesignation == designation) {
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

        // Filtry przystanków (wszystkich na trasach)
        if (allStops.isNotEmpty()) {
            Text(
                text = "Przystanek na trasie:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Pole wyszukiwania przystanków
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Szukaj przystanku...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            selected = selectedStop == stop,
                            onClick = { onStopSelected(stop) },
                            label = { Text(stop) },
                            leadingIcon = if (selectedStop == stop) {
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
        }

        // Filtry kierunków (końcowy przystanek)
        if (directions.isNotEmpty()) {
            Text(
                text = "Kierunek końcowy:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        Divider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusScheduleItem(
    schedule: BusSchedule,
    highlightedStop: String? = null,
    onClick: () -> Unit
) {
    val minutesUntil = calculateMinutesUntil(schedule.departureTime)
    val hasHighlightedStop = highlightedStop != null &&
            schedule.stops.any { it.stopName == highlightedStop }
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
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

                // Wybrany przystanek
                if (hasHighlightedStop && highlightedStop != schedule.stopName) {
                    val stopOnRoute = schedule.stops.find { it.stopName == highlightedStop }
                    stopOnRoute?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "🎯 ",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = highlightedStop,  // ZMIENIONE: bez czasu
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
                    imageVector = Icons.Default.ArrowForward,
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

// Funkcja obliczająca minuty do odjazdu
fun calculateMinutesUntil(departureTime: String): Int? {
    return try {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        val departure = Calendar.getInstance()

        val time = format.parse(departureTime)
        time?.let {
            departure.time = it
            departure.set(Calendar.YEAR, now.get(Calendar.YEAR))
            departure.set(Calendar.MONTH, now.get(Calendar.MONTH))
            departure.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

            val diff = (departure.timeInMillis - now.timeInMillis) / (60 * 1000)
            diff.toInt()
        }
    } catch (e: Exception) {
        null
    }
}