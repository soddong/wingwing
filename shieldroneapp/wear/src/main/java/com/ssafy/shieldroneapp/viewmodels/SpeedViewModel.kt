package com.ssafy.shieldroneapp.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.shieldroneapp.data.repository.SensorRepository
import com.ssafy.shieldroneapp.data.repository.MeasureMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpeedViewModel(
    private val sensorRepository: SensorRepository
) : ViewModel() {
    val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val speed: MutableState<Double> = mutableStateOf(0.0)
    val availability: MutableState<DataTypeAvailability> =
        mutableStateOf(DataTypeAvailability.UNKNOWN)
    val uiState: MutableState<SpeedUiState> =
        mutableStateOf(SpeedUiState.Startup)

    init {
        viewModelScope.launch {
            val supported = sensorRepository.hasSpeedCapability()
            uiState.value = if (supported) {
                SpeedUiState.Supported
            } else {
                SpeedUiState.NotSupported
            }
        }

        viewModelScope.launch {
            enabled.collect {
                if (it) {
                    while (enabled.value) {
                        sensorRepository.speedMeasureFlow()
                            .takeWhile { enabled.value }
                            .collect { measureMessage ->
                                when (measureMessage) {
                                    is MeasureMessage.MeasureData -> {
                                        measureMessage.data.lastOrNull()?.let { dataPoint ->
                                            speed.value = dataPoint.value
                                        }
                                    }

                                    is MeasureMessage.MeasureAvailability -> {
                                        availability.value = measureMessage.availability
                                    }

                                    else -> {}
                                }
                            }
                        delay(1000)
                    }
                }
            }
        }
    }

    fun toggleEnabled() {
        enabled.value = !enabled.value
        if (!enabled.value) {
            availability.value = DataTypeAvailability.UNKNOWN
        }
    }

    // m/s를 km/h로 변환하는 유틸리티 함수
    fun getSpeedKmh(): Double {
        return speed.value * 3.6  // m/s * (3600/1000) = km/h
    }
}

class SpeedViewModelFactory(
    private val sensorRepository: SensorRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpeedViewModel(
                sensorRepository = sensorRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class SpeedUiState {
    object Startup : SpeedUiState()
    object NotSupported : SpeedUiState()
    object Supported : SpeedUiState()
}