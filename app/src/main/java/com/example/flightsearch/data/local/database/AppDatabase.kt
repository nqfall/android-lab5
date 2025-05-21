package com.example.flightsearch.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.flightsearch.data.local.dao.AirportDao
import com.example.flightsearch.data.local.entities.Airport
import com.example.flightsearch.data.local.entities.Favorite

@Database(entities = [Airport::class, Favorite::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun airportDao(): AirportDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "flight_search_db")
                    .createFromAsset("database/flights.db") // Убедитесь, что файл в assets/database/
                    .fallbackToDestructiveMigration() // Для простоты, в проде лучше миграции
                    .build()
                    .also { Instance = it }
            }
        }
    }
}