package com.myvillagebus.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.myvillagebus.data.model.BusStop
import java.time.DayOfWeek

class Converters {
    private val gson = Gson()

    // Konwerter dla List<BusStop>
    @TypeConverter
    fun fromBusStopList(value: List<BusStop>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBusStopList(value: String): List<BusStop> {
        val listType = object : TypeToken<List<BusStop>>() {}.type
        return gson.fromJson(value, listType)
    }

    // ← NOWE: Konwerter dla List<DayOfWeek>
    @TypeConverter
    fun fromDayOfWeekList(value: List<DayOfWeek>): String {
        // Zapisujemy jako "MONDAY,TUESDAY,WEDNESDAY,..."
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekList(value: String): List<DayOfWeek> {
        if (value.isBlank()) return emptyList()
        return value.split(",").mapNotNull { dayName ->
            try {
                DayOfWeek.valueOf(dayName.trim())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}