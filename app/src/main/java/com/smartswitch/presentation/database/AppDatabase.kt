package com.smartswitch.presentation.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ZMediaHistoryEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mediaHistoryDao(): MediaHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "SmartSwitchDatabase"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
