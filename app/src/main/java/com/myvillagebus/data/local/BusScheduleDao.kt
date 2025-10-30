package com.myvillagebus.data.local

import androidx.room.*
import com.myvillagebus.data.model.BusSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface BusScheduleDao {

    // Pobierz wszystkie rozkłady
    @Query("SELECT * FROM bus_schedules ORDER BY departureTime ASC")
    fun getAllSchedules(): Flow<List<BusSchedule>>

    // Pobierz rozkład po ID
    @Query("SELECT * FROM bus_schedules WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: Int): BusSchedule?

    // Pobierz rozkłady po przewoźniku
    @Query("SELECT * FROM bus_schedules WHERE carrierName = :carrierName ORDER BY departureTime ASC")
    fun getSchedulesByCarrier(carrierName: String): Flow<List<BusSchedule>>

    // Pobierz rozkłady po oznaczeniu linii
    @Query("SELECT * FROM bus_schedules WHERE lineDesignation = :designation ORDER BY departureTime ASC")
    fun getSchedulesByDesignation(designation: String): Flow<List<BusSchedule>>

    // Pobierz rozkłady które zatrzymują się na danym przystanku
    @Query("SELECT * FROM bus_schedules WHERE stops LIKE '%' || :stopName || '%' ORDER BY departureTime ASC")
    fun getSchedulesByStop(stopName: String): Flow<List<BusSchedule>>

    // Wstaw jeden rozkład
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: BusSchedule): Long

    // Wstaw wiele rozkładów
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<BusSchedule>)

    // Aktualizuj rozkład
    @Update
    suspend fun updateSchedule(schedule: BusSchedule)

    // Usuń rozkład
    @Delete
    suspend fun deleteSchedule(schedule: BusSchedule)

    // Usuń wszystkie rozkłady
    @Query("DELETE FROM bus_schedules")
    suspend fun deleteAllSchedules()

    // Pobierz wszystkich unikalnych przewoźników
    @Query("SELECT DISTINCT carrierName FROM bus_schedules ORDER BY carrierName ASC")
    fun getAllCarriers(): Flow<List<String>>

    // Pobierz wszystkie unikalne oznaczenia linii
    @Query("SELECT DISTINCT lineDesignation FROM bus_schedules WHERE lineDesignation IS NOT NULL ORDER BY lineDesignation ASC")
    fun getAllDesignations(): Flow<List<String>>

    // Pobierz liczbę rozkładów
    @Query("SELECT COUNT(*) FROM bus_schedules")
    suspend fun getSchedulesCount(): Int

    // Usuń rozkłady konkretnego przewoźnika
    @Query("DELETE FROM bus_schedules WHERE carrierName = :carrierName")
    suspend fun deleteSchedulesByCarrier(carrierName: String)
}