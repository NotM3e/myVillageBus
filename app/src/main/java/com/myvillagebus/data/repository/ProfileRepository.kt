package com.myvillagebus.data.repository

import android.util.Log
import com.myvillagebus.data.local.ProfileDao
import com.myvillagebus.data.model.Profile
import kotlinx.coroutines.flow.Flow

class ProfileRepository(
    private val dao: ProfileDao
) {

    val allProfiles: Flow<List<Profile>> = dao.getAllProfiles()

    /**
     * Pobierz profil po ID
     */
    suspend fun getProfileById(id: Int): Profile? {
        return dao.getProfileById(id)
    }

    /**
     * Sprawdź czy można utworzyć nowy profil (max 10)
     */
    suspend fun canCreateProfile(): Boolean {
        return dao.getProfilesCount() < Profile.MAX_PROFILES
    }

    /**
     * Waliduj nazwę profilu
     */
    suspend fun validateProfileName(name: String, currentId: Int? = null): String? {
        val trimmed = name.trim()

        return when {
            trimmed.isBlank() -> "Nazwa nie może być pusta"
            trimmed.length > Profile.MAX_NAME_LENGTH -> "Maksymalnie ${Profile.MAX_NAME_LENGTH} znaków"
            else -> {
                // Sprawdź duplikaty (exclude current profile)
                val existingProfile = dao.getProfileByName(trimmed)

                if (existingProfile != null && existingProfile.id != currentId) {
                    "Profil o tej nazwie już istnieje"
                } else {
                    null  // OK
                }
            }
        }
    }

    /**
     * Utwórz nowy profil
     */
    suspend fun createProfile(profile: Profile): Result<Long> {
        return try {
            // Sprawdź limit
            if (!canCreateProfile()) {
                return Result.failure(Exception("Maksymalna liczba profili to ${Profile.MAX_PROFILES}"))
            }

            // Walidacja
            validateProfileName(profile.name)?.let { error ->
                return Result.failure(Exception(error))
            }

            val id = dao.insertProfile(profile)
            Log.d("ProfileRepository", "✅ Utworzono profil '${profile.name}' (id=$id)")

            Result.success(id)

        } catch (e: Exception) {
            Log.e("ProfileRepository", "❌ Błąd tworzenia profilu", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuj profil
     */
    suspend fun updateProfile(profile: Profile): Result<Unit> {
        return try {
            // Walidacja (exclude current profile name)
            validateProfileName(profile.name, profile.id)?.let { error ->
                return Result.failure(Exception(error))
            }

            dao.updateProfile(profile)
            Log.d("ProfileRepository", "✅ Zaktualizowano profil '${profile.name}'")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("ProfileRepository", "❌ Błąd aktualizacji profilu", e)
            Result.failure(e)
        }
    }

    /**
     * Usuń profil
     */
    suspend fun deleteProfile(profile: Profile): Result<Unit> {
        return try {
            dao.deleteProfile(profile)
            Log.d("ProfileRepository", "✅ Usunięto profil '${profile.name}'")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("ProfileRepository", "❌ Błąd usuwania profilu", e)
            Result.failure(e)
        }
    }

    /**
     * Oznacz profil jako ostatnio użyty (sortowanie)
     */
    suspend fun markAsUsed(profileId: Int) {
        try {
            dao.updateLastUsedTime(profileId)
            Log.d("ProfileRepository", "✅ Zaktualizowano lastUsedAt dla profilu $profileId")
        } catch (e: Exception) {
            Log.e("ProfileRepository", "❌ Błąd update lastUsedAt", e)
        }
    }

    /**
     * Usuń wszystkie profile
     */
    suspend fun deleteAllProfiles(): Result<Unit> {
        return try {
            dao.deleteAllProfiles()
            Log.d("ProfileRepository", "✅ Usunięto wszystkie profile")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("ProfileRepository", "❌ Błąd usuwania wszystkich profili", e)
            Result.failure(e)
        }
    }
}