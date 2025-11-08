package com.myvillagebus.data.remote

/**
 * Konfiguracja pobrana z arkusza Config
 */
data class RemoteConfig(
    val version: String,
    val lastUpdate: String? = null,
    val minAppVersion: String? = null,
    val carriersGid: String,
    val baseUrl: String,
    val appVersionsGid: String? = null
) {
    /**
     * Buduje pełny URL do arkusza TSV (zalecane - brak problemów z przecinkami)
     */
    fun buildSheetUrl(gid: String, format: String = "tsv"): String {
        return "$baseUrl?gid=$gid&single=true&output=$format"
    }

    /**
     * URL do arkusza Carriers (TSV)
     */
    fun getCarriersUrl(): String {
        return buildSheetUrl(carriersGid, "tsv")
    }

    companion object {
        /**
         * URL do arkusza Config (TSV)
         */
        fun getConfigUrl(baseUrl: String, configGid: String): String {
            return "$baseUrl?gid=$configGid&single=true&output=tsv"
        }
    }
}

/**
 * Informacja o przewoźniku z arkusza Carriers
 */
data class CarrierInfo(
    val carrierName: String,
    val gid: String,
    val active: Boolean = true,
    val description: String? = null,
    val version: Int? = null
) {
    /**
     * Czy przewoźnik jest aktywny i ma poprawne dane
     */
    fun isValid(): Boolean {
        return active && gid.isNotBlank() && carrierName.isNotBlank()
    }
}