package com.example.flightsearch

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.room.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

// ---------- ENTITIES ----------
@Entity(tableName = "airport")
data class Airport(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "iata_code") val iataCode: String,
    val name: String,
    val passengers: Int
)

@Entity(tableName = "favorite")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "departure_code") val departureCode: String,
    @ColumnInfo(name = "destination_code") val destinationCode: String
)

// ---------- DAO ----------
@Dao
interface AirportDao {
    @Query("SELECT * FROM airport WHERE iata_code LIKE :query OR name LIKE :query ORDER BY passengers DESC")
    suspend fun searchAirports(query: String): List<Airport>

    @Query("SELECT * FROM airport ORDER BY passengers DESC")
    suspend fun getPopularAirports(): List<Airport>

    @Query("SELECT * FROM favorite")
    suspend fun getFavorites(): List<Favorite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Delete
    suspend fun deleteFavorite(favorite: Favorite)
}

// ---------- DATABASE ----------
@Database(entities = [Airport::class, Favorite::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun airportDao(): AirportDao
}

// ---------- DATASTORE ----------
val Context.dataStore by preferencesDataStore(name = "search_pref")
val SEARCH_QUERY_KEY = stringPreferencesKey("search_query")

suspend fun saveSearchQuery(context: Context, query: String) {
    context.dataStore.edit { it[SEARCH_QUERY_KEY] = query }
}

fun getSearchQuery(context: Context): Flow<String> {
    return context.dataStore.data.map { it[SEARCH_QUERY_KEY] ?: "" }
}

@Composable
fun FlightSearchScreen(dao: AirportDao, context: Context) {
    val scope = rememberCoroutineScope()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf(listOf<Airport>()) }
    var selectedAirport by remember { mutableStateOf<Airport?>(null) }
    var destinations by remember { mutableStateOf(listOf<Airport>()) }
    var favorites by remember { mutableStateOf(listOf<Favorite>()) }

    // при старте
    LaunchedEffect(Unit) {
        getSearchQuery(context).collect { savedQuery ->
            searchQuery = savedQuery
            if (savedQuery.isBlank()) {
                favorites = dao.getFavorites()
            } else {
                searchSuggestions = dao.searchAirports("%$savedQuery%")
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                selectedAirport = null
                destinations = emptyList()
                scope.launch {
                    saveSearchQuery(context, it)
                    if (it.isBlank()) {
                        favorites = dao.getFavorites()
                        searchSuggestions = emptyList()
                    } else {
                        searchSuggestions = dao.searchAirports("%$it%")
                    }
                }
            },
            label = { Text("Аэропорт (IATA или название)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedAirport == null && searchQuery.isNotBlank()) {
            LazyColumn {
                items(searchSuggestions) { airport ->
                    TextButton(
                        onClick = {
                            selectedAirport = airport
                            scope.launch {
                                destinations = dao.getPopularAirports()
                                    .filter { it.iataCode != airport.iataCode }
                            }
                        }
                    ) {
                        Text("${airport.iataCode} — ${airport.name}")
                    }
                }
            }
        }

        selectedAirport?.let { departure ->
            Text("Рейсы из: ${departure.iataCode} — ${departure.name}", style = MaterialTheme.typography.titleMedium)

            LazyColumn {
                items(destinations) { dest ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(shape = RoundedCornerShape(10.dp))
                            .background(Color.LightGray)
                            .padding(15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Первая колонка с отображением аэропортов
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = departure.iataCode,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = departure.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dest.iataCode,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = dest.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Вторая колонка с кнопкой
                        val isFavorite = favorites.any { it.departureCode == departure.iataCode && it.destinationCode == dest.iataCode }
                        val buttonColor = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (isFavorite) {
                                            dao.deleteFavorite(Favorite(departureCode = departure.iataCode, destinationCode = dest.iataCode))
                                        } else {
                                            dao.insertFavorite(Favorite(departureCode = departure.iataCode, destinationCode = dest.iataCode))
                                        }
                                        favorites = dao.getFavorites()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                            ) {
                                Text(
                                    text = if (isFavorite) "-" else "+",
                                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
                                )
                            }
                        }
                    }


                }
            }

        }

        if (searchQuery.isBlank()) {
            Text("Избранные маршруты:", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(favorites) { fav ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape = RoundedCornerShape(10.dp))
                            .background(Color.LightGray)
                            .padding(15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${fav.departureCode} → ${fav.destinationCode}")
                        IconButton(onClick = {
                            scope.launch {
                                dao.deleteFavorite(fav)
                                favorites = dao.getFavorites()
                            }
                        }) {
                            Text("❌")
                        }
                    }
                }
            }
        }
    }
}

// ---------- MAIN ACTIVITY ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "flight-db")
            .fallbackToDestructiveMigration() // удалить в production
            .createFromAsset("flights.db") // база должна быть в assets
            .build()

        setContent {
            val context = LocalContext.current
            val dao = db.airportDao()
            FlightSearchScreen(dao, context)
        }
    }
}
