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
                if (parts.size < 4) return@forEach  // Minimum: nazwa, gid, active, version

                val carrierName = parts[0]
                val gid = parts[1]
                val activeString = parts[2]
                val versionString = parts.getOrNull(3)?.trim()
                val description = parts.getOrNull(4)?.ifEmpty { null }

                val active = when (activeString.uppercase()) {
                    "TRUE", "1", "YES", "TAK" -> true
                    else -> false
                }

                val version = versionString?.toIntOrNull()

                carriers.add(
                    CarrierInfo(
                        carrierName = carrierName,
                        gid = gid,
                        active = active,
                        version = version,
                        description = description
                    )
                )

                Log.d("CsvImporter", "Przewoźnik: $carrierName, wersja: $version, aktywny: $active")

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
        if (routeStops.isBlank()) {
            return emptyList()
        }

        return routeStops.split(",", ";").mapNotNull { stopData ->
            val trimmed = stopData.trim()
            if (trimmed.isEmpty()) return@mapNotNull null

            // Sprawdź czy jest dwukropek (nazwa:czas)
            if (trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                val stopName = parts[0].trim()
                val time = parts.getOrNull(1)?.trim() ?: ""

                // Walidacja czasu (opcjonalnie)
                val validTime = if (time.matches(Regex("\\d{1,2}:\\d{2}"))) {
                    time
                } else {
                    ""
                }

                BusStop(
                    stopName = stopName,
                    arrivalTime = validTime,
                    delayMinutes = 0
                )
            } else {
                // Tylko nazwa przystanku (bez czasu)
                BusStop(
                    stopName = trimmed,
                    arrivalTime = "",
                    delayMinutes = 0
                )
            }
        }
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