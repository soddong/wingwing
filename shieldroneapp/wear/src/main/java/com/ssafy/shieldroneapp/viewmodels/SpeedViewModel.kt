package com.ssafy.shieldroneapp.viewmodels

import android.util.Log
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

//    init {
//        viewModelScope.launch {
//            val supported = sensorRepository.hasSpeedCapability()
//            Log.d("SpeedViewModel", "Speed capability supported: $supported")
//            uiState.value = if (supported) {
//                SpeedUiState.Supported
//            } else {
//                SpeedUiState.NotSupported
//            }
//        }

//    init {
//        viewModelScope.launch {
//            // 먼저 초기 capability 체크
//            val hasCapability = sensorRepository.hasSpeedCapability()
//            Log.d("SpeedViewModel", "Initial speed capability check: $hasCapability")
//
//            // capability 상태에 따라 UI 상태 업데이트
//            uiState.value = if (hasCapability) {
//                SpeedUiState.Supported
//            } else {
//                SpeedUiState.NotSupported
//            }
//
//            // 이후 capability 변화 관찰
//            sensorRepository.speedCapabilityFlow.collect { supported ->
//                Log.d("SpeedViewModel", "Speed capability changed: $supported")
//                uiState.value = if (supported) {
//                    SpeedUiState.Supported
//                } else {
//                    SpeedUiState.NotSupported
//                }
//            }
//        }
//    init {
//        viewModelScope.launch {
//            // 초기 capability 체크
//            val hasCapability = sensorRepository.hasSpeedCapability()
//            Log.d("SpeedViewModel", "Initial speed capability check: $hasCapability")
//
//            // 이미 Startup이 한번 떴다면, NotSupported로 변경하지 않음
//            if (uiState.value == SpeedUiState.Startup) {
//                uiState.value = SpeedUiState.Supported
//            }
//
//            // capability 변화 관찰
//            sensorRepository.speedCapabilityFlow.collect { supported ->
//                Log.d("SpeedViewModel", "Speed capability changed: $supported")
//                // Startup이 한번 떴다면 NotSupported로 변경하지 않음
//                if (uiState.value == SpeedUiState.Startup) {
//                    uiState.value = SpeedUiState.Supported
//                }
//            }
//        }
//
//        viewModelScope.launch {
//            val supported = sensorRepository.hasSpeedCapability()
//            Log.d("SpeedViewModel", "Initial speed capability supported: $supported")
//        }
//
//        viewModelScope.launch {
//            enabled.collect {
//                if (it) {
//                    Log.d("SpeedViewModel", "Starting speed collection")
//                    while (enabled.value) {
//                        sensorRepository.speedMeasureFlow()
//                            .takeWhile { enabled.value }
//                            .collect { measureMessage ->
//                                Log.d("SpeedViewModel", "Received message: $measureMessage")
//                                when (measureMessage) {
//                                    is MeasureMessage.MeasureData -> {
//                                        measureMessage.data.lastOrNull()?.let { dataPoint ->
//                                            Log.d(
//                                                "SpeedViewModel",
//                                                "New speed value: ${dataPoint.value}"
//                                            )
//                                            speed.value = dataPoint.value
//                                            availability.value = DataTypeAvailability.AVAILABLE
//                                        }
//                                    }
//
//                                    is MeasureMessage.MeasureAvailability -> {
//                                        Log.d(
//                                            "SpeedViewModel",
//                                            "Availability changed: ${measureMessage.availability}"
//                                        )
//                                        availability.value = measureMessage.availability
//                                    }
//                                }
//                            }
//                        delay(1000)
//                    }
//                } else {
//                    Log.d("SpeedViewModel", "Speed collection disabled")
//                    availability.value = DataTypeAvailability.UNKNOWN
//                }
//            }
//        }
//    }
//

    // init {
    //     viewModelScope.launch {
    //         enabled.collect { isEnabled ->
    //             Log.d("SpeedViewModel", "Enabled state changed to: $isEnabled")
    //             if (isEnabled) {
    //                 Log.d("SpeedViewModel", "Starting speed collection")
    //                 while (enabled.value) {
    //                     try {
    //                         sensorRepository.speedMeasureFlow()
    //                             .takeWhile { enabled.value }
    //                             .collect { measureMessage ->
    //                                 Log.d("SpeedViewModel", "Received message: $measureMessage")
    //                                 when (measureMessage) {
    //                                     is MeasureMessage.MeasureData -> {
    //                                         measureMessage.data.lastOrNull()?.let { dataPoint ->
    //                                             Log.d("SpeedViewModel", "New speed value: ${dataPoint.value}")
    //                                             speed.value = dataPoint.value
    //                                             availability.value = DataTypeAvailability.AVAILABLE
    //                                         }
    //                                     }
    //                                     is MeasureMessage.MeasureAvailability -> {
    //                                         Log.d("SpeedViewModel", "Availability changed: ${measureMessage.availability}")
    //                                         availability.value = measureMessage.availability
    //                                     }
    //                                 }
    //                             }
    //                     } catch (e: Exception) {
    //                         Log.e("SpeedViewModel", "Error collecting speed data", e)
    //                     }
    //                     delay(1000)
    //                 }
    //             } else {
    //                 Log.d("SpeedViewModel", "Speed collection disabled")
    //             }
    //         }
    //     }
    // }
    init {
        viewModelScope.launch {
            // capability 상태 관찰
            sensorRepository.speedCapabilityFlow.collect { supported ->
                Log.d("SpeedViewModel", "Speed capability changed: $supported")
                // Startup 상태에서는 무조건 Supported로 변경
                if (uiState.value == SpeedUiState.Startup) {
                    Log.d("SpeedViewModel", "Changing from Startup to Supported")
                    uiState.value = SpeedUiState.Supported
                }
            }
        }
    
        viewModelScope.launch {
            enabled.collect { isEnabled ->
                Log.d("SpeedViewModel", "Enabled state changed to: $isEnabled")
                if (isEnabled) {
                    Log.d("SpeedViewModel", "Starting speed collection")
                    availability.value = DataTypeAvailability.ACQUIRING  // 측정 시작할 때 ACQUIRING으로 설정
                    try {
                        sensorRepository.speedMeasureFlow()
                            .collect { measureMessage ->
                                when (measureMessage) {
                                    is MeasureMessage.MeasureData -> {
                                        measureMessage.data.lastOrNull()?.let { dataPoint ->
                                            speed.value = dataPoint.value
                                            availability.value = DataTypeAvailability.AVAILABLE
                                        }
                                    }
                                    is MeasureMessage.MeasureAvailability -> {
                                        Log.d("SpeedViewModel", "Availability changed: ${measureMessage.availability}")
                                        availability.value = measureMessage.availability
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        Log.e("SpeedViewModel", "Error collecting speed data", e)
                        availability.value = DataTypeAvailability.UNKNOWN
                    }
                } else {
                    Log.d("SpeedViewModel", "Speed collection disabled")
                    availability.value = DataTypeAvailability.UNKNOWN
                }
            }
        }
    }

    // 권한 변경 시 capability 재체크
    fun recheckCapability() {
        viewModelScope.launch {
            sensorRepository.checkSpeedCapability()
        }
    }

    fun toggleEnabled() {
        Log.d("SpeedViewModel", "Toggle enabled from ${enabled.value} to ${!enabled.value}")
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