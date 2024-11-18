package com.ssafy.shieldroneapp

import androidx.compose.runtime.Composable
import com.ssafy.shieldroneapp.core.ui.components.Layout
import com.ssafy.shieldroneapp.features.main.ui.MainScreen
import com.ssafy.shieldroneapp.core.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.domain.repository.SensorRepository
import com.ssafy.shieldroneapp.domain.repository.DataRepository
import com.ssafy.shieldroneapp.data.remote.AlertHandler
import com.ssafy.shieldroneapp.data.remote.WearConnectionManager

@Composable
fun WearApp(
    sensorRepository: SensorRepository,
    dataRepository: DataRepository,
    wearConnectionManager: WearConnectionManager,
    alertHandler: AlertHandler,
) {
    ShieldroneappTheme {
        Layout(
            hasClock = true
        ) {
            MainScreen(
                sensorRepository = sensorRepository,
                dataRepository = dataRepository,
                wearConnectionManager = wearConnectionManager,
                alertHandler= alertHandler,
            )
        }
    }
}