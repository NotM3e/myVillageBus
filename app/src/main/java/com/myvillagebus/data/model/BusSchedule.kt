package com.myvillagebus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.myvillagebus.data.local.Converters
import java.time.DayOfWeek
import java.util.Calendar

@Entity(tableName = "bus_schedules")
@TypeConverters(Converters::class)
data class BusSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val carrierId: String,      // FK do CarrierMetadata (np. "PKS-Grudziądz")
    val carrierName: String,    // Zachowane dla backward compatibility

    val departureTime: String,
    val direction: String,
    val lineDesignation: String? = null,
    val designationDescription: String? = null,
    val stopName: String,
    val busLine: String,

    // Lista dni tygodnia
    val operatingDays: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ),

    val stops: List<BusStop> = emptyList()
) {
    /**
     * Sprawdza czy kurs działa dziś
     */
    fun operatesToday(): Boolean {
        val today = getCurrentDayOfWeek()
        return operatingDays.contains(today)
    }

    /**
     * Sprawdza czy kurs działa w konkretny dzień
     */
    fun operatesOn(dayOfWeek: DayOfWeek): Boolean {
        return operatingDays.contains(dayOfWeek)
    }

    /**
     * Zwraca czytelny opis dni kursu
     */
    fun getOperatingDaysDescription(): String {
        return when {
            operatingDays.size == 7 -> "Codziennie"
            operatingDays.containsAll(weekdaysList()) && operatingDays.size == 5 -> "Dni powszednie (PN-PT)"
            operatingDays.containsAll(weekendsList()) && operatingDays.size == 2 -> "Weekendy (SO-ND)"
            operatingDays.size == 1 -> getDayNameInPolish(operatingDays.first())
            else -> operatingDays.joinToString(", ") { getDayNameInPolish(it) }
        }
    }

    /**
     * Zwraca krótki opis (dla listy)
     */
    fun getOperatingDaysShort(): String {
        return when {
            operatingDays.size == 7 -> "Codziennie"
            operatingDays.containsAll(weekdaysList()) && operatingDays.size == 5 -> "PN-PT"
            operatingDays.containsAll(weekendsList()) && operatingDays.size == 2 -> "SO-ND"
            operatingDays.size == 1 -> getDayAbbreviation(operatingDays.first())
            else -> operatingDays.joinToString(",") { getDayAbbreviation(it) }
        }
    }

    companion object {
        /**
         * Zwraca dzisiejszy dzień tygodnia
         */
        fun getCurrentDayOfWeek(): DayOfWeek {
            val calendar = Calendar.getInstance()
            val calendarDay = calendar.get(Calendar.DAY_OF_WEEK)

            // Calendar: niedziela=1, poniedziałek=2, ...
            // DayOfWeek: poniedziałek=1, wtorek=2, ..., niedziela=7
            return when (calendarDay) {
                Calendar.MONDAY -> DayOfWeek.MONDAY
                Calendar.TUESDAY -> DayOfWeek.TUESDAY
                Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
                Calendar.THURSDAY -> DayOfWeek.THURSDAY
                Calendar.FRIDAY -> DayOfWeek.FRIDAY
                Calendar.SATURDAY -> DayOfWeek.SATURDAY
                Calendar.SUNDAY -> DayOfWeek.SUNDAY
                else -> DayOfWeek.MONDAY
            }
        }

        /**
         * Lista dni powszednich
         */
        fun weekdaysList() = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        /**
         * Lista weekendów
         */
        fun weekendsList() = listOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        /**
         * Nazwa dnia po polsku
         */
        fun getDayNameInPolish(day: DayOfWeek): String {
            return when (day) {
                DayOfWeek.MONDAY -> "Poniedziałek"
                DayOfWeek.TUESDAY -> "Wtorek"
                DayOfWeek.WEDNESDAY -> "Środa"
                DayOfWeek.THURSDAY -> "Czwartek"
                DayOfWeek.FRIDAY -> "Piątek"
                DayOfWeek.SATURDAY -> "Sobota"
                DayOfWeek.SUNDAY -> "Niedziela"
            }
        }

        /**
         * Skrót dnia (PN, WT, ŚR, ...)
         */
        fun getDayAbbreviation(day: DayOfWeek): String {
            return when (day) {
                DayOfWeek.MONDAY -> "PN"
                DayOfWeek.TUESDAY -> "WT"
                DayOfWeek.WEDNESDAY -> "ŚR"
                DayOfWeek.THURSDAY -> "CZ"
                DayOfWeek.FRIDAY -> "PT"
                DayOfWeek.SATURDAY -> "SO"
                DayOfWeek.SUNDAY -> "ND"
            }
        }

        /**
         * Parsuje skrót na DayOfWeek
         */
        fun parseDayAbbreviation(abbreviation: String): DayOfWeek? {
            return when (abbreviation.uppercase().trim()) {
                "PN", "MON", "MONDAY" -> DayOfWeek.MONDAY
                "WT", "TUE", "TUESDAY" -> DayOfWeek.TUESDAY
                "ŚR", "SR", "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY
                "CZ", "THU", "THURSDAY" -> DayOfWeek.THURSDAY
                "PT", "FRI", "FRIDAY" -> DayOfWeek.FRIDAY
                "SO", "SAT", "SATURDAY" -> DayOfWeek.SATURDAY
                "ND", "SUN", "SUNDAY" -> DayOfWeek.SUNDAY
                else -> null
            }
        }
    }
}