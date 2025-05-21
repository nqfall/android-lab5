package com.example.flightsearch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.flightsearch.data.local.dao.AirportDao
import com.example.flightsearch.data.local.datastore.UserPreferencesRepository
import com.example.flightsearch.data.local.entities.Airport
import com.example.flightsearch.data.local.entities.Favorite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Data class для отображения избранных рейсов с деталями
data class FavoriteFlight(
    val id: Int,
    val departureAirport: Airport,
    val destinationAirport: Airport
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FlightViewModel(
    private val airportDao: AirportDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAirport = MutableStateFlow<Airport?>(null)
    val selectedAirport: StateFlow<Airport?> = _selectedAirport.asStateFlow()

    val searchResults: StateFlow<List<Airport>> = _searchQuery
        .debounce(300) // Небольшая задержка для уменьшения числа запросов при вводе
        .filter { it.isNotBlank() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            flow { emit(airportDao.searchAirports("%$query%")) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val flightDestinations: StateFlow<List<Airport>> = _selectedAirport
        .filterNotNull()
        .flatMapLatest { departureAirport ->
            flow {
                // Получаем все аэропорты и фильтруем, чтобы не было совпадений
                val allAirports = airportDao.getAllAirports()
                emit(allAirports.filter { it.iataCode != departureAirport.iataCode })
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Используем Flow из DAO и трансформируем его
    val favoriteFlights: StateFlow<List<FavoriteFlight>> = airportDao.getFavoritesFlow()
        .map { favorites ->
            favorites.mapNotNull { fav ->
                val dep = airportDao.getAirportByIataCode(fav.departureCode)
                val dest = airportDao.getAirportByIataCode(fav.destinationCode)
                if (dep != null && dest != null) {
                    FavoriteFlight(fav.id, dep, dest)
                } else {
                    null // Если аэропорт не найден, пропускаем
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )


    init {
        viewModelScope.launch {
            userPreferencesRepository.searchQuery.collect { query ->
                _searchQuery.value = query
                if (query.isNotBlank()) {
                    // searchResults обновится автоматически через flatMapLatest
                } else {
                    _selectedAirport.value = null // Сбрасываем выбранный аэропорт, если запрос пуст
                }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        _selectedAirport.value = null // Сбрасываем выбор при новом поиске
        viewModelScope.launch {
            userPreferencesRepository.saveSearchQuery(newQuery)
        }
    }

    fun onAirportSelected(airport: Airport) {
        _selectedAirport.value = airport
        // flightDestinations обновится автоматически
    }

    fun clearSelectionAndQuery() {
        _searchQuery.value = ""
        _selectedAirport.value = null
        viewModelScope.launch {
            userPreferencesRepository.saveSearchQuery("")
        }
    }


    fun isFavorite(departureCode: String, destinationCode: String): Flow<Boolean> {
        return airportDao.getFavoritesFlow().map { favorites ->
            favorites.any { it.departureCode == departureCode && it.destinationCode == destinationCode }
        }
    }

    fun toggleFavorite(departureAirport: Airport, destinationAirport: Airport) {
        viewModelScope.launch {
            val isCurrentlyFavorite = favoriteFlights.value.any {
                it.departureAirport.iataCode == departureAirport.iataCode &&
                        it.destinationAirport.iataCode == destinationAirport.iataCode
            }

            if (isCurrentlyFavorite) {
                airportDao.deleteFavorite(departureAirport.iataCode, destinationAirport.iataCode)
            } else {
                airportDao.insertFavorite(
                    Favorite(
                        departureCode = departureAirport.iataCode,
                        destinationCode = destinationAirport.iataCode
                    )
                )
            }
        }
    }

    fun removeFavorite(favoriteFlight: FavoriteFlight) {
        viewModelScope.launch {
            // Используем id из FavoriteFlight, который соответствует Favorite.id
            airportDao.deleteFavorite(Favorite(id = favoriteFlight.id, departureCode = favoriteFlight.departureAirport.iataCode, destinationCode = favoriteFlight.destinationAirport.iataCode))
        }
    }


    // ViewModel Factory
    companion object {
        fun Factory(
            airportDao: AirportDao,
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FlightViewModel::class.java)) {
                    return FlightViewModel(airportDao, userPreferencesRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}