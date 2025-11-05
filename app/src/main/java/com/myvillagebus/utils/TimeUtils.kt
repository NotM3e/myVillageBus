package com.myvillagebus.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Oblicza ile minut pozostało do odjazdu
 * Obsługuje przejście przez północ (23:55 → 00:10)
 *
 * @param departureTime Czas odjazdu w formacie "HH:mm"
 * @return Liczba minut do odjazdu lub null jeśli błąd parsowania
 */
fun calculateMinutesUntil(departureTime: String): Int? {
    return try {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        val departure = Calendar.getInstance()

        val time = format.parse(departureTime) ?: return null

        departure.time = time
        departure.set(Calendar.YEAR, now.get(Calendar.YEAR))
        departure.set(Calendar.MONTH, now.get(Calendar.MONTH))
        departure.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

        var diff = (departure.timeInMillis - now.timeInMillis) / (60 * 1000)

        // Jeśli ujemne (przeszłość), dodaj 24h (zakładamy że to następny dzień)
        if (diff < 0) {
            diff += 24 * 60  // +1440 minut
        }

        diff.toInt()

    } catch (e: Exception) {
        null
    }
}