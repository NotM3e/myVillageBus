package com.myvillagebus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myvillagebus.data.model.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    /**
     * Pobierz wszystkie profile (sortowane: ostatnio użyte na górze, potem po dacie utworzenia)
     */
    @Query("""
        SELECT * FROM profiles 
        ORDER BY 
            CASE WHEN lastUsedAt IS NOT NULL THEN lastUsedAt ELSE 0 END DESC,
            createdAt DESC
    """)
    fun getAllProfiles(): Flow<List<Profile>>

    /**
     * Pobierz profil po ID
     */
    @Query("SELECT * FROM profiles WHERE id = :profileId")
    suspend fun getProfileById(profileId: Int): Profile?

    /**
     * Pobierz profil po nazwie (case-insensitive)
     */
    @Query("SELECT * FROM profiles WHERE LOWER(name) = LOWER(:name)")
    suspend fun getProfileByName(name: String): Profile?

    /**
     * Pobierz wszystkie nazwy profili (dla walidacji)
     */
    @Query("SELECT name FROM profiles")
    suspend fun getAllProfileNames(): List<String>

    /**
     * Pobierz liczbę profili
     */
    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfilesCount(): Int

    /**
     * Wstaw profil
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    /**
     * Aktualizuj profil
     */
    @Update
    suspend fun updateProfile(profile: Profile)

    /**
     * Usuń profil
     */
    @Delete
    suspend fun deleteProfile(profile: Profile)

    /**
     * Usuń profil po ID
     */
    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Int)

    /**
     * Zaktualizuj timestamp ostatniego użycia
     */
    @Query("UPDATE profiles SET lastUsedAt = :timestamp WHERE id = :profileId")
    suspend fun updateLastUsedTime(profileId: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Usuń wszystkie profile (do celów testowych/resetowania)
     */
    @Query("DELETE FROM profiles")
    suspend fun deleteAllProfiles()
}