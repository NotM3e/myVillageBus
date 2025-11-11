package com.myvillagebus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

/**
 * Metadata przewoźnika (niezależne od rozkładów)
 * Wspiera backup poprzednich wersji oraz różne źródła danych
 */
@Entity(tableName = "carrier_metadata")
data class CarrierMetadata(
    @PrimaryKey
    val carrierId: String,  // "PKS-Grudziądz", "ŻANA", etc.

    val name: String,  // Nazwa wyświetlana

    val description: String? = null,  // Opcjonalny opis

    val currentVersion: Int,  // Aktualna wersja rozkładów

    val previousVersion: Int? = null,  // Poprzednia wersja (backup support)

    val downloadedAt: Long,  // Timestamp pobrania

    val updatedAt: Long? = null,  // Timestamp ostatniej aktualizacji

    val isActive: Boolean = true,  // Czy przewoźnik jest aktywny

    val scheduleCount: Int = 0,  // Liczba rozkładów

    val sourceType: String = SOURCE_GOOGLE_SHEETS,  // Źródło danych

    val sourceGid: String? = null  // GID arkusza (dla Google Sheets)
) {
    companion object {
        const val SOURCE_GOOGLE_SHEETS = "google_sheets"
        const val SOURCE_API = "api"
        const val SOURCE_SERVER = "server"
    }

    /**
     * Formatowany czas pobrania
     */
    fun getDownloadedAtFormatted(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(downloadedAt))
    }

    /**
     * Formatowany czas aktualizacji
     */
    fun getUpdatedAtFormatted(): String? {
        return updatedAt?.let {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        }
    }

    /**
     * Czy można przywrócić poprzednią wersję
     */
    fun canRollback(): Boolean {
        return previousVersion != null && previousVersion < currentVersion
    }

    /**
     * Czy potrzebuje aktualizacji
     */
    fun needsUpdate(remoteVersion: Int): Boolean {
        return remoteVersion > currentVersion
    }
}