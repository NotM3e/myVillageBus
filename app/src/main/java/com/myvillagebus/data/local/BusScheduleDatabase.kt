package com.myvillagebus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myvillagebus.data.model.BusSchedule

@Database(
    entities = [BusSchedule::class],
    version = 2,  // ← ZWIĘKSZ WERSJĘ z 1 na 2
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BusScheduleDatabase : RoomDatabase() {

    abstract fun busScheduleDao(): BusScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: BusScheduleDatabase? = null

        fun getDatabase(context: Context): BusScheduleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusScheduleDatabase::class.java,
                    "bus_schedule_database"
                )
                    .fallbackToDestructiveMigration()  // Usuwa i tworzy od nowa
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}