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
 * Dialog do edycji istniejącego profilu
 * Podobny do SaveProfileDialog, ale z pre-filled wartościami
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditDialog(
    profile: Profile,
    viewModel: BusViewModel,
    onDismiss: () -> Unit
) {
    var profileName by remember { mutableStateOf(profile.name) }
    var selectedIcon by remember { mutableStateOf(profile.icon) }
    var customIcon by remember { mutableStateOf("") }
    var useCustomIcon by remember { mutableStateOf(!Profile.DEFAULT_ICONS.contains(profile.icon)) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Jeśli ikona nie jest domyślna, ustaw jako custom
    LaunchedEffect(Unit) {
        if (!Profile.DEFAULT_ICONS.contains(profile.icon)) {
            customIcon = profile.icon
        }
    }

    // Helper do wyświetlania filtrów
    val filtersSummary = buildString {
        if (profile.selectedCarriers.isNotEmpty()) {
            append("• Przewoźnicy: ${profile.selectedCarriers.joinToString(", ")}\n")
        }
        if (profile.selectedDesignations.isNotEmpty()) {
            append("• Oznaczenia: ${profile.selectedDesignations.joinToString(", ")}\n")
        }
        if (profile.selectedStops.isNotEmpty()) {
            append("• Przystanki: ${profile.selectedStops.joinToString(", ")}\n")
        }
        if (profile.selectedDirection != null) {
            append("• Kierunek: ${profile.selectedDirection}\n")
        }
        if (profile.selectedDay != null) {
            append("• Dzień: ${com.myvillagebus.data.model.BusSchedule.getDayNameInPolish(profile.selectedDay)}\n")
        }

        if (isEmpty()) {
            append("⚠️ Brak aktywnych filtrów")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("✏️ Edytuj filtr")
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
                    label = { Text("Nazwa filtru") },
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
                        if (it.length <= 2) {
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

                // Podgląd zapisanych filtrów (tylko read-only)
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
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "ℹ️ Filtry można zmienić tylko stosując profil i zapisując ponownie",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                        // Walidacja nazwy (exclude current profile)
                        val error = viewModel.validateProfileName(profileName, profile.id)
                        if (error != null) {
                            validationError = error
                            isSaving = false
                            return@launch
                        }

                        // Zaktualizuj profil (tylko nazwa i ikona)
                        val finalIcon = if (useCustomIcon && customIcon.isNotEmpty()) {
                            customIcon
                        } else {
                            selectedIcon
                        }

                        val updatedProfile = profile.copy(
                            name = profileName.trim(),
                            icon = finalIcon
                        )

                        viewModel.updateProfile(updatedProfile)

                        isSaving = false
                        onDismiss()
                    }
                },
                enabled = !isSaving && profileName.isNotBlank()
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