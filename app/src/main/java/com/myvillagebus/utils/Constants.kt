package com.myvillagebus.utils

/**
 * Centralne miejsce dla stałych konfiguracyjnych aplikacji
 */
object AppConstants {

    /**
     * URL do głównego arkusza Config w Google Sheets
     */
    const val CONFIG_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vSUpEKaD5spMbQ0e_VVj2XI1pxlTbGz6QV5AEvD0HQIM-xDk1yzhWA3yo7zwPjJ8yq9anAJrixPn4WI/pub?gid=0&single=true&output=tsv"

    /**
     * Nazwa bazy danych Room
     */
    const val DATABASE_NAME = "bus_schedule_database"

    /**
     * Nazwy plików SharedPreferences
     */
    object PrefsFiles {
        const val BUS_SCHEDULE = "bus_schedule_prefs"
        const val VERSION = "version_prefs"
        const val CARRIER_VERSIONS = "carrier_versions"
    }

    /**
     * Klucze dla PreferencesManager (synchronizacja rozkładów)
     */
    object SyncKeys {
        const val LAST_SYNC_VERSION = "last_sync_version"
        const val LAST_SYNC_TIME = "last_sync_time"
        const val LAST_USED_PROFILE = "last_used_profile"
    }

    /**
     * Klucze dla VersionManager (sprawdzanie wersji aplikacji)
     */
    object VersionKeys {
        const val AUTO_CHECK_ENABLED = "auto_check_enabled"
        const val LAST_VERSION_CHECK_TIME = "last_version_check_time"
    }
}