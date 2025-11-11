package com.myvillagebus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myvillagebus.data.model.BusSchedule
import com.myvillagebus.data.model.CarrierMetadata
import com.myvillagebus.utils.AppConstants

@Database(
    entities = [
        BusSchedule::class,
        CarrierMetadata::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BusScheduleDatabase : RoomDatabase() {

    abstract fun busScheduleDao(): BusScheduleDao
    abstract fun carrierMetadataDao(): CarrierMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: BusScheduleDatabase? = null

        fun getDatabase(context: Context): BusScheduleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusScheduleDatabase::class.java,
                    AppConstants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}