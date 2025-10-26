package com.myvillagebus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.repository.BusScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BusViewModel(private val repository: BusScheduleRepository) : ViewModel() {

    // Wszystkie rozkłady z bazy danych
    val allSchedules: StateFlow<List<BusSchedule>> = repository.allSchedules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Wszyscy przewoźnicy
    val allCarriers: StateFlow<List<String>> = repository.allCarriers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Wszystkie oznaczenia
    val allDesignations: StateFlow<List<String>> = repository.allDesignations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Stan ładowania
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Inicjalizacja z przykładowymi danymi
    fun initializeSampleData(schedules: List<BusSchedule>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.initializeSampleData(schedules)
            _isLoading.value = false
        }
    }

    // Pobierz rozkład po ID
    suspend fun getScheduleById(id: Int): BusSchedule? {
        return repository.getScheduleById(id)
    }

    // Wstaw rozkład
    fun insertSchedule(schedule: BusSchedule) {
        viewModelScope.launch {
            repository.insertSchedule(schedule)
        }
    }

    // Wstaw wiele rozkładów (import)
    fun insertSchedules(schedules: List<BusSchedule>) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.insertSchedules(schedules)
            _isLoading.value = false
        }
    }

    // Usuń wszystkie rozkłady
    fun deleteAllSchedules() {
        viewModelScope.launch {
            repository.deleteAllSchedules()
        }
    }

    // Usuń rozkład
    fun deleteSchedule(schedule: BusSchedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
        }
    }

    // Aktualizuj rozkład
    fun updateSchedule(schedule: BusSchedule) {
        viewModelScope.launch {
            repository.updateSchedule(schedule)
        }
    }

    // Pobierz liczbę rozkładów
    suspend fun getSchedulesCount(): Int {
        return repository.getSchedulesCount()
    }

    // Stan synchronizacji
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Synchronizuje rozkłady z Google Sheets
     */
    fun syncWithGoogleSheets(configUrl: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Synchronizacja..."

            val result = repository.syncWithGoogleSheets(configUrl, forceSync = true)

            result.onSuccess { message ->
                _syncStatus.value = "✅ $message"
            }

            result.onFailure { error ->
                _syncStatus.value = "❌ Błąd: ${error.message}"
            }

            _isSyncing.value = false
        }
    }
}

// Factory do tworzenia ViewModelu z Repository
class BusViewModelFactory(private val repository: BusScheduleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BusViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}