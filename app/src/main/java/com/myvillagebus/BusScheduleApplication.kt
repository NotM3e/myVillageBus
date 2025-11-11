package com.myvillagebus

import android.app.Application
import com.myvillagebus.data.local.BusScheduleDatabase
import com.myvillagebus.data.repository.BusScheduleRepository
import com.myvillagebus.utils.PreferencesManager
import com.myvillagebus.utils.CarrierVersionManager
import com.myvillagebus.utils.VersionManager

class BusScheduleApplication : Application() {

    val preferencesManager by lazy { PreferencesManager(this) }
    val carrierVersionManager by lazy { CarrierVersionManager(this) }
    val versionManager by lazy { VersionManager(this) }

    private val database by lazy { BusScheduleDatabase.getDatabase(this) }

    val repository by lazy {
        BusScheduleRepository(
            dao = database.busScheduleDao(),
            carrierMetadataDao = database.carrierMetadataDao(),
            preferencesManager = preferencesManager,
            carrierVersionManager = carrierVersionManager
        )
    }
}