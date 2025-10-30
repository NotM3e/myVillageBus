package com.myvillagebus.data.repository

import android.util.Log
import com.myvillagebus.data.local.BusScheduleDao
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.utils.CsvImporter
import com.myvillagebus.utils.PreferencesManager
import com.myvillagebus.utils.CarrierVersionManager
import kotlinx.coroutines.flow.Flow

class BusScheduleRepository(
    private val dao: BusScheduleDao,
    private val preferencesManager: PreferencesManager,
    private val carrierVersionManager: CarrierVersionManager  // NOWE
) {

    val allSchedules: Flow<List<BusSchedule>> = dao.getAllSchedules()
    val allCarriers: Flow<List<String>> = dao.getAllCarriers()
    val allDesignations: Flow<List<String>> = dao.getAllDesignations()

    suspend fun getScheduleById(id: Int): BusSchedule? {
        return dao.getScheduleById(id)
    }

    fun getSchedulesByCarrier(carrierName: String): Flow<List<BusSchedule>> {
        return dao.getSchedulesByCarrier(carrierName)
    }

    fun getSchedulesByStop(stopName: String): Flow<List<BusSchedule>> {
        return dao.getSchedulesByStop(stopName)
    }

    suspend fun insertSchedule(schedule: BusSchedule): Long {
        return dao.insertSchedule(schedule)
    }

    suspend fun insertSchedules(schedules: List<BusSchedule>) {
        dao.insertSchedules(schedules)
    }

    suspend fun updateSchedule(schedule: BusSchedule) {
        dao.updateSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: BusSchedule) {
        dao.deleteSchedule(schedule)
    }

    suspend fun deleteAllSchedules() {
        dao.deleteAllSchedules()
        preferencesManager.clearSyncData()
    }

    suspend fun getSchedulesCount(): Int {
        return dao.getSchedulesCount()
    }

    suspend fun initializeSampleData(schedules: List<BusSchedule>) {
        val count = dao.getSchedulesCount()
        if (count == 0) {
            dao.insertSchedules(schedules)
        }
    }

    /**
     * Synchronizacja z Google Sheets
     *
     * @param configUrl URL do arkusza Config
     * @param forceSync Wymuś synchronizację nawet jeśli wersja się nie zmieniła
     * @return Result z komunikatem sukcesu lub błędu
     */
    suspend fun syncWithGoogleSheets(
        configUrl: String,
        forceSync: Boolean = false
    ): Result<String> {
        return try {
            Log.d("Sync", "Rozpoczynam synchronizację...")

            // 1. Pobierz Config
            val config = CsvImporter.getRemoteConfig(configUrl)
                ?: return Result.failure(Exception("Nie można pobrać Config"))

            Log.d("Sync", "Config pobrany: version=${config.version}")

            // 2. Pobierz Carriers
            val carriersUrl = config.getCarriersUrl()
            val carriersCsv = CsvImporter.downloadCsvFromUrl(carriersUrl)
            val carriers = CsvImporter.parseCarriers(carriersCsv)
                .filter { it.isValid() }

            Log.d("Sync", "Znaleziono ${carriers.size} aktywnych przewoźników")

            if (carriers.isEmpty()) {
                return Result.failure(Exception("Brak aktywnych przewoźników"))
            }

            // 3. Pobierz dane TYLKO dla przewoźników które się zmieniły
            val allSchedules = mutableListOf<BusSchedule>()
            var updatedCount = 0
            var skippedCount = 0

            carriers.forEach { carrier ->
                val needsUpdate = forceSync ||
                        carrierVersionManager.needsUpdate(carrier.carrierName, carrier.version)

                if (needsUpdate) {
                    Log.d("Sync", "📥 Pobieranie: ${carrier.carrierName} (wersja: ${carrier.version})")

                    try {
                        val dataUrl = config.buildSheetUrl(carrier.gid, "tsv")
                        val dataCsv = CsvImporter.downloadCsvFromUrl(dataUrl)
                        val schedules = CsvImporter.parseUniversalCsv(dataCsv, carrier.carrierName)

                        dao.deleteSchedulesByCarrier(carrier.carrierName)
                        dao.insertSchedules(schedules)
                        allSchedules.addAll(schedules)

                        // NOWE: Zapisz wersję jako Int
                        carrier.version?.let { version ->
                            carrierVersionManager.saveCarrierVersion(carrier.carrierName, version)
                        }

                        updatedCount++
                        Log.d("Sync", "✅ ${carrier.carrierName}: ${schedules.size} rozkładów (v${carrier.version})")

                    } catch (e: Exception) {
                        Log.e("Sync", "❌ Błąd: ${carrier.carrierName}: ${e.message}", e)
                    }
                } else {
                    skippedCount++
                    val localVer = carrierVersionManager.getCarrierVersion(carrier.carrierName)
                    Log.d("Sync", "⏭️  Pominięto: ${carrier.carrierName} (lokalna: v$localVer, zdalna: v${carrier.version})")
                }
            }

            // Zapisz globalną wersję i czas
            preferencesManager.saveLastSyncVersion(config.version)
            preferencesManager.saveLastSyncTime()

            // Przygotuj komunikat
            val message = when {
                updatedCount == 0 && skippedCount > 0 ->
                    "Wszystkie dane są aktualne"

                updatedCount > 0 && skippedCount == 0 ->
                    "Zsynchronizowano $updatedCount ${if (updatedCount == 1) "przewoźnika" else "przewoźników"}"

                updatedCount > 0 && skippedCount > 0 ->
                    "Zsynchronizowano $updatedCount ${if (updatedCount == 1) "przewoźnika" else "przewoźników"} (${skippedCount} bez zmian)"

                else ->
                    "Synchronizacja zakończona"
            }

            Log.d("Sync", "✅ $message")

            Result.success(message)

        } catch (e: Exception) {
            Log.e("Sync", "❌ Błąd synchronizacji: ${e.message}", e)
            Result.failure(e)
        }
    }
}