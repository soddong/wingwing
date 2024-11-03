package com.ssafy.shieldroneapp

import androidx.compose.runtime.Composable
import com.ssafy.shieldroneapp.ui.components.Layout
import com.ssafy.shieldroneapp.ui.screens.MainScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.data.repository.SensorRepository
import com.ssafy.shieldroneapp.data.repository.DataRepository

@Composable
fun WearApp(
    sensorRepository: SensorRepository,
    dataRepository: DataRepository
) {
    ShieldroneappTheme {
        Layout(
            children = {
                MainScreen(
                    sensorRepository = sensorRepository,
                    dataRepository = dataRepository
                )
            },
            hasClock = true
        )
    }
}
