package com.myvillagebus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.model.BusStop
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.FlowRow
import java.time.DayOfWeek
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.myvillagebus.utils.calculateMinutesUntil


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleDetailScreen(
    schedule: BusSchedule,
    highlightedStops: Set<String> = emptySet(),
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(schedule.carrierName)

                        schedule.lineDesignation?.let { designation ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = designation,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Wróć"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Nagłówek z informacjami o trasie
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Przewoźnik + oznaczenie
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = schedule.carrierName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // ← ZMIANA: Rozdziel wielokrotne oznaczenia
                        schedule.lineDesignation?.let { designation ->
                            val designations = designation.split(",").map { it.trim() }

                            designations.forEach { singleDesignation ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = getDesignationColor(singleDesignation)  // ← Kolor per oznaczenie
                                ) {
                                    Text(
                                        text = singleDesignation,  // ← Wyświetla "Komursk" i "Warlubie" osobno
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Wytłumaczenie oznaczenia
                    schedule.designationDescription?.let { description ->
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Informacja",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trasa
                    Text(
                        text = schedule.busLine,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Odjazd: ${schedule.departureTime}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Kierunek: ${schedule.direction}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // ← NOWE: Informacje o dniach kursu
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "📅 Dni kursu:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Wyświetl wszystkie dni jako chipy
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayOfWeek.values().forEach { day ->
                            val isOperating = schedule.operatesOn(day)
                            val isToday = day == BusSchedule.getCurrentDayOfWeek()

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when {
                                    isOperating && isToday -> MaterialTheme.colorScheme.primary
                                    isOperating -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                },
                                border = if (isToday) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else null
                            ) {
                                Text(
                                    text = BusSchedule.getDayAbbreviation(day),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        isOperating && isToday -> MaterialTheme.colorScheme.onPrimary
                                        isOperating -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = schedule.getOperatingDaysDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    // Badge "Dziś kursuje"
                    if (schedule.operatesToday()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Ten kurs dziś kursuje",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Lista przystanków
            Text(
                text = "Przystanki",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(schedule.stops) { index, stop ->
                    BusStopItem(
                        stop = stop,
                        isFirst = index == 0,
                        isLast = index == schedule.stops.lastIndex,
                        isHighlighted = highlightedStops.contains(stop.stopName),
                        departureTime = if (index == 0) schedule.departureTime else null
                    )
                }
            }
        }
    }
}

@Composable
fun BusStopItem(
    stop: BusStop,
    isFirst: Boolean,
    isLast: Boolean,
    isHighlighted: Boolean = false,
    departureTime: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Wizualizacja linii i punktu przystanku
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Linia górna
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Punkt przystanku
            Box(
                modifier = Modifier
                    .size(if (isHighlighted) 18.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isHighlighted -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
            )

            // Linia dolna
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Informacje o przystanku
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stop.stopName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isHighlighted)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )

                        // Badge dla wybranych przystanków
                        if (isHighlighted) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiary
                            ) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (stop.delayMinutes > 0) {
                        Text(
                            text = "Opóźnienie: +${stop.delayMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Pokaż czas jeśli jest dostępny
                val displayTime = when {
                    isFirst && departureTime != null -> departureTime
                    stop.arrivalTime.isNotEmpty() -> stop.arrivalTime
                    else -> null
                }

                if (displayTime != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isHighlighted)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary
                        )

                        // Licznik "za X min" dla podświetlonych przystanków
                        if (isHighlighted) {
                            val estimatedMinutes = calculateMinutesUntil(displayTime)
                            if (estimatedMinutes != null && estimatedMinutes >= 0 && estimatedMinutes < 120) {
                                Text(
                                    text = when {
                                        estimatedMinutes == 0 -> "Teraz"
                                        estimatedMinutes < 60 -> "za $estimatedMinutes min"
                                        else -> "za ${estimatedMinutes / 60}h ${estimatedMinutes % 60}min"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        estimatedMinutes <= 3 -> MaterialTheme.colorScheme.error
                                        estimatedMinutes <= 10 -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
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