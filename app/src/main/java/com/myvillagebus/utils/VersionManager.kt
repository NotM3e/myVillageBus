package com.myvillagebus.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reprezentacja wersji aplikacji (semantic versioning)
 */
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int = 0
) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String {
        return if (patch == 0) "$major.$minor" else "$major.$minor.$patch"
    }

    companion object {
        fun parse(versionString: String): Version? {
            return try {
                val parts = versionString.trim().split(".").map { it.toIntOrNull() ?: 0 }
                Version(
                    major = parts.getOrNull(0) ?: 0,
                    minor = parts.getOrNull(1) ?: 0,
                    patch = parts.getOrNull(2) ?: 0
                )
            } catch (e: Exception) {
                Log.e("Version", "Błąd parsowania wersji: $versionString", e)
                null
            }
        }
    }
}

/**
 * Informacje o dostępnej aktualizacji
 */
data class UpdateInfo(
    val latestVersion: Version,
    val minVersion: Version,
    val currentVersion: Version,
    val downloadUrl: String,
    val updateMessage: String? = null,
    val forceUpdateMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isUpdateAvailable: Boolean
        get() = currentVersion < latestVersion

    val isUpdateRequired: Boolean
        get() = currentVersion < minVersion

    val canSync: Boolean
        get() = currentVersion >= minVersion
}

/**
 * Manager do sprawdzania wersji aplikacji
 */
class VersionManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(
        AppConstants.PrefsFiles.VERSION,
        Context.MODE_PRIVATE
    )

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    /**
     * Pobiera aktualną wersję aplikacji z build.gradle
     */
    fun getCurrentVersion(): Version {
        return try {
            val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            Version.parse(versionName) ?: Version(0, 0, 0)
        } catch (e: Exception) {
            Log.e("VersionManager", "Nie można pobrać wersji aplikacji", e)
            Version(0, 0, 0)
        }
    }

    /**
     * Sprawdza czy auto-check aktualizacji jest włączony
     */
    fun isAutoCheckEnabled(): Boolean {
        return prefs.getBoolean(AppConstants.VersionKeys.AUTO_CHECK_ENABLED, true)
    }

    /**
     * Ustawia czy auto-check ma być włączony
     */
    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(AppConstants.VersionKeys.AUTO_CHECK_ENABLED, enabled).apply()
        Log.d("VersionManager", "Auto-check aktualizacji: ${if (enabled) "włączony" else "wyłączony"}")
    }

    /**
     * Pobiera timestamp ostatniego sprawdzenia wersji
     */
    private fun getLastVersionCheckTime(): Long {
        return prefs.getLong(AppConstants.VersionKeys.LAST_VERSION_CHECK_TIME, 0)
    }

    /**
     * Sprawdza ile godzin minęło od ostatniego sprawdzenia wersji
     */
    private fun getHoursSinceLastVersionCheck(): Long {
        val lastCheck = getLastVersionCheckTime()
        if (lastCheck == 0L) return Long.MAX_VALUE

        val now = System.currentTimeMillis()
        val diffMillis = now - lastCheck
        return diffMillis / (1000 * 60 * 60)
    }

    /**
     * Sprawdza czy należy wykonać auto-check (24h throttle)
     */
    fun shouldCheckForUpdates(): Boolean {
        val isEnabled = isAutoCheckEnabled()
        val hoursSinceCheck = getHoursSinceLastVersionCheck()

        Log.d("VersionManager", "Auto-check enabled: $isEnabled, hours since last check: $hoursSinceCheck")

        return isEnabled && hoursSinceCheck >= 24
    }

    /**
     * Sprawdza dostępność aktualizacji
     *
     * @param configUrl URL do arkusza Config
     * @param manualCheck true jeśli użytkownik kliknął "Sprawdź aktualizacje" w ustawieniach
     * @return Result<UpdateInfo?> - null jeśli brak aktualizacji
     */
    suspend fun checkForUpdates(
        configUrl: String,
        manualCheck: Boolean = false
    ): Result<UpdateInfo?> {
        _isChecking.value = true

        return try {
            Log.d("VersionManager", "Sprawdzanie aktualizacji (manual=$manualCheck)...")

            // 1. Pobierz Config → app_versions_gid
            val config = CsvImporter.getRemoteConfig(configUrl)
                ?: return Result.failure(Exception("Nie można pobrać Config"))

            val appVersionsGid = config.appVersionsGid
                ?: return Result.failure(Exception("Brak app_versions_gid w Config"))

            // 2. Pobierz AppVersions sheet
            val appVersionsUrl = config.buildSheetUrl(appVersionsGid, "tsv")
            val appVersionsCsv = CsvImporter.downloadCsvFromUrl(appVersionsUrl)

            // 3. Parsuj dane
            val versionData = parseAppVersions(appVersionsCsv)
                ?: return Result.failure(Exception("Błąd parsowania AppVersions"))

            val currentVersion = getCurrentVersion()
            val latestVersion = Version.parse(versionData["latest_version"] ?: "0.0")
                ?: return Result.failure(Exception("Nieprawidłowa latest_version"))
            val minVersion = Version.parse(versionData["min_version"] ?: "0.0")
                ?: return Result.failure(Exception("Nieprawidłowa min_version"))

            Log.d("VersionManager", "Aktualna: $currentVersion, Najnowsza: $latestVersion, Minimalna: $minVersion")

            // 4. Sprawdź czy aktualizacja potrzebna
            val updateInfo = UpdateInfo(
                latestVersion = latestVersion,
                minVersion = minVersion,
                currentVersion = currentVersion,
                downloadUrl = versionData["download_url"] ?: "",
                updateMessage = versionData["update_message"],
                forceUpdateMessage = versionData["force_update_msg"]
            )

            // 5. ZAWSZE ustaw updateInfo + zapisz timestamp (tylko dla auto-check)
            if (updateInfo.isUpdateAvailable) {
                _updateInfo.value = updateInfo.copy()
                Log.d("VersionManager", "Znaleziono aktualizację: $updateInfo")

                if (!manualCheck) {
                    prefs.edit().putLong(AppConstants.VersionKeys.LAST_VERSION_CHECK_TIME, System.currentTimeMillis()).apply()
                    Log.d("VersionManager", "Zapisano timestamp auto-check")
                }

                Result.success(updateInfo)
            } else {
                Log.d("VersionManager", "Aplikacja jest aktualna")
                _updateInfo.value = null

                if (!manualCheck) {
                    prefs.edit().putLong(AppConstants.VersionKeys.LAST_VERSION_CHECK_TIME, System.currentTimeMillis()).apply()
                    Log.d("VersionManager", "Zapisano timestamp auto-check")
                }

                Result.success(null)
            }

        } catch (e: Exception) {
            Log.e("VersionManager", "Błąd sprawdzania aktualizacji", e)
            Result.failure(e)
        } finally {
            _isChecking.value = false
        }
    }

    /**
     * Parsuje arkusz AppVersions (TSV)
     */
    private fun parseAppVersions(csvContent: String): Map<String, String>? {
        return try {
            val map = mutableMapOf<String, String>()
            val lines = csvContent.lines()

            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach

                val parts = line.split("\t").map { it.trim() }
                if (parts.size >= 2) {
                    val key = parts[0]
                    val value = parts[1]
                    map[key] = value
                }
            }

            Log.d("VersionManager", "Parsowano AppVersions: $map")
            map
        } catch (e: Exception) {
            Log.e("VersionManager", "Błąd parsowania AppVersions", e)
            null
        }
    }

    /**
     * Resetuje stan (np. po aktualizacji aplikacji)
     */
    fun reset() {
        prefs.edit().clear().apply()
        _updateInfo.value = null
    }
}