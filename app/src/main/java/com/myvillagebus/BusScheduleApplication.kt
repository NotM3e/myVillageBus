package com.myvillagebus

import android.app.Application
import com.myvillagebus.data.local.BusScheduleDatabase
import com.myvillagebus.data.repository.BusScheduleRepository

class BusScheduleApplication : Application() {

    // Lazy initialization - tworzenie tylko gdy potrzebne
    val database by lazy { BusScheduleDatabase.getDatabase(this) }
    val repository by lazy { BusScheduleRepository(database.busScheduleDao()) }
}