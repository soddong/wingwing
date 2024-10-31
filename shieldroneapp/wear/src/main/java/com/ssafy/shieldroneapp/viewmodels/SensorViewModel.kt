package com.ssafy.shieldroneapp.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ssafy.shieldroneapp.services.WearableService

class SensorViewModel : ViewModel() {
    init {
        WearableService.setSensorViewModel(this)
    }

    var currentHeartRate by mutableStateOf(0)
        private set

    var currentScreen by mutableStateOf<Screen>(Screen.Main)
        private set

    sealed class Screen {
        object Main : Screen()
        object Alert : Screen()
    }

    fun showAlert() {
        currentScreen = Screen.Alert
    }

    fun hideAlert() {
        currentScreen = Screen.Main
    }

    override fun onCleared() {
        super.onCleared()
        if (WearableService.getSensorViewModel() == this) {
            WearableService.setSensorViewModel(null)
        }
    }
}
