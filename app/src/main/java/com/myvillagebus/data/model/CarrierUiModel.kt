package com.myvillagebus.ui.model

import com.myvillagebus.data.model.CarrierMetadata
import com.myvillagebus.data.remote.CarrierInfo

/**
 * UI model dla przewoźnika - łączy dane lokalne (metadata) ze zdalnymi (info)
 */
data class CarrierUiModel(
    val id: String,
    val name: String,
    val description: String?,
    val currentVersion: Int?,
    val remoteVersion: Int?,
    val previousVersion: Int?,
    val downloadedAt: Long?,
    val updatedAt: Long?,
    val scheduleCount: Int,
    val isDownloaded: Boolean,
    val sourceGid: String?
) {
    val hasUpdate: Boolean
        get() = isDownloaded && remoteVersion != null && currentVersion != null && remoteVersion > currentVersion

    val canRollback: Boolean
        get() = isDownloaded && previousVersion != null && currentVersion != null && previousVersion < currentVersion

    val downloadedAtFormatted: String?
        get() = downloadedAt?.let {
            val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(it))
        }

    val updatedAtFormatted: String?
        get() = updatedAt?.let {
            val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(it))
        }

    companion object {
        /**
         * Tworzy UI model z lokalnej metadata (już pobrane)
         */
        fun fromMetadata(metadata: CarrierMetadata, remoteVersion: Int?): CarrierUiModel {
            return CarrierUiModel(
                id = metadata.carrierId,
                name = metadata.name,
                description = metadata.description,
                currentVersion = metadata.currentVersion,
                remoteVersion = remoteVersion,
                previousVersion = metadata.previousVersion,
                downloadedAt = metadata.downloadedAt,
                updatedAt = metadata.updatedAt,
                scheduleCount = metadata.scheduleCount,
                isDownloaded = true,
                sourceGid = metadata.sourceGid
            )
        }

        /**
         * Tworzy UI model ze zdalnego info (dostępne do pobrania)
         */
        fun fromRemoteInfo(info: CarrierInfo): CarrierUiModel {
            return CarrierUiModel(
                id = info.carrierName,
                name = info.carrierName,
                description = info.description,
                currentVersion = null,
                remoteVersion = info.version,
                previousVersion = null,
                downloadedAt = null,
                updatedAt = null,
                scheduleCount = 0,
                isDownloaded = false,
                sourceGid = info.gid
            )
        }
    }
}