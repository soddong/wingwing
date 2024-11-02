package com.ssafy.shieldroneapp

import androidx.compose.runtime.Composable
import com.ssafy.shieldroneapp.ui.components.Layout
import com.ssafy.shieldroneapp.ui.screens.MainScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.data.repository.SensorRepository


@Composable
fun WearApp(
    sensorRepository: SensorRepository
) {
    ShieldroneappTheme {
        Layout(
            children = {
                MainScreen(
                    sensorRepository = sensorRepository
                )
            },
            hasClock = true
        )
    }
}