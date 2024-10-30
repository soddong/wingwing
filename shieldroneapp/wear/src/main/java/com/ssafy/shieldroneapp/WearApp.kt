package com.ssafy.shieldroneapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import com.ssafy.shieldroneapp.ui.components.Layout
import com.ssafy.shieldroneapp.ui.screens.MainScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.viewmodels.SensorViewModel
import androidx.compose.runtime.remember

@Composable
fun WearApp() {
    val sensorViewModel = remember { SensorViewModel() }  
    
    ShieldroneappTheme {
        Layout(
            children = {
                MainScreen(sensorViewModel = sensorViewModel)
            }
        )
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}