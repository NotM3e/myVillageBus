package com.myvillagebus.data.repository

import android.util.Log
import com.myvillagebus.data.local.BusScheduleDao
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.utils.CsvImporter
import com.myvillagebus.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow

class BusScheduleRepository(
    private val dao: BusScheduleDao,
    private val preferencesManager: PreferencesManager
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
            Log.d("Sync", "URL Config: $configUrl")

            // 1. Pobierz Config
            Log.d("Sync", "Pobieranie Config...")
            val config = CsvImporter.getRemoteConfig(configUrl)
                ?: return Result.failure(Exception("Nie można pobrać Config"))

            Log.d("Sync", "Config pobrany:")
            Log.d("Sync", "   - version: ${config.version}")
            Log.d("Sync", "   - carriers_gid: ${config.carriersGid}")
            Log.d("Sync", "   - base_url: ${config.baseUrl}")

            // 2. Sprawdź czy potrzebna synchronizacja
            val localVersion = preferencesManager.getLastSyncVersion()
            val remoteVersion = config.version

            if (!forceSync && localVersion == remoteVersion) {
                Log.d("Sync", "Dane są aktualne (wersja: $remoteVersion)")
                return Result.success("Dane są już aktualne (wersja $remoteVersion)")
            }

            Log.d("Sync", "Wykryto nową wersję: $localVersion -> $remoteVersion")

            // 3. Pobierz Carriers
            val carriersUrl = config.getCarriersUrl()
            Log.d("Sync", "Pobieranie Carriers z: $carriersUrl")

            val carriersCsv = CsvImporter.downloadCsvFromUrl(carriersUrl)
            val carriers = CsvImporter.parseCarriers(carriersCsv)
                .filter { it.isValid() }

            Log.d("Sync", "Znaleziono ${carriers.size} aktywnych przewoźników:")
            carriers.forEach { carrier ->
                Log.d("Sync", "   - ${carrier.carrierName} (GID: ${carrier.gid})")
            }

            if (carriers.isEmpty()) {
                return Result.failure(Exception("Brak aktywnych przewoźników"))
            }

            // 4. Pobierz dane dla każdego przewoźnika
            val allSchedules = mutableListOf<BusSchedule>()
            var successCount = 0
            var errorCount = 0

            carriers.forEach { carrier ->
                Log.d("Sync", "Pobieranie danych: ${carrier.carrierName}...")

                try {
                    val dataUrl = config.buildSheetUrl(carrier.gid, "tsv")
                    Log.d("Sync", "   URL: $dataUrl")

                    val dataCsv = CsvImporter.downloadCsvFromUrl(dataUrl)
                    Log.d("Sync", "   Pobrano ${dataCsv.length} znaków")

                    val schedules = CsvImporter.parseUniversalCsv(dataCsv, carrier.carrierName)

                    allSchedules.addAll(schedules)
                    successCount++

                    Log.d("Sync", "${carrier.carrierName}: ${schedules.size} rozkładów")

                } catch (e: Exception) {
                    errorCount++
                    Log.e("Sync", "Błąd pobierania ${carrier.carrierName}: ${e.message}", e)
                }
            }

            if (allSchedules.isEmpty()) {
                return Result.failure(Exception("Nie pobrano żadnych rozkładów"))
            }

            // 5. Zapisz do bazy danych
            Log.d("Sync", "Zapisywanie do bazy...")
            dao.deleteAllSchedules()
            dao.insertSchedules(allSchedules)

            // 6. Zapisz wersję i czas synchronizacji
            preferencesManager.saveLastSyncVersion(remoteVersion)
            preferencesManager.saveLastSyncTime()

            val summary = "Synchronizacja zakończona!\n" +
                    "Przewoźnicy: $successCount/${carriers.size}\n" +
                    "Rozkłady: ${allSchedules.size}\n" +
                    "Wersja: $remoteVersion"

            Log.d("Sync", summary)

            Result.success("Zsynchronizowano ${allSchedules.size} rozkładów z ${successCount} przewoźników (wersja $remoteVersion)")

        } catch (e: Exception) {
            Log.e("Sync", "Błąd synchronizacji: ${e.message}", e)
            Result.failure(e)
        }
    }
}