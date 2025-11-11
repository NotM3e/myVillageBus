package com.myvillagebus.data.repository

import android.util.Log
import com.myvillagebus.data.local.BusScheduleDao
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.utils.CsvImporter
import com.myvillagebus.utils.PreferencesManager
import com.myvillagebus.utils.CarrierVersionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.myvillagebus.data.model.CarrierMetadata
import com.myvillagebus.data.local.CarrierMetadataDao

class BusScheduleRepository(
    private val dao: BusScheduleDao,
    private val carrierMetadataDao: CarrierMetadataDao,
    private val preferencesManager: PreferencesManager,
    private val carrierVersionManager: CarrierVersionManager
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

    // Metody dla carrier browser
    /**
     * Pobiera wszystkie lokalne metadata przewoźników
     */
    suspend fun getAllCarrierMetadata(): List<CarrierMetadata> {
        return withContext(Dispatchers.IO) {
            carrierMetadataDao.getAllCarriers().first()
        }
    }

    /**
     * Pobiera rozkłady wybranego przewoźnika
     */
    suspend fun downloadCarrier(
        carrierId: String,
        configUrl: String
    ): Result<String> {
        return try {
            Log.d("Repository", "Pobieranie przewoźnika: $carrierId")

            // 1. Pobierz Config
            val config = CsvImporter.getRemoteConfig(configUrl)
                ?: return Result.failure(Exception("Nie można pobrać Config"))

            // 2. Pobierz Carriers sheet
            val carriersUrl = config.getCarriersUrl()
            val carriersCsv = CsvImporter.downloadCsvFromUrl(carriersUrl)
            val carriers = CsvImporter.parseCarriers(carriersCsv)

            val carrier = carriers.find { it.carrierName == carrierId }
                ?: return Result.failure(Exception("Przewoźnik '$carrierId' nie znaleziony"))

            if (!carrier.isValid()) {
                return Result.failure(Exception("Przewoźnik nieaktywny lub nieprawidłowy"))
            }

            // 3. Pobierz dane przewoźnika
            val dataUrl = config.buildSheetUrl(carrier.gid, "tsv")
            val dataCsv = CsvImporter.downloadCsvFromUrl(dataUrl)
            val schedules = CsvImporter.parseUniversalCsv(dataCsv, carrier.carrierName)

            if (schedules.isEmpty()) {
                return Result.failure(Exception("Brak rozkładów dla przewoźnika '$carrierId'"))
            }

            // 4. Zapisz rozkłady
            dao.insertSchedules(schedules)

            // 5. Zapisz metadata
            carrierMetadataDao.insertCarrier(
                CarrierMetadata(
                    carrierId = carrier.carrierName,
                    name = carrier.carrierName,
                    description = carrier.description,
                    currentVersion = carrier.version ?: 1,
                    downloadedAt = System.currentTimeMillis(),
                    isActive = true,
                    scheduleCount = schedules.size,
                    sourceGid = carrier.gid
                )
            )

            Log.d("Repository", "✅ Pobrano $carrierId: ${schedules.size} rozkładów (v${carrier.version})")

            Result.success("Pobrano ${schedules.size} rozkładów dla $carrierId")

        } catch (e: Exception) {
            Log.e("Repository", "❌ Błąd pobierania $carrierId", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje rozkłady przewoźnika
     */
    suspend fun updateCarrier(
        carrierId: String,
        configUrl: String
    ): Result<String> {
        return try {
            Log.d("Repository", "Aktualizowanie przewoźnika: $carrierId")

            // Usuń stare rozkłady i pobierz nowe (reuse downloadCarrier logic)
            dao.deleteSchedulesByCarrierId(carrierId)

            downloadCarrier(carrierId, configUrl)

        } catch (e: Exception) {
            Log.e("Repository", "❌ Błąd aktualizacji $carrierId", e)
            Result.failure(e)
        }
    }

    /**
     * Usuwa rozkłady przewoźnika
     */
    suspend fun deleteCarrier(carrierId: String): Result<String> {
        return try {
            dao.deleteSchedulesByCarrierId(carrierId)
            carrierMetadataDao.deleteCarrier(carrierId)

            Log.d("Repository", "✅ Usunięto $carrierId")

            Result.success("Usunięto przewoźnika '$carrierId'")

        } catch (e: Exception) {
            Log.e("Repository", "❌ Błąd usuwania $carrierId", e)
            Result.failure(e)
        }
    }

    /**
     * Przywraca poprzednią wersję przewoźnika (symboliczne - na przyszłość backup)
     */
    suspend fun rollbackCarrier(carrierId: String): Result<String> {
        return try {
            carrierMetadataDao.rollbackCarrierVersion(carrierId)

            Log.d("Repository", "✅ Przywrócono poprzednią wersję $carrierId")

            Result.success("Przywrócono poprzednią wersję (funkcja w przyszłości będzie pobierać backup)")

        } catch (e: Exception) {
            Log.e("Repository", "❌ Błąd rollback $carrierId", e)
            Result.failure(e)
        }
    }

    /**
     * Usuwa wszystkich przewoźników
     */
    suspend fun deleteAllCarriers(): Result<String> {
        return try {
            dao.deleteAllSchedules()
            carrierMetadataDao.deleteAllCarriers()
            preferencesManager.clearSyncData()

            Log.d("Repository", "✅ Usunięto wszystkich przewoźników")

            Result.success("Usunięto wszystkich przewoźników")

        } catch (e: Exception) {
            Log.e("Repository", "❌ Błąd usuwania wszystkich", e)
            Result.failure(e)
        }
    }
}