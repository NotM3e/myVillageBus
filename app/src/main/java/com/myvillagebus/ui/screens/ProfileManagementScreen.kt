package com.myvillagebus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myvillagebus.data.model.Profile
import com.myvillagebus.ui.viewmodel.BusViewModel
import com.myvillagebus.utils.rememberDebouncedClick

/**
 * Ekran zarządzania profilami - fullscreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    viewModel: BusViewModel,
    onBackClick: () -> Unit
) {
    // Debounced back click
    val debouncedBackClick = rememberDebouncedClick(onClick = onBackClick)

    val allProfiles by viewModel.allProfiles.collectAsState()
    val profileStatus by viewModel.profileOperationStatus.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<Profile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Pokaż Snackbar po operacji
    LaunchedEffect(profileStatus) {
        profileStatus?.let { status ->
            snackbarHostState.showSnackbar(
                message = status,
                duration = SnackbarDuration.Short
            )
            viewModel.clearProfileStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zarządzaj profilami") },
                navigationIcon = {
                    IconButton(onClick = debouncedBackClick) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (allProfiles.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "📂",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "Brak profili",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Utworz profile w głównym ekranie",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header info
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "📊 Statystyki",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Liczba profili: ${allProfiles.size}/${Profile.MAX_PROFILES}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (allProfiles.size >= Profile.MAX_PROFILES) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "⚠️ LIMIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Lista profili
                items(allProfiles, key = { it.id }) { profile ->
                    ProfileManagementItem(
                        profile = profile,
                        onEditClick = {
                            profileToEdit = profile
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            profileToDelete = profile
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Dialog edycji profilu
    if (showEditDialog && profileToEdit != null) {
        ProfileEditDialog(
            profile = profileToEdit!!,
            viewModel = viewModel,
            onDismiss = {
                showEditDialog = false
                profileToEdit = null
            }
        )
    }

    // Dialog potwierdzenia usunięcia
    if (showDeleteDialog && profileToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                profileToDelete = null
            },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Usuń profil?") },
            text = {
                Text("Czy na pewno chcesz usunąć profil '${profileToDelete!!.name}'? Ta operacja jest nieodwracalna.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(profileToDelete!!.id)
                        showDeleteDialog = false
                        profileToDelete = null
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        profileToDelete = null
                    }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }
}

/**
 * Item profilu w ekranie zarządzania
 */
@Composable
fun ProfileManagementItem(
    profile: Profile,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
            // Ikona + Info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.icon,
                    style = MaterialTheme.typography.displaySmall
                )

                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = profile.getFiltersSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Utworzono: ${java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(profile.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Akcje
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Edit
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edytuj",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Usuń",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}