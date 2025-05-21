package com.example.flightsearch.data.local.dao

import androidx.room.*
import com.example.flightsearch.data.local.entities.Airport
import com.example.flightsearch.data.local.entities.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface AirportDao {
    @Query("SELECT * FROM airport WHERE iata_code LIKE :query OR name LIKE :query ORDER BY passengers DESC")
    suspend fun searchAirports(query: String): List<Airport>

    // Для получения конкретного аэропорта по коду
    @Query("SELECT * FROM airport WHERE iata_code = :iataCode")
    suspend fun getAirportByIataCode(iataCode: String): Airport?

    @Query("SELECT * FROM airport ORDER BY passengers DESC")
    suspend fun getAllAirports(): List<Airport> // Изменил для ясности

    @Query("SELECT * FROM favorite")
    fun getFavoritesFlow(): Flow<List<Favorite>> // Сделаем Flow для автоматического обновления

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Query("DELETE FROM favorite WHERE departure_code = :departureCode AND destination_code = :destinationCode")
    suspend fun deleteFavorite(departureCode: String, destinationCode: String)

    @Delete // Для удаления из списка избранного
    suspend fun deleteFavorite(favorite: Favorite)
}