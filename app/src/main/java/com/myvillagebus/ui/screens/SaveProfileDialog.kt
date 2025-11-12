package com.myvillagebus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myvillagebus.data.model.Profile
import com.myvillagebus.ui.viewmodel.BusViewModel
import kotlinx.coroutines.launch

/**
 * Dialog do zapisywania aktualnych filtrów jako profil
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveProfileDialog(
    currentFilters: Map<String, Any?>,  // carriers, designations, stops, direction, day
    viewModel: BusViewModel,
    onDismiss: () -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(Profile.DEFAULT_ICONS.first()) }
    var customIcon by remember { mutableStateOf("") }
    var useCustomIcon by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Helper do wyświetlania filtrów
    val filtersSummary = buildString {
        val carriers = currentFilters["carriers"] as? Set<*>
        val designations = currentFilters["designations"] as? Set<*>
        val stops = currentFilters["stops"] as? Set<*>
        val direction = currentFilters["direction"] as? String
        val day = currentFilters["day"]

        if (!carriers.isNullOrEmpty()) {
            append("• Przewoźnicy: ${carriers.joinToString(", ")}\n")
        }
        if (!designations.isNullOrEmpty()) {
            append("• Oznaczenia: ${designations.joinToString(", ")}\n")
        }
        if (!stops.isNullOrEmpty()) {
            append("• Przystanki: ${stops.joinToString(", ")}\n")
        }
        if (!direction.isNullOrEmpty()) {
            append("• Kierunek: $direction\n")
        }
        if (day != null) {
            append("• Dzień: $day\n")
        }

        if (isEmpty()) {
            append("⚠️ Brak aktywnych filtrów")
        }
    }

    val hasActiveFilters = currentFilters.values.any {
        when (it) {
            is Set<*> -> it.isNotEmpty()
            is String -> it.isNotEmpty()
            null -> false
            else -> true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("💾 Zapisz jako profil")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nazwa profilu
                OutlinedTextField(
                    value = profileName,
                    onValueChange = {
                        if (it.length <= Profile.MAX_NAME_LENGTH) {
                            profileName = it
                            validationError = null
                        }
                    },
                    label = { Text("Nazwa profilu") },
                    placeholder = { Text("np. SZKOŁA, PRACA, DOM") },
                    isError = validationError != null,
                    supportingText = {
                        if (validationError != null) {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("${profileName.length}/${Profile.MAX_NAME_LENGTH}")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // Wybór ikony
                Text(
                    text = "Wybierz ikonę:",
                    style = MaterialTheme.typography.labelMedium
                )

                // Grid z predefiniowanymi emoji
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(Profile.DEFAULT_ICONS) { icon ->
                        IconButton(
                            onClick = {
                                selectedIcon = icon
                                useCustomIcon = false
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = icon,
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center
                                )

                                // Checkmark jeśli wybrane
                                if (icon == selectedIcon && !useCustomIcon) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Wybrane",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .padding(2.dp)
                                                .size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Własne emoji
                OutlinedTextField(
                    value = customIcon,
                    onValueChange = {
                        if (it.length <= 2) {  // Max 2 znaki (niektóre emoji to 2 chars)
                            customIcon = it
                            if (it.isNotEmpty()) {
                                useCustomIcon = true
                                selectedIcon = it
                            }
                        }
                    },
                    label = { Text("Lub wpisz własne emoji") },
                    placeholder = { Text("🚀") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // Podgląd zapisanych filtrów
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Zapisane filtry:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = filtersSummary.trim(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true

                        // Walidacja nazwy
                        val error = viewModel.validateProfileName(profileName)
                        if (error != null) {
                            validationError = error
                            isSaving = false
                            return@launch
                        }

                        // Sprawdź limit profili
                        if (!viewModel.canCreateProfile()) {
                            validationError = "Maksymalnie ${Profile.MAX_PROFILES} profili"
                            isSaving = false
                            return@launch
                        }

                        // Zapisz profil
                        val finalIcon = if (useCustomIcon && customIcon.isNotEmpty()) {
                            customIcon
                        } else {
                            selectedIcon
                        }

                        viewModel.createProfileFromCurrentFilters(
                            name = profileName.trim(),
                            icon = finalIcon,
                            filters = currentFilters
                        )

                        isSaving = false
                        onDismiss()
                    }
                },
                enabled = !isSaving && profileName.isNotBlank() && hasActiveFilters
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isSaving) "Zapisywanie..." else "Zapisz")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Anuluj")
            }
        }
    )
}