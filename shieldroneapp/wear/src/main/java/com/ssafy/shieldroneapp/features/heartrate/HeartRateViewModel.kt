package com.ssafy.shieldroneapp.features.heartrate

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.shieldroneapp.domain.repository.DataRepository
import com.ssafy.shieldroneapp.domain.repository.SensorRepository
import com.ssafy.shieldroneapp.services.HeartRateService
import com.ssafy.shieldroneapp.services.WearableService
import kotlinx.coroutines.launch

class HeartRateViewModel(
    private val sensorRepository: SensorRepository,
    private val dataRepository: DataRepository
) : ViewModel() {
    val hr: MutableState<Double> = mutableStateOf(0.0)
    val availability: MutableState<DataTypeAvailability> =
        mutableStateOf(DataTypeAvailability.UNKNOWN)
    val uiState: MutableState<HeartRateMeasureUiState> =
        mutableStateOf(HeartRateMeasureUiState.Startup)

    init {
        viewModelScope.launch {
            try {
                val supported = sensorRepository.hasHeartRateCapability()
                if (supported) {
                    uiState.value = HeartRateMeasureUiState.Supported
                    Log.d("HeartRateViewModel", "심박수 센서 지원됨")
                    WearableService.setHeartRateViewModel(this@HeartRateViewModel)

                    // 서비스 시작
                    val context = WearableService.getContext()
                    if (context != null) {
                        Log.d("HeartRateViewModel", "서비스 자동 시작")
                        val serviceIntent = Intent(context, HeartRateService::class.java)
                        context.startForegroundService(serviceIntent)
                    } else {
                        Log.e("HeartRateViewModel", "Context가 null입니다")
                    }
                } else {
                    uiState.value = HeartRateMeasureUiState.NotSupported
                    Log.d("HeartRateViewModel", "심박수 센서 지원 안됨")
                }
            } catch (e: Exception) {
                Log.e("HeartRateViewModel", "초기화 중 오류 발생", e)
                e.printStackTrace()
            }
        }
    }

    fun toggleEnabled() {
        Log.d("HeartRateViewModel", "서비스 시작")
        val context = WearableService.getContext() ?: return
        val serviceIntent = Intent(context, HeartRateService::class.java)
        context.startForegroundService(serviceIntent)
    }

    fun updateHeartRate(bpm: Double) {
        hr.value = bpm
    }

    fun updateAvailability(newAvailability: DataTypeAvailability) {
        availability.value = newAvailability
    }

    override fun onCleared() {
        super.onCleared()
        val context = WearableService.getContext()
        context?.stopService(Intent(context, HeartRateService::class.java))
        WearableService.setHeartRateViewModel(null)
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