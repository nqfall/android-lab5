package com.example.flightsearch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flightsearch.R
import com.example.flightsearch.data.local.entities.Airport
import com.example.flightsearch.ui.viewmodel.FavoriteFlight
import com.example.flightsearch.ui.viewmodel.FlightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightSearchScreen(viewModel: FlightViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedAirport by viewModel.selectedAirport.collectAsStateWithLifecycle()
    val flightDestinations by viewModel.flightDestinations.collectAsStateWithLifecycle()
    val favoriteFlights by viewModel.favoriteFlights.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name_title)) }, // Добавьте строку в strings.xml
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SearchTextField(
                searchQuery = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClearQuery = { viewModel.clearSelectionAndQuery() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (searchQuery.isBlank() && selectedAirport == null) {
                // Показываем избранное, если поиск пуст и аэропорт не выбран
                FavoriteFlightsList(
                    favoriteFlights = favoriteFlights,
                    onRemoveFavorite = { viewModel.removeFavorite(it) },
                    onFavoriteClick = { fav ->
                        // При клике на избранное, устанавливаем аэропорт отправления и ищем его направления
                        viewModel.onAirportSelected(fav.departureAirport)
                        // Можно также установить searchQuery, если это нужно для UI
                        // viewModel.onSearchQueryChanged(fav.departureAirport.name)
                    }
                )
            } else if (selectedAirport == null) {
                // Показываем результаты поиска (предложения аэропортов)
                AirportSuggestionsList(
                    airports = searchResults,
                    onAirportClick = { viewModel.onAirportSelected(it) }
                )
            } else {
                // Показываем выбранный аэропорт и направления
                selectedAirport?.let { departure ->
                    FlightResultsList(
                        departureAirport = departure,
                        destinationAirports = flightDestinations,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun SearchTextField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        label = { Text(stringResource(R.string.search_airport_hint)) }, // Добавьте строку
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_icon_desc)) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search_desc))
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AirportSuggestionsList(
    airports: List<Airport>,
    onAirportClick: (Airport) -> Unit,
    modifier: Modifier = Modifier
) {
    if (airports.isEmpty()) {
        Text(
            stringResource(R.string.no_airports_found), // Добавьте строку
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }
    LazyColumn(modifier = modifier) {
        items(airports, key = { it.id }) { airport ->
            ListItem(
                headlineContent = { Text("${airport.iataCode} - ${airport.name}") },
                modifier = Modifier.clickable { onAirportClick(airport) }
            )
            Divider()
        }
    }
}

@Composable
fun FlightResultsList(
    departureAirport: Airport,
    destinationAirports: List<Airport>,
    viewModel: FlightViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            // Используйте форматированную строку для локализации
            stringResource(R.string.flights_from_airport, departureAirport.name),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (destinationAirports.isEmpty()) {
            Text(
                stringResource(R.string.no_destinations_found), // Добавьте строку
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            return
        }
        LazyColumn {
            items(destinationAirports, key = { it.id }) { destination ->
                FlightItemCard(
                    departureAirport = departureAirport,
                    destinationAirport = destination,
                    viewModel = viewModel
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FlightItemCard(
    departureAirport: Airport,
    destinationAirport: Airport,
    viewModel: FlightViewModel
) {
    val isFavorite by viewModel.isFavorite(departureAirport.iataCode, destinationAirport.iataCode)
        .collectAsStateWithLifecycle(initialValue = false)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AirportInfo(airport = departureAirport, label = stringResource(R.string.depart_label))
                Spacer(modifier = Modifier.height(8.dp))
                AirportInfo(airport = destinationAirport, label = stringResource(R.string.arrive_label))
            }
            FavoriteToggleButton(
                isFavorite = isFavorite,
                onClick = { viewModel.toggleFavorite(departureAirport, destinationAirport) }
            )
        }
    }
}

@Composable
fun AirportInfo(airport: Airport, label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        text = airport.iataCode,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = airport.name,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun FavoriteToggleButton(isFavorite: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (isFavorite) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            )
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.Star,
            contentDescription = if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
            tint = if (isFavorite) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}


@Composable
fun FavoriteFlightsList(
    favoriteFlights: List<FavoriteFlight>,
    onRemoveFavorite: (FavoriteFlight) -> Unit,
    onFavoriteClick: (FavoriteFlight) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            stringResource(R.string.favorite_routes_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (favoriteFlights.isEmpty()) {
            Text(
                stringResource(R.string.no_favorite_routes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            return
        }
        LazyColumn {
            items(favoriteFlights, key = { it.id }) { favFlight ->
                FavoriteFlightItem(
                    favoriteFlight = favFlight,
                    onRemoveClick = { onRemoveFavorite(favFlight) },
                    onClick = { onFavoriteClick(favFlight) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun FavoriteFlightItem(
    favoriteFlight: FavoriteFlight,
    onRemoveClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${favoriteFlight.departureAirport.iataCode} (${favoriteFlight.departureAirport.name})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${favoriteFlight.destinationAirport.iataCode} (${favoriteFlight.destinationAirport.name})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onRemoveClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.remove_from_favorites),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}