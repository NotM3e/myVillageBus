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

        // ← POPRAWIONE: Obsługa midnight + edge case
        when {
            // Przypadek 1: Rozkład w przyszłości (dzisiaj)
            diff >= 0 && diff < 12 * 60 -> {
                // OK, normalny odjazd za X minut
            }

            // Przypadek 2: Rozkład już był (dzisiaj) - pokaż jako przeszłość
            diff < 0 && diff > -12 * 60 -> {
                // Rozkład był np. 30 minut temu - nie pokazuj "za 23h 30min"
                return null  // ← Zwróć null (nie pokazuj licznika)
            }

            // Przypadek 3: Rozkład jest jutro (przejście przez północ)
            diff < 0 -> {
                diff += 24 * 60  // +1440 minut
            }

            // Przypadek 4: Rozkład jest za >12h (jutro rano)
            diff >= 12 * 60 -> {
                // OK, pokazuj "za Xh Ymin"
            }
        }

        diff.toInt()

    } catch (e: Exception) {
        null
    }
}