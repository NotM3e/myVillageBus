package com.myvillagebus.data.repository

import com.myvillagebus.data.local.BusScheduleDao
import com.myvillagebus.data.model.BusSchedule
import kotlinx.coroutines.flow.Flow

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
}