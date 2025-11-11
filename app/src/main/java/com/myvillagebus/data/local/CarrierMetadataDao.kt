package com.myvillagebus.data.local

import androidx.room.*
import com.myvillagebus.data.model.CarrierMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface CarrierMetadataDao {

    /**
     * Pobierz wszystkich aktywnych przewoźników
     */
    @Query("SELECT * FROM carrier_metadata WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCarriers(): Flow<List<CarrierMetadata>>

    /**
     * Pobierz wszystkich przewoźników (włącznie z nieaktywnymi)
     */
    @Query("SELECT * FROM carrier_metadata ORDER BY name ASC")
    fun getAllCarriers(): Flow<List<CarrierMetadata>>

    /**
     * Pobierz przewoźnika po ID
     */
    @Query("SELECT * FROM carrier_metadata WHERE carrierId = :carrierId")
    suspend fun getCarrierById(carrierId: String): CarrierMetadata?

    /**
     * Wstaw lub zaktualizuj przewoźnika
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarrier(carrier: CarrierMetadata)

    /**
     * Wstaw wielu przewoźników
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarriers(carriers: List<CarrierMetadata>)

    /**
     * Aktualizuj wersję przewoźnika (zapisz starą jako backup)
     */
    @Query("""
        UPDATE carrier_metadata 
        SET previousVersion = currentVersion, 
            currentVersion = :newVersion,
            updatedAt = :timestamp,
            scheduleCount = :scheduleCount
        WHERE carrierId = :carrierId
    """)
    suspend fun updateCarrierVersion(
        carrierId: String,
        newVersion: Int,
        scheduleCount: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Przywróć poprzednią wersję przewoźnika
     */
    @Query("""
        UPDATE carrier_metadata 
        SET currentVersion = previousVersion,
            previousVersion = NULL,
            updatedAt = :timestamp
        WHERE carrierId = :carrierId 
        AND previousVersion IS NOT NULL
    """)
    suspend fun rollbackCarrierVersion(
        carrierId: String,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Usuń przewoźnika (soft delete - oznacz jako nieaktywny)
     */
    @Query("UPDATE carrier_metadata SET isActive = 0 WHERE carrierId = :carrierId")
    suspend fun deactivateCarrier(carrierId: String)

    /**
     * Usuń przewoźnika (hard delete)
     */
    @Query("DELETE FROM carrier_metadata WHERE carrierId = :carrierId")
    suspend fun deleteCarrier(carrierId: String)

    /**
     * Usuń wszystkich przewoźników
     */
    @Query("DELETE FROM carrier_metadata")
    suspend fun deleteAllCarriers()

    /**
     * Aktualizuj liczbę rozkładów dla przewoźnika
     */
    @Query("""
        UPDATE carrier_metadata 
        SET scheduleCount = :count 
        WHERE carrierId = :carrierId
    """)
    suspend fun updateScheduleCount(carrierId: String, count: Int)

    /**
     * Pobierz liczbę pobranych przewoźników
     */
    @Query("SELECT COUNT(*) FROM carrier_metadata WHERE isActive = 1")
    suspend fun getActiveCarriersCount(): Int
}