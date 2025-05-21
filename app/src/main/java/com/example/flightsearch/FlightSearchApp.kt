package com.example.flightsearch

import android.app.Application
import com.example.flightsearch.data.local.database.AppDatabase
import com.example.flightsearch.data.local.datastore.UserPreferencesRepository

class FlightSearchApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val userPreferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
}