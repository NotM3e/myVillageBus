package com.myvillagebus.data.model

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/**
 * Profil użytkownika zawierający zapisane filtry
 * Maksymalnie 10 profili per użytkownik
 */
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,  // Unikalne, max 20 znaków

    val icon: String,  // Emoji (user może wpisać własne)

    // Zapisane filtry
    val selectedCarriers: Set<String> = emptySet(),

    val selectedDesignations: Set<String> = emptySet(),

    val selectedStops: Set<String> = emptySet(),

    val selectedDirection: String? = null,  // Nullable single-select

    val selectedDay: DayOfWeek? = null,  // Nullable single-select

    // Metadata
    val createdAt: Long = System.currentTimeMillis(),

    val lastUsedAt: Long? = null  // Ostatnie użycie (dla sortowania)
) {

    // Cache dla wydajności (nie zapisywany w Room, tylko w RAM)
    @delegate:Transient
    private val _matchingSchedulesCache by lazy {
        mutableMapOf<Int, Int>()  // scheduleListHashCode → count
    }

    /**
     * Zwraca liczbę rozkładów pasujących do filtrów profilu
     * UWAGA: Wymaga przekazania wszystkich rozkładów z ViewModel
     *
     * ← POPRAWIONE: Dodano cache (hash list rozkładów → count)
     */
    fun getMatchingSchedulesCount(allSchedules: List<BusSchedule>): Int {
        // Cache key = hashCode listy rozkładów
        val cacheKey = allSchedules.hashCode()

        // Sprawdź cache
        _matchingSchedulesCache[cacheKey]?.let { cachedCount ->
            return cachedCount
        }

        // Oblicz na nowo
        val count = allSchedules.count { schedule ->
            val matchesCarrier = selectedCarriers.isEmpty() || selectedCarriers.contains(schedule.carrierName)
            val matchesDesignation = selectedDesignations.isEmpty() ||
                    selectedDesignations.all { designation ->
                        schedule.lineDesignation?.split(",")?.map { it.trim() }?.contains(designation) == true
                    }
            val matchesStop = selectedStops.isEmpty() ||
                    selectedStops.any { stop ->
                        schedule.stops.any { it.stopName == stop }
                    }
            val matchesDirection = selectedDirection == null || schedule.direction == selectedDirection
            val matchesDay = selectedDay?.let { schedule.operatesOn(it) } ?: true

            matchesCarrier && matchesDesignation && matchesStop && matchesDirection && matchesDay
        }

        // Zapisz w cache
        _matchingSchedulesCache[cacheKey] = count

        _matchingSchedulesCache[cacheKey]?.let { cachedCount ->
            Log.d("Profile", "Cache hit dla ${this.name}: $cachedCount")
            return cachedCount
        }
        Log.d("Profile", "Cache miss dla ${this.name}, obliczam...")

        return count
    }

    companion object {
        const val MAX_PROFILES = 10
        const val MAX_NAME_LENGTH = 20

        /**
         * Domyślne emoji do wyboru
         */
        val DEFAULT_ICONS = listOf(
            "🏠", "🏫", "💼", "🎉", "🚌",
            "🏥", "🛒", "⚽", "🎵", "🍕"
        )

        /**
         * Waliduje nazwę profilu
         */
        fun validateName(name: String, existingNames: List<String>, currentId: Int? = null): String? {
            val trimmed = name.trim()

            return when {
                trimmed.isBlank() -> "Nazwa nie może być pusta"
                trimmed.length > MAX_NAME_LENGTH -> "Maksymalnie $MAX_NAME_LENGTH znaków"
                existingNames.any { existing ->
                    existing.equals(trimmed, ignoreCase = true) &&
                            // Pozwól na tę samą nazwę jeśli edytujemy ten sam profil
                            (currentId == null || existing != name)
                } -> "Profil o tej nazwie już istnieje"
                else -> null
            }
        }
    }

    /**
     * Sprawdza czy profil ma jakiekolwiek aktywne filtry
     */
    fun hasActiveFilters(): Boolean {
        return selectedCarriers.isNotEmpty() ||
                selectedDesignations.isNotEmpty() ||
                selectedStops.isNotEmpty() ||
                selectedDirection != null ||
                selectedDay != null
    }

    /**
     * Zwraca krótki opis aktywnych filtrów
     */
    fun getFiltersSummary(): String {
        val parts = mutableListOf<String>()

        if (selectedCarriers.isNotEmpty()) {
            parts.add("${selectedCarriers.size} przewoźnik${if (selectedCarriers.size > 1) "ów" else ""}")
        }
        if (selectedStops.isNotEmpty()) {
            parts.add("${selectedStops.size} przystanek${if (selectedStops.size > 1) "i" else ""}")
        }
        if (selectedDay != null) {
            parts.add(BusSchedule.getDayAbbreviation(selectedDay))
        }

        return if (parts.isEmpty()) "Brak filtrów" else parts.joinToString(", ")
    }
}