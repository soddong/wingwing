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
import kotlinx.coroutines.launch

class SensorViewModel(
    private val sensorRepository: SensorRepository
) : ViewModel() {
    val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val hr: MutableState<Double> = mutableStateOf(0.0)
    val availability: MutableState<DataTypeAvailability> =
        mutableStateOf(DataTypeAvailability.UNKNOWN)
    val uiState: MutableState<HeartRateMeasureUiState> =
        mutableStateOf(HeartRateMeasureUiState.Startup)

    init {
        viewModelScope.launch {
            val supported = sensorRepository.hasHeartRateCapability()
            uiState.value = if (supported) {
                HeartRateMeasureUiState.Supported
            } else {
                HeartRateMeasureUiState.NotSupported
            }
        }

        viewModelScope.launch {
            enabled.collect {
                if (it) {
                    sensorRepository.heartRateMeasureFlow()
                        .takeWhile { enabled.value }
                        .collect { measureMessage ->
                            when (measureMessage) {
                                is MeasureMessage.MeasureData -> {
                                    hr.value = measureMessage.data.last().value
                                }
                                is MeasureMessage.MeasureAvailability -> {
                                    availability.value = measureMessage.availability
                                }
                            }
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
}

class HeartRateMeasureViewModelFactory(
    private val sensorRepository: SensorRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SensorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorViewModel(
                sensorRepository = sensorRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class HeartRateMeasureUiState {
    object Startup : HeartRateMeasureUiState()
    object NotSupported : HeartRateMeasureUiState()
    object Supported : HeartRateMeasureUiState()
}