package com.myvillagebus.data.source

import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.remote.CarrierInfo

/**
 * Abstrakcja źródła danych rozkładów
 * Pozwala na łatwą zmianę z Google Sheets → API → Server
 */
interface ScheduleDataSource {

    /**
     * Pobierz listę dostępnych przewoźników
     */
    suspend fun getAvailableCarriers(): Result<List<CarrierInfo>>

    /**
     * Pobierz rozkłady dla danego przewoźnika
     */
    suspend fun downloadCarrierSchedules(carrierId: String): Result<List<BusSchedule>>

    /**
     * Sprawdź wersję przewoźnika
     */
    suspend fun getCarrierVersion(carrierId: String): Result<Int>

    /**
     * Typ źródła danych
     */
    val sourceType: String
}

/**
 * Implementacja Google Sheets (istniejąca logika)
 */
class GoogleSheetsDataSource(
    private val configUrl: String
) : ScheduleDataSource {

    override val sourceType: String = "google_sheets"

    override suspend fun getAvailableCarriers(): Result<List<CarrierInfo>> {
        // TODO: Przenieś logikę z BusScheduleRepository
        return Result.success(emptyList())
    }

    override suspend fun downloadCarrierSchedules(carrierId: String): Result<List<BusSchedule>> {
        // TODO: Przenieś logikę parsowania CSV
        return Result.success(emptyList())
    }

    override suspend fun getCarrierVersion(carrierId: String): Result<Int> {
        // TODO: Sprawdź wersję z Carriers sheet
        return Result.success(1)
    }
}

/**
 * Przyszła implementacja REST API
 */
class ApiDataSource(
    private val baseUrl: String
) : ScheduleDataSource {

    override val sourceType: String = "api"

    override suspend fun getAvailableCarriers(): Result<List<CarrierInfo>> {
        // TODO: Retrofit call do /api/carriers
        return Result.success(emptyList())
    }

    override suspend fun downloadCarrierSchedules(carrierId: String): Result<List<BusSchedule>> {
        // TODO: Retrofit call do /api/carriers/{id}/schedules
        return Result.success(emptyList())
    }

    override suspend fun getCarrierVersion(carrierId: String): Result<Int> {
        // TODO: Retrofit call do /api/carriers/{id}/version
        return Result.success(1)
    }
}