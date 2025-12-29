package com.myvillagebus.data.model

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/**
 * Profil użytkownika zawierający zapisane filtry   -> profil = zapisany filtr
 * Maksymalnie 10 profili per użytkownik            -> profil = zapisany filtr
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

    // Zamiast selectedStops (Set) -> fromStop i toStop (String?)
    val fromStop: String? = null,

    val toStop: String? = null,

    val selectedDirection: String? = null,  // zastąpione przez toStop

    val selectedDay: DayOfWeek? = null,

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
     */
    fun getMatchingSchedulesCount(allSchedules: List<BusSchedule>): Int {
        val cacheKey = allSchedules.hashCode()

        _matchingSchedulesCache[cacheKey]?.let { cachedCount ->
            return cachedCount
        }

        val count = allSchedules.count { schedule ->
            val matchesCarrier = selectedCarriers.isEmpty() || selectedCarriers.contains(schedule.carrierName)

            val matchesDesignation = selectedDesignations.isEmpty() ||
                    selectedDesignations.all { designation ->
                        schedule.lineDesignation?.split(",")?.map { it.trim() }?.contains(designation) == true
                    }

            // fromStop / toStop
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
        }

        _matchingSchedulesCache[cacheKey] = count
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
                fromStop != null ||
                toStop != null ||
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
        if (fromStop != null || toStop != null) {
            val route = listOfNotNull(fromStop, toStop).joinToString(" → ")
            parts.add(route)
        }
        if (selectedDay != null) {
            parts.add(BusSchedule.getDayAbbreviation(selectedDay))
        }

        return if (parts.isEmpty()) "Brak filtrów" else parts.joinToString(", ")
    }

}