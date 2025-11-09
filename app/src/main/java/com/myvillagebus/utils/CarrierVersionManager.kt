package com.myvillagebus.utils

import android.content.Context
import android.content.SharedPreferences

class CarrierVersionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        AppConstants.PrefsFiles.CARRIER_VERSIONS,
        Context.MODE_PRIVATE
    )


    /**
     * Zapisz wersję dla konkretnego przewoźnika (jako liczba)
     */
    fun saveCarrierVersion(carrierName: String, version: Int) {
        prefs.edit().putInt("version_$carrierName", version).apply()
    }

    /**
     * Pobierz wersję dla konkretnego przewoźnika
     */
    fun getCarrierVersion(carrierName: String): Int {
        return prefs.getInt("version_$carrierName", -1)
    }

    /**
     * Sprawdź czy przewoźnik wymaga aktualizacji
     * @return true jeśli zdalna wersja jest nowsza
     */
    fun needsUpdate(carrierName: String, remoteVersion: Int?): Boolean {
        if (remoteVersion == null) return false

        val localVersion = getCarrierVersion(carrierName)
        if (localVersion == -1) return true

        return remoteVersion > localVersion
    }

    /**
     * Usuń wersję dla przewoźnika
     */
    fun removeCarrierVersion(carrierName: String) {
        prefs.edit().remove("version_$carrierName").apply()
    }

    /**
     * Usuń wszystkie wersje
     */
    fun clearAllVersions() {
        prefs.edit().clear().apply()
    }

    /**
     * Pobierz wszystkie zapisane wersje
     */
    fun getAllVersions(): Map<String, Int> {
        return prefs.all.mapNotNull { (key, value) ->
            if (key.startsWith("version_") && value is Int) {
                val carrierName = key.removePrefix("version_")
                carrierName to value
            } else {
                null
            }
        }.toMap()
    }
}