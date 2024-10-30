package com.ssafy.shieldroneapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import com.ssafy.shieldroneapp.ui.components.Layout
import com.ssafy.shieldroneapp.ui.screens.AlertScreen
import com.ssafy.shieldroneapp.ui.screens.MainScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.viewmodels.SensorViewModel

@Composable
fun WearApp() {
    val sensorViewModel = remember { SensorViewModel() }

    ShieldroneappTheme {
        Layout(
            children = {
                when (sensorViewModel.currentScreen) {
                    is SensorViewModel.Screen.Main -> MainScreen(
                        sensorViewModel = sensorViewModel
                    )
                    is SensorViewModel.Screen.Alert -> AlertScreen(
                        onSafeConfirm = { sensorViewModel.hideAlert() },
                    )
                }
            }
        )
    }
}

//@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
//@Composable
//fun DefaultPreview() {
//    WearApp()
//}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun AlertScreenPreview() {
    ShieldroneappTheme {
        Layout {
            AlertScreen(
                onSafeConfirm = {},
            )
        }
    }
}