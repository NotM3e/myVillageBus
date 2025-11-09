package com.myvillagebus.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        AppConstants.PrefsFiles.BUS_SCHEDULE,
        Context.MODE_PRIVATE
    )

    /**
     * Zapisuje wersję ostatniej synchronizacji
     */
    fun saveLastSyncVersion(version: String) {
        prefs.edit().putString(AppConstants.SyncKeys.LAST_SYNC_VERSION, version).apply()
    }

    /**
     * Pobiera wersję ostatniej synchronizacji
     */
    fun getLastSyncVersion(): String? {
        return prefs.getString(AppConstants.SyncKeys.LAST_SYNC_VERSION, null)
    }

    /**
     * Zapisuje timestamp ostatniej synchronizacji
     */
    fun saveLastSyncTime(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(AppConstants.SyncKeys.LAST_SYNC_TIME, timestamp).apply()
    }

    /**
     * Pobiera timestamp ostatniej synchronizacji
     */
    fun getLastSyncTime(): Long {
        return prefs.getLong(AppConstants.SyncKeys.LAST_SYNC_TIME, 0)
    }

    /**
     * Zwraca sformatowany czas ostatniej synchronizacji
     * Format: "26.10.2025 15:30"
     */
    fun getLastSyncTimeFormatted(): String? {
        val timestamp = getLastSyncTime()
        if (timestamp == 0L) return null

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Sprawdza ile czasu minęło od ostatniej synchronizacji (w godzinach)
     */
    fun getHoursSinceLastSync(): Long {
        val lastSync = getLastSyncTime()
        if (lastSync == 0L) return Long.MAX_VALUE

        val now = System.currentTimeMillis()
        val diffMillis = now - lastSync
        return diffMillis / (1000 * 60 * 60)
    }

    /**
     * Czyści wszystkie dane synchronizacji
     */
    fun clearSyncData() {
        prefs.edit()
            .remove(AppConstants.SyncKeys.LAST_SYNC_VERSION)
            .remove(AppConstants.SyncKeys.LAST_SYNC_TIME)
            .apply()
    }
}