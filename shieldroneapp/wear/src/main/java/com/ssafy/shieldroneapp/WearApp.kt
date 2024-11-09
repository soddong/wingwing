package com.ssafy.shieldroneapp

import androidx.compose.runtime.Composable
import com.ssafy.shieldroneapp.ui.components.Layout
import com.ssafy.shieldroneapp.ui.screens.MainScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.data.repository.SensorRepository
import com.ssafy.shieldroneapp.data.repository.DataRepository
import com.ssafy.shieldroneapp.services.connection.WearConnectionManager

@Composable
fun WearApp(
    sensorRepository: SensorRepository,
    dataRepository: DataRepository,
    wearConnectionManager: WearConnectionManager
) {
    ShieldroneappTheme {
        Layout(
            hasClock = true
        ) {
            MainScreen(
                sensorRepository = sensorRepository,
                dataRepository = dataRepository,
                wearConnectionManager = wearConnectionManager
            )
        }
    }
}