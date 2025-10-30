package com.myvillagebus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myvillagebus.BusScheduleApplication
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.repository.BusScheduleRepository
import com.myvillagebus.utils.CarrierVersionManager
import com.myvillagebus.utils.NetworkUtils
import com.myvillagebus.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BusViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<BusScheduleApplication>()
    private val repository: BusScheduleRepository = app.repository

    private val preferencesManager: PreferencesManager = app.preferencesManager
    private val carrierVersionManager: CarrierVersionManager = app.carrierVersionManager
    private val _lastSyncVersion = MutableStateFlow<String?>(null)

    val lastSyncVersion: StateFlow<String?> = _lastSyncVersion.asStateFlow()
    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()
    private val _hoursSinceLastSync = MutableStateFlow<Long>(Long.MAX_VALUE)
    val hoursSinceLastSync: StateFlow<Long> = _hoursSinceLastSync.asStateFlow(

    )
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
}