package com.myvillagebus

import android.app.Application
import com.myvillagebus.data.local.BusScheduleDatabase
import com.myvillagebus.data.repository.BusScheduleRepository
import com.myvillagebus.utils.PreferencesManager

class BusScheduleApplication : Application() {

    val preferencesManager by lazy { PreferencesManager(this) }

    private val database by lazy { BusScheduleDatabase.getDatabase(this) }

    val repository by lazy {
        BusScheduleRepository(
            dao = database.busScheduleDao(),
            preferencesManager = preferencesManager
        )
    }
}