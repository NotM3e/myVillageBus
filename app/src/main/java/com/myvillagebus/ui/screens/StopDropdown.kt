package com.myvillagebus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDropdown(
    label: String,
    selectedStop: String?,
    availableStops: List<String>,
    onStopSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember(selectedStop) { mutableStateOf(selectedStop ?: "") }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Filtruj przystanki po wpisanym tekście
    val filteredStops = remember(availableStops, searchQuery) {
        if (searchQuery.isBlank()) {
            availableStops
        } else {
            availableStops.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Label
        Text(
            text = "$label",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newValue ->
                    searchQuery = newValue
                    expanded = true  // Otwórz dropdown podczas pisania

                    // Jeśli użytkownik wyczyścił pole, wyczyść też wybór
                    if (newValue.isBlank()) {
                        onStopSelected(null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            expanded = true
                        }
                    },
                placeholder = { Text("Wpisz lub wybierz...") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Przycisk czyszczenia
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    onStopSelected(null)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Wyczyść",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        // Strzałka rozwijania
                        IconButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (expanded) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Zwiń" else "Rozwiń",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Dropdown menu z wynikami
            ExposedDropdownMenu(
                expanded = expanded && filteredStops.isNotEmpty(),
                onDismissRequest = {
                    expanded = false
                    // Przywróć wybrany przystanek jeśli użytkownik nie wybrał nowego
                    if (selectedStop != null && searchQuery != selectedStop) {
                        searchQuery = selectedStop
                    }
                },
                modifier = Modifier
                    .heightIn(max = 250.dp)  // Maksymalna wysokość
                    .exposedDropdownSize(matchTextFieldWidth = true)
            ) {
                // Informacja o liczbie wyników
                if (filteredStops.size > 20) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Znaleziono ${filteredStops.size} przystanków - wpisz więcej liter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { },
                        enabled = false,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    )
                    HorizontalDivider()
                }

                // Lista przystanków (max 20 dla wydajności)
                filteredStops.take(20).forEach { stop ->
                    val isSelected = stop == selectedStop

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stop,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            searchQuery = stop
                            onStopSelected(stop)
                            expanded = false
                            focusManager.clearFocus()
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Text(
                                    text = "✓",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else null,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Informacja jeśli są jeszcze wyniki
                if (filteredStops.size > 20) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "... jeszcze ${filteredStops.size - 20} (wpisz dokładniej)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { },
                        enabled = false,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Komunikat "brak wyników"
            ExposedDropdownMenu(
                expanded = expanded && filteredStops.isEmpty() && searchQuery.isNotBlank(),
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Nie znaleziono przystanku \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { },
                    enabled = false
                )
            }
        }

        // Podpowiedź pod polem
        if (isFocused && searchQuery.isBlank() && availableStops.isNotEmpty()) {
            Text(
                text = "Dostępnych przystanków: ${availableStops.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp)
            )
        }
    }
}