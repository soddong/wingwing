package com.ssafy.shieldroneapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val application = application as MainApplication
        val sensorRepository = application.sensorRepository
        val dataRepository = application.dataRepository

        setTheme(android.R.style.Theme_DeviceDefault_Light)

        setContent {
            WearApp(
                sensorRepository = sensorRepository,
                dataRepository = dataRepository
            )
        }
    }
}