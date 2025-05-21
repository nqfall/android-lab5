package com.example.flightsearch.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "flight_search_preferences"
)

class UserPreferencesRepository(private val context: Context) {
    private companion object {
        val SEARCH_QUERY_KEY = stringPreferencesKey("search_query")
        const val TAG = "UserPreferencesRepo"
    }

    val searchQuery: Flow<String> = context.dataStore.data
        .catch {
            if (it is IOException) {
                // Log.e(TAG, "Error reading preferences.", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[SEARCH_QUERY_KEY] ?: ""
        }

    suspend fun saveSearchQuery(query: String) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_QUERY_KEY] = query
        }
    }
}