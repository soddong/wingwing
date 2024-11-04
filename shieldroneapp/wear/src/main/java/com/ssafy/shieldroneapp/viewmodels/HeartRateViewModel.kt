package com.ssafy.shieldroneapp.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.shieldroneapp.data.model.DataAvailability
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.repository.DataRepository
import com.ssafy.shieldroneapp.data.repository.SensorRepository
import com.ssafy.shieldroneapp.data.repository.MeasureMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class HeartRateViewModel(
    private val sensorRepository: SensorRepository,
    private val dataRepository: DataRepository
) : ViewModel() {
    private val enabled = MutableStateFlow(true)
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
                                    val bpm = measureMessage.data.last().value
                                    hr.value = bpm
                                    sendHeartRateData(bpm)
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

    private fun sendHeartRateData(bpm: Double) {
        viewModelScope.launch {
            val heartRateData = HeartRateData(
                bpm = bpm,
                availability = when (availability.value) {
                    DataTypeAvailability.AVAILABLE -> DataAvailability.AVAILABLE
                    DataTypeAvailability.ACQUIRING -> DataAvailability.ACQUIRING
                    DataTypeAvailability.UNAVAILABLE -> DataAvailability.UNAVAILABLE
                    else -> DataAvailability.UNKNOWN
                }
            )
            dataRepository.sendHeartRateData(heartRateData)
        }
    }

    fun toggleEnabled() {
        if (!enabled.value) {
            enabled.value = true
        }
    }
}

class HeartRateMeasureViewModelFactory(
    private val sensorRepository: SensorRepository,
    private val dataRepository: DataRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HeartRateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HeartRateViewModel(
                sensorRepository = sensorRepository,
                dataRepository = dataRepository
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