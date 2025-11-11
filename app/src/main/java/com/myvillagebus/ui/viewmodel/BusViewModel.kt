package com.myvillagebus.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myvillagebus.BusScheduleApplication
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.repository.BusScheduleRepository
import com.myvillagebus.utils.CarrierVersionManager
import com.myvillagebus.utils.NetworkUtils
import com.myvillagebus.utils.PreferencesManager
import com.myvillagebus.utils.VersionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.myvillagebus.ui.model.CarrierUiModel
import com.myvillagebus.utils.AppConstants

class BusViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<BusScheduleApplication>()

    private val repository: BusScheduleRepository = app.repository
    private val preferencesManager: PreferencesManager = app.preferencesManager

    val versionManager: VersionManager = app.versionManager

    // Expose updateInfo z VersionManager
    val updateInfo = versionManager.updateInfo
    val isCheckingVersion = versionManager.isChecking

    private val carrierVersionManager: CarrierVersionManager = app.carrierVersionManager

    // Flows dla carrier browser
    private val _availableCarriers = MutableStateFlow<List<CarrierUiModel>>(emptyList())
    val availableCarriers: StateFlow<List<CarrierUiModel>> = _availableCarriers.asStateFlow()

    private val _downloadedCarriers = MutableStateFlow<List<CarrierUiModel>>(emptyList())
    val downloadedCarriers: StateFlow<List<CarrierUiModel>> = _downloadedCarriers.asStateFlow()

    private val _isLoadingCarriers = MutableStateFlow(false)
    val isLoadingCarriers: StateFlow<Boolean> = _isLoadingCarriers.asStateFlow()

    private val _carrierOperationStatus = MutableStateFlow<String?>(null)
    val carrierOperationStatus: StateFlow<String?> = _carrierOperationStatus.asStateFlow()

    private val _lastSyncVersion = MutableStateFlow<String?>(null)

    val lastSyncVersion: StateFlow<String?> = _lastSyncVersion.asStateFlow()
    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()
    private val _hoursSinceLastSync = MutableStateFlow<Long>(Long.MAX_VALUE)
    val hoursSinceLastSync: StateFlow<Long> = _hoursSinceLastSync.asStateFlow()

    init {
        refreshSyncInfo()
    }

    val allSchedules: StateFlow<List<BusSchedule>> = repository.allSchedules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCarriers: StateFlow<List<String>> = repository.allCarriers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDesignations: StateFlow<List<String>> = repository.allDesignations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun initializeSampleData(schedules: List<BusSchedule>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.initializeSampleData(schedules)
            _isLoading.value = false
        }
    }

    suspend fun getScheduleById(id: Int): BusSchedule? {
        return repository.getScheduleById(id)
    }

    fun insertSchedule(schedule: BusSchedule) {
        viewModelScope.launch {
            repository.insertSchedule(schedule)
        }
    }

    fun insertSchedules(schedules: List<BusSchedule>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.insertSchedules(schedules)
            _isLoading.value = false
        }
    }

    fun deleteAllSchedules() {
        viewModelScope.launch {
            repository.deleteAllSchedules()

            val app = getApplication<BusScheduleApplication>()
            app.carrierVersionManager.clearAllVersions()

            clearSyncInfo()
            _syncStatus.value = "Usunięto wszystkie rozkłady"
        }
    }

    fun deleteSchedule(schedule: BusSchedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
        }
    }

    fun updateSchedule(schedule: BusSchedule) {
        viewModelScope.launch {
            repository.updateSchedule(schedule)
        }
    }

    suspend fun getSchedulesCount(): Int {
        return repository.getSchedulesCount()
    }

    /**
     * Odświeża informacje o synchronizacji z PreferencesManager
     */
    fun refreshSyncInfo() {
        _lastSyncVersion.value = preferencesManager.getLastSyncVersion()
        _lastSyncTime.value = preferencesManager.getLastSyncTimeFormatted()
        _hoursSinceLastSync.value = preferencesManager.getHoursSinceLastSync()
    }

    /**
     * Czyści informacje o synchronizacji (po usunięciu danych)
     */
    private fun clearSyncInfo() {
        _lastSyncVersion.value = null
        _lastSyncTime.value = null
        _hoursSinceLastSync.value = Long.MAX_VALUE
    }

    /**
     * Synchronizuje rozkłady z Google Sheets
     *
     * @param configUrl URL do arkusza Config
     * @param forceSync Wymuś synchronizację nawet jeśli wersja się nie zmieniła
     */
    fun syncWithGoogleSheets(configUrl: String, forceSync: Boolean = false) {
        viewModelScope.launch {
            val currentUpdateInfo = updateInfo.value
            if (currentUpdateInfo != null && !currentUpdateInfo.canSync) {
                _syncStatus.value = "Synchronizacja wymaga aktualizacji aplikacji do wersji ${currentUpdateInfo.minVersion}"
                return@launch
            }

            _isSyncing.value = true
            _syncStatus.value = "Sprawdzanie połączenia..."

            val context = getApplication<BusScheduleApplication>()

            if (!NetworkUtils.isNetworkAvailable(context)) {
                _syncStatus.value = "Brak połączenia z Internetem"
                _isSyncing.value = false
                return@launch
            }

            _syncStatus.value = "Synchronizacja..."

            val result = repository.syncWithGoogleSheets(configUrl, forceSync)

            result.onSuccess { message ->
                _syncStatus.value = message
                refreshSyncInfo()
            }

            result.onFailure { error ->
                _syncStatus.value = "Błąd: ${error.message}"
            }

            _isSyncing.value = false
        }
    }

    /**
     * Sprawdza dostępność aktualizacji aplikacji
     *
     * @param configUrl URL do arkusza Config
     * @param manualCheck true jeśli użytkownik kliknął przycisk w ustawieniach
     */
    fun checkAppVersion(configUrl: String, manualCheck: Boolean = false) {
        viewModelScope.launch {
            val result = versionManager.checkForUpdates(configUrl, manualCheck)

            result.onSuccess { updateInfo ->
                if (updateInfo != null) {
                    Log.d("BusViewModel", "Dostępna aktualizacja: ${updateInfo.latestVersion}")
                } else {
                    Log.d("BusViewModel", "Aplikacja aktualna")
                }
            }

            result.onFailure { error ->
                Log.e("BusViewModel", "Błąd sprawdzania wersji: ${error.message}")
            }
        }
    }

    // Metody dla carrier browser

    /**
     * Pobiera listę dostępnych przewoźników z Google Sheets
     */
    fun loadAvailableCarriers(configUrl: String = AppConstants.CONFIG_URL) {
        viewModelScope.launch {
            _isLoadingCarriers.value = true
            _carrierOperationStatus.value = "Pobieranie listy przewoźników..."

            try {
                // 1. Pobierz Config
                val config = com.myvillagebus.utils.CsvImporter.getRemoteConfig(configUrl)
                    ?: throw Exception("Nie można pobrać Config")

                // 2. Pobierz Carriers
                val carriersUrl = config.getCarriersUrl()
                val carriersCsv = com.myvillagebus.utils.CsvImporter.downloadCsvFromUrl(carriersUrl)
                val remoteCarriers = com.myvillagebus.utils.CsvImporter.parseCarriers(carriersCsv)
                    .filter { it.isValid() }

                // 3. Pobierz lokalne metadata
                val localMetadata = repository.getAllCarrierMetadata()
                val localMap = localMetadata.associateBy { it.carrierId }

                // 4. Połącz zdalne i lokalne
                val merged = remoteCarriers.map { remote ->
                    val local = localMap[remote.carrierName]
                    if (local != null) {
                        // Przewoźnik pobrany - użyj metadata + zdalna wersja
                        com.myvillagebus.ui.model.CarrierUiModel.fromMetadata(local, remote.version)
                    } else {
                        // Przewoźnik dostępny - użyj zdalnego info
                        com.myvillagebus.ui.model.CarrierUiModel.fromRemoteInfo(remote)
                    }
                }

                _availableCarriers.value = merged
                _downloadedCarriers.value = merged.filter { it.isDownloaded }
                _carrierOperationStatus.value = null

                Log.d("BusViewModel", "Załadowano ${merged.size} przewoźników (${_downloadedCarriers.value.size} pobranych)")

            } catch (e: Exception) {
                Log.e("BusViewModel", "Błąd ładowania przewoźników", e)
                _carrierOperationStatus.value = "Błąd: ${e.message}"
            } finally {
                _isLoadingCarriers.value = false
            }
        }
    }

    /**
     * Pobiera rozkłady wybranego przewoźnika
     */
    fun downloadCarrier(carrierId: String, configUrl: String = AppConstants.CONFIG_URL) {
        viewModelScope.launch {
            _carrierOperationStatus.value = "Pobieranie $carrierId..."

            val result = repository.downloadCarrier(carrierId, configUrl)

            result.onSuccess { message ->
                _carrierOperationStatus.value = message
                loadAvailableCarriers(configUrl)  // Odśwież listę
            }

            result.onFailure { error ->
                _carrierOperationStatus.value = "Błąd: ${error.message}"
                Log.e("BusViewModel", "Błąd pobierania $carrierId", error)
            }
        }
    }

    /**
     * Aktualizuje rozkłady przewoźnika
     */
    fun updateCarrier(carrierId: String, configUrl: String = AppConstants.CONFIG_URL) {
        viewModelScope.launch {
            _carrierOperationStatus.value = "Aktualizowanie $carrierId..."

            val result = repository.updateCarrier(carrierId, configUrl)

            result.onSuccess { message ->
                _carrierOperationStatus.value = message
                loadAvailableCarriers(configUrl)
            }

            result.onFailure { error ->
                _carrierOperationStatus.value = "Błąd: ${error.message}"
                Log.e("BusViewModel", "Błąd aktualizacji $carrierId", error)
            }
        }
    }

    /**
     * Usuwa rozkłady przewoźnika
     */
    fun deleteCarrier(carrierId: String) {
        viewModelScope.launch {
            _carrierOperationStatus.value = "Usuwanie $carrierId..."

            val result = repository.deleteCarrier(carrierId)

            result.onSuccess { message ->
                _carrierOperationStatus.value = message
                loadAvailableCarriers()
            }

            result.onFailure { error ->
                _carrierOperationStatus.value = "Błąd: ${error.message}"
                Log.e("BusViewModel", "Błąd usuwania $carrierId", error)
            }
        }
    }

    /**
     * Przywraca poprzednią wersję rozkładów
     */
    fun rollbackCarrier(carrierId: String) {
        viewModelScope.launch {
            _carrierOperationStatus.value = "Przywracanie poprzedniej wersji $carrierId..."

            val result = repository.rollbackCarrier(carrierId)

            result.onSuccess { message ->
                _carrierOperationStatus.value = message
                loadAvailableCarriers()
            }

            result.onFailure { error ->
                _carrierOperationStatus.value = "Błąd: ${error.message}"
                Log.e("BusViewModel", "Błąd przywracania $carrierId", error)
            }
        }
    }

    /**
     * Aktualizuje wszystkich pobranych przewoźników
     */
    fun updateAllCarriers(configUrl: String = AppConstants.CONFIG_URL) {
        viewModelScope.launch {
            val downloaded = _downloadedCarriers.value
            if (downloaded.isEmpty()) {
                _carrierOperationStatus.value = "Brak pobranych przewoźników"
                return@launch
            }

            _carrierOperationStatus.value = "Aktualizowanie ${downloaded.size} przewoźników..."

            var successCount = 0
            var failCount = 0

            downloaded.forEach { carrier ->
                if (carrier.hasUpdate) {
                    val result = repository.updateCarrier(carrier.id, configUrl)
                    if (result.isSuccess) successCount++ else failCount++
                }
            }

            _carrierOperationStatus.value = when {
                failCount == 0 -> "Zaktualizowano $successCount przewoźników"
                successCount == 0 -> "Błąd aktualizacji wszystkich przewoźników"
                else -> "Zaktualizowano $successCount, błędów: $failCount"
            }

            loadAvailableCarriers(configUrl)
        }
    }

    /**
     * Usuwa wszystkich pobranych przewoźników
     */
    fun deleteAllCarriers() {
        viewModelScope.launch {
            _carrierOperationStatus.value = "Usuwanie wszystkich przewoźników..."

            val result = repository.deleteAllCarriers()

            result.onSuccess { message ->
                _carrierOperationStatus.value = message
                loadAvailableCarriers()
            }

            result.onFailure { error ->
                _carrierOperationStatus.value = "Błąd: ${error.message}"
            }
        }
    }

    /**
     * Czyści status operacji na przewoźnikach
     */
    fun clearCarrierStatus() {
        _carrierOperationStatus.value = null
    }
}