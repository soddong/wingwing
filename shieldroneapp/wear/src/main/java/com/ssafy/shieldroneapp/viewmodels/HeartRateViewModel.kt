package com.ssafy.shieldroneapp.viewmodels

import android.util.Log
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
import kotlinx.coroutines.Job
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

    private var measureJob: Job? = null

    init {
        viewModelScope.launch {
            val supported = sensorRepository.hasHeartRateCapability()
            if (supported) {
                uiState.value = HeartRateMeasureUiState.Supported
                Log.d("HeartRateViewModel", "심박수 센서 지원됨")
            } else {
                uiState.value = HeartRateMeasureUiState.NotSupported
                Log.d("HeartRateViewModel", "심박수 센서 지원 안됨")
            }
        }
    }

    fun toggleEnabled() {
        startMeasuring()
    }

    fun startMeasuring() {
        Log.d("HeartRateViewModel", "심박수 측정 시작")
        measureJob?.cancel() // 기존 job이 있다면 취소

        measureJob = viewModelScope.launch {
            try {
                sensorRepository.heartRateMeasureFlow()
                    .collect { measureMessage ->
                        when (measureMessage) {
                            is MeasureMessage.MeasureData -> {
                                val bpm = measureMessage.data.last().value
                                hr.value = bpm
                                Log.d("HeartRateViewModel", "심박수: $bpm")
                                sendHeartRateData(bpm)
                            }
                            is MeasureMessage.MeasureAvailability -> {
                                availability.value = measureMessage.availability
                                Log.d("HeartRateViewModel", "가용성 변경: ${measureMessage.availability}")
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("HeartRateViewModel", "측정 중 오류 발생", e)
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

    override fun onCleared() {
        super.onCleared()
        measureJob?.cancel()
        availability.value = DataTypeAvailability.UNKNOWN
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