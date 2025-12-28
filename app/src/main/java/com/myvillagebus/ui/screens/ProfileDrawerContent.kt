package com.myvillagebus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.model.Profile

/**
 * Zawartość Navigation Drawer - lista profili
 */
@Composable
fun ProfileDrawerContent(
    profiles: List<Profile>,
    allSchedules: List<BusSchedule>,
    currentProfileId: Int?,
    onProfileClick: (Int) -> Unit,
    onCreateNewClick: () -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "💾 Zapisane filtry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${profiles.size}/${Profile.MAX_PROFILES} zapisanych filtrów",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Lista profili
        if (profiles.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📂",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = "Brak zapisanych filtrów",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Utwórz pierwszy filtr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileDrawerItem(
                        profile = profile,
                        allSchedules = allSchedules,
                        isActive = profile.id == currentProfileId,
                        onClick = { onProfileClick(profile.id) }
                    )
                }
            }
        }

        HorizontalDivider()

        // Akcje na dole
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Nowy profil
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                label = { Text("Nowy filtr") },
                selected = false,
                onClick = onCreateNewClick,
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // Zarządzaj profilami
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Zarządzaj filtrami") },
                selected = false,
                onClick = onManageClick
            )
        }
    }
}

/**
 * Item pojedynczego profilu w Drawer
 */
@Composable
fun ProfileDrawerItem(
    profile: Profile,
    allSchedules: List<BusSchedule>,
    isActive: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Text(
                text = profile.icon,
                style = MaterialTheme.typography.titleLarge
            )
        },
        label = {
            Column {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                // Pokaż summary filtrów
                if (profile.hasActiveFilters()) {
                    val schedulesCount = profile.getMatchingSchedulesCount(allSchedules)
                    Text(
                        text = if (schedulesCount > 0) {
                            "$schedulesCount rozkład${when {
                                schedulesCount == 1 -> ""
                                schedulesCount < 5 -> "y"
                                else -> "ów"
                            }}"
                        } else {
                            "Brak pasujących rozkładów"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        badge = {
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Aktywny",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        selected = isActive,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}