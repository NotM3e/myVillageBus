package com.myvillagebus.data.repository

import android.util.Log
import com.myvillagebus.data.local.BusScheduleDao
import com.myvillagebus.data.model.BusSchedule
import kotlinx.coroutines.flow.Flow
import com.myvillagebus.utils.CsvImporter

class BusScheduleRepository(private val dao: BusScheduleDao) {

    // Wszystkie rozkłady jako Flow (automatyczna aktualizacja UI)
    val allSchedules: Flow<List<BusSchedule>> = dao.getAllSchedules()

    // Wszyscy przewoźnicy
    val allCarriers: Flow<List<String>> = dao.getAllCarriers()

    // Wszystkie oznaczenia
    val allDesignations: Flow<List<String>> = dao.getAllDesignations()

    // Pobierz rozkład po ID
    suspend fun getScheduleById(id: Int): BusSchedule? {
        return dao.getScheduleById(id)
    }

    // Pobierz rozkłady po przewoźniku
    fun getSchedulesByCarrier(carrierName: String): Flow<List<BusSchedule>> {
        return dao.getSchedulesByCarrier(carrierName)
    }

    // Pobierz rozkłady po przystanku
    fun getSchedulesByStop(stopName: String): Flow<List<BusSchedule>> {
        return dao.getSchedulesByStop(stopName)
    }

    // Wstaw rozkład
    suspend fun insertSchedule(schedule: BusSchedule): Long {
        return dao.insertSchedule(schedule)
    }

    // Wstaw wiele rozkładów (import z CSV)
    suspend fun insertSchedules(schedules: List<BusSchedule>) {
        dao.insertSchedules(schedules)
    }

    // Aktualizuj rozkład
    suspend fun updateSchedule(schedule: BusSchedule) {
        dao.updateSchedule(schedule)
    }

    // Usuń rozkład
    suspend fun deleteSchedule(schedule: BusSchedule) {
        dao.deleteSchedule(schedule)
    }

    // Usuń wszystkie rozkłady
    suspend fun deleteAllSchedules() {
        dao.deleteAllSchedules()
    }

    // Liczba rozkładów
    suspend fun getSchedulesCount(): Int {
        return dao.getSchedulesCount()
    }

    // Inicjalizacja z przykładowymi danymi (tymczasowo)
    suspend fun initializeSampleData(schedules: List<BusSchedule>) {
        val count = dao.getSchedulesCount()
        if (count == 0) {
            dao.insertSchedules(schedules)
        }
    }

    /**
     * Synchronizacja z Google Sheets
     *
     * @param configUrl URL do arkusza Config (z GID)
     * @param forceSync Wymuś synchronizację nawet jeśli wersja się nie zmieniła
     * @return true jeśli synchronizacja się powiodła
     */
    suspend fun syncWithGoogleSheets(
        configUrl: String,
        forceSync: Boolean = false
    ): Result<String> {
        return try {
            Log.d("Sync", "🔄 Rozpoczynam synchronizację...")

            // 1. Pobierz Config
            Log.d("Sync", "📥 Pobieranie Config...")
            val config = CsvImporter.getRemoteConfig(configUrl)
                ?: return Result.failure(Exception("Nie można pobrać Config"))

            Log.d("Sync", "✅ Config: version=${config.version}")

            // TODO: Sprawdź wersję lokalną vs zdalną (pominiemy na razie)

            // 2. Pobierz Carriers
            Log.d("Sync", "📥 Pobieranie Carriers...")
            val carriersUrl = config.getCarriersUrl()
            val carriersCsv = CsvImporter.downloadCsvFromUrl(carriersUrl)
            val carriers = CsvImporter.parseCarriers(carriersCsv)
                .filter { it.isValid() }

            Log.d("Sync", "✅ Carriers: ${carriers.size} przewoźników")

            if (carriers.isEmpty()) {
                return Result.failure(Exception("Brak aktywnych przewoźników"))
            }

            // 3. Pobierz dane dla każdego przewoźnika
            val allSchedules = mutableListOf<BusSchedule>()

            carriers.forEach { carrier ->
                Log.d("Sync", "📥 Pobieranie danych: ${carrier.carrierName}...")

                try {
                    val dataUrl = config.buildSheetUrl(carrier.gid, "tsv")
                    val dataCsv = CsvImporter.downloadCsvFromUrl(dataUrl)
                    val schedules = CsvImporter.parseUniversalCsv(dataCsv, carrier.carrierName)

                    allSchedules.addAll(schedules)
                    Log.d("Sync", "✅ ${carrier.carrierName}: ${schedules.size} rozkładów")

                } catch (e: Exception) {
                    Log.e("Sync", "❌ Błąd pobierania ${carrier.carrierName}: ${e.message}")
                    // Kontynuuj dla innych przewoźników
                }
            }

            if (allSchedules.isEmpty()) {
                return Result.failure(Exception("Nie pobrano żadnych rozkładów"))
            }

            // 4. Zapisz do bazy danych
            Log.d("Sync", "💾 Zapisywanie do bazy...")
            dao.deleteAllSchedules()
            dao.insertSchedules(allSchedules)

            Log.d("Sync", "✅ Synchronizacja zakończona! Zapisano ${allSchedules.size} rozkładów")

            Result.success("Zsynchronizowano ${allSchedules.size} rozkładów (wersja ${config.version})")

        } catch (e: Exception) {
            Log.e("Sync", "❌ Błąd synchronizacji: ${e.message}", e)
            Result.failure(e)
        }
    }
}