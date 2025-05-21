package com.example.flightsearch // Убедитесь, что пакет правильный

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // Для by viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.flightsearch.ui.screens.FlightSearchScreen
import com.example.flightsearch.ui.theme.FlightsearchTheme // Ваша Compose тема
import com.example.flightsearch.ui.viewmodel.FlightViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем экземпляры зависимостей из Application класса
        val application = application as FlightSearchApp // Приведение к вашему Application классу
        val viewModel: FlightViewModel by viewModels { // Инициализация ViewModel
            FlightViewModel.Factory(
                application.database.airportDao(),
                application.userPreferencesRepository
            )
        }

        setContent {
            FlightsearchTheme { // Применение вашей Compose темы
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FlightSearchScreen(viewModel = viewModel) // Запуск главного экрана
                }
            }
        }
    }
}