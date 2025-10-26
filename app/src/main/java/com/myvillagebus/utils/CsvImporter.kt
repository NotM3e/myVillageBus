package com.myvillagebus.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.model.BusStop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.time.DayOfWeek
import com.myvillagebus.data.remote.RemoteConfig
import com.myvillagebus.data.remote.CarrierInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CsvImporter {

    /**
     * Rozpoznaje separator w danych (CSV lub TSV)
     */
    private fun detectSeparator(csvContent: String): String {
        val firstLine = csvContent.lines().firstOrNull() ?: return "\t"

        return when {
            firstLine.contains("\t") -> "\t"  // TSV
            firstLine.contains(",") -> ","    // CSV
            else -> "\t"  // Domyślnie TSV
        }
    }

    /**
     * Dzieli linię używając automatycznie wykrytego separatora
     */
    private fun splitLine(line: String, separator: String = "\t"): List<String> {
        return line.split(separator).map { it.trim() }
    }

    /**
     * Parsuje arkusz Config
     */
    fun parseConfig(csvContent: String): RemoteConfig? {
        return try {
            val map = mutableMapOf<String, String>()
            val lines = csvContent.lines()
            val separator = detectSeparator(csvContent)

            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach

                val parts = splitLine(line, separator)
                if (parts.size >= 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    map[key] = value
                }
            }

            val version = map["version"] ?: return null
            val carriersGid = map["carriers_gid"] ?: return null
            val baseUrl = map["base_url"] ?: return null

            RemoteConfig(
                version = version,
                lastUpdate = map["last_update"],
                minAppVersion = map["min_app_version"],
                carriersGid = carriersGid,
                baseUrl = baseUrl
            )
        } catch (e: Exception) {
            Log.e("CsvImporter", "Błąd parsowania Config", e)
            null
        }
    }

    /**
     * Parsuje arkusz Carriers
     */
    fun parseCarriers(csvContent: String): List<CarrierInfo> {
        val carriers = mutableListOf<CarrierInfo>()
        val lines = csvContent.lines()
        val separator = detectSeparator(csvContent)

        lines.drop(1).forEach { line ->
            if (line.isBlank()) return@forEach

            try {
                val parts = splitLine(line, separator)
                if (parts.size < 5) return@forEach

                val carrierName = parts[0]
                val gid = parts[1]
                val color = parts.getOrNull(2)?.ifEmpty { null }
                val icon = parts.getOrNull(3)?.ifEmpty { null }
                val activeString = parts[4]
                val description = parts.getOrNull(5)?.ifEmpty { null }

                val active = when (activeString.uppercase()) {
                    "TRUE", "1", "YES", "TAK" -> true
                    else -> false
                }

                carriers.add(
                    CarrierInfo(
                        carrierName = carrierName,
                        gid = gid,
                        color = color,
                        icon = icon,
                        active = active,
                        description = description
                    )
                )

            } catch (e: Exception) {
                Log.e("CsvImporter", "Błąd parsowania linii Carriers: $line", e)
            }
        }

        return carriers
    }

    /**
     * UNIWERSALNY parser CSV dla dowolnego przewoźnika
     */
    fun parseUniversalCsv(csvContent: String, carrierName: String): List<BusSchedule> {
        val schedules = mutableListOf<BusSchedule>()
        val lines = csvContent.lines()
        val separator = detectSeparator(csvContent)

        if (lines.size < 2) return emptyList()

        lines.drop(1).forEach { line ->
            if (line.isBlank()) return@forEach

            try {
                val parts = splitLine(line, separator)

                if (parts.size < 7) {
                    Log.w("CsvImporter", "Pominięto linię (za mało kolumn, expected ≥7, got ${parts.size}): $line")
                    return@forEach
                }

                val lineDesignation = parts[0]
                val designationDescription = parts[1].ifEmpty { null }
                val busLine = parts[2]
                val departureTime = parts[3]
                val stopNameFromCsv = parts[4]
                val direction = parts[5]
                val daysString = parts[6]
                val stopsString = parts.getOrNull(7) ?: ""

                val stopName = if (stopNameFromCsv.isEmpty()) {
                    extractStartStop(busLine, direction)
                } else {
                    stopNameFromCsv
                }

                val operatingDays = parseDays(daysString)

                val stops = generateStopsFromRoute(
                    startStop = stopName,
                    endStop = direction,
                    departureTime = departureTime,
                    routeStops = stopsString
                )

                // ZMIANA: Nie rozdzielaj oznaczeń - dodaj jeden rozkład
                schedules.add(
                    BusSchedule(
                        carrierName = carrierName,
                        lineDesignation = lineDesignation.ifEmpty { null },
                        designationDescription = designationDescription,
                        busLine = busLine,
                        departureTime = departureTime,
                        stopName = stopName,
                        direction = direction,
                        operatingDays = operatingDays,
                        stops = stops
                    )
                )

            } catch (e: Exception) {
                Log.e("CsvImporter", "Błąd parsowania linii: $line", e)
            }
        }

        return schedules
    }

    // ========== FUNKCJE POMOCNICZE ==========

    private fun extractStartStop(busLine: String, direction: String): String {
        val parts = busLine.split("-").map { it.trim() }
        return if (parts.size == 2) {
            if (parts[1] == direction) parts[0] else parts[1]
        } else {
            parts.firstOrNull() ?: busLine
        }
    }

    private fun parseDays(daysString: String): List<DayOfWeek> {
        if (daysString.isBlank()) return BusSchedule.weekdaysList()

        return daysString.split(",").mapNotNull { day ->
            BusSchedule.parseDayAbbreviation(day.trim())
        }
    }

    private fun generateStopsFromRoute(
        startStop: String,
        endStop: String,
        departureTime: String,
        routeStops: String
    ): List<BusStop> {
        val stops = mutableListOf<BusStop>()

        // 1. Przystanek startowy
        stops.add(BusStop(startStop, departureTime, 0))

        // 2. NOWE: Parsuj przystanki pośrednie z kolumny "stops"
        if (routeStops.isNotBlank()) {
            val intermediateStops = routeStops
                .split(",", ";")  // Akceptuj przecinek lub średnik
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != startStop && it != endStop }

            // Dodaj przystanki z szacowanym czasem (co 5 minut)
            intermediateStops.forEachIndexed { index, stopName ->
                val estimatedTime = addMinutesToTime(departureTime, (index + 1) * 5)
                stops.add(BusStop(stopName, estimatedTime, 0))
            }
        }

        // 3. Przystanek końcowy
        val totalMinutes = if (routeStops.isNotBlank()) {
            // Oblicz czas na podstawie liczby przystanków (5 min/przystanek)
            val intermediateCount = routeStops.split(",", ";").size
            (intermediateCount + 1) * 5
        } else {
            20  // Domyślnie 20 minut jeśli brak przystanków pośrednich
        }

        val arrivalTime = addMinutesToTime(departureTime, totalMinutes)
        stops.add(BusStop(endStop, arrivalTime, 0))

        return stops
    }

    private fun addMinutesToTime(time: String, minutes: Int): String {
        val parts = time.split(":").map { it.toIntOrNull() ?: 0 }
        var hour = parts[0]
        var minute = parts[1] + minutes

        if (minute >= 60) {
            hour += minute / 60
            minute %= 60
        }

        hour %= 24

        return String.format("%02d:%02d", hour, minute)
    }

    suspend fun downloadCsvFromUrl(url: String): String {
        return withContext(Dispatchers.IO) {  // ← Użyj wątku IO
            try {
                Log.d("CsvImporter", "Pobieranie: $url")

                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000  // 10 sekund
                connection.readTimeout = 10000

                val text = connection.getInputStream().bufferedReader().use { it.readText() }

                Log.d("CsvImporter", "✅ Pobrano ${text.length} znaków")
                text

            } catch (e: Exception) {
                Log.e("CsvImporter", "❌ Błąd pobierania z $url: ${e.message}", e)
                throw e
            }
        }
    }

    suspend fun getRemoteVersion(configUrl: String): String? {
        return try {
            val csv = downloadCsvFromUrl(configUrl)
            parseConfig(csv)?.version
        } catch (e: Exception) {
            Log.e("CsvImporter", "Błąd pobierania wersji", e)
            null
        }
    }

    suspend fun getRemoteConfig(configUrl: String): RemoteConfig? {
        return try {
            val csv = downloadCsvFromUrl(configUrl)
            parseConfig(csv)
        } catch (e: Exception) {
            Log.e("CsvImporter", "Błąd pobierania config", e)
            null
        }
    }
}