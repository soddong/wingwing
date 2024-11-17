package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.shieldrone.station.data.Position
import com.shieldrone.station.data.State
import com.shieldrone.station.service.route.RouteAdapter
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class FlightControlGPSVM : ViewModel() {
    private val TAG = "FlightControlGPSVM"
    private val handler = Handler(Looper.getMainLooper())
    private val flightControlGPSModel = FlightControlGPSModel()

    private val _droneState = MutableStateFlow<State?>(null)
    val droneState: StateFlow<State?> = _droneState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _virtualStickState = MutableStateFlow<String?>(null)
    val virtualStickState: StateFlow<String?> = _virtualStickState.asStateFlow()

    private val _currentLocation = MutableStateFlow(Position(0.0, 0.0, 0.0))
    val currentLocation: StateFlow<Position> = _currentLocation.asStateFlow()

    private val _destinationLocation = MutableStateFlow(Position(0.0, 0.0, 0.0))
    val destinationLocation: StateFlow<Position> = _destinationLocation.asStateFlow()

    var altitude: Int by mutableStateOf(0)
        private set
    var pitch: Int by mutableStateOf(0)
        private set
    private var isReturningHome = false  // 홈 복귀 여부를 추적
    private var reachedTargetTime: Long? = null  // 3미터 이내 도달 시간 기록

    val routeListener = object : RouteAdapter.RouteListener {
        override fun onRouteUpdate(
            locationLat: Double,
            locationLng: Double,
            destLat: Double,
            destLng: Double,
            altitude: Double
        ) {
            _currentLocation.value = Position(locationLat, locationLng, altitude)
            _destinationLocation.value = Position(destLat, destLng, altitude)

            val latDiff = abs(locationLat - destLat)
            val lngDiff = abs(locationLng - destLng)
            val threshold = 0.000027  // 대략적인 3미터 범위

            // 3미터 이내에 도달
            if (latDiff <= threshold && lngDiff <= threshold) {
                // 이미 타이머가 실행 중인 경우 처리하지 않음
                if (reachedTargetTime == null) {
                    reachedTargetTime = System.currentTimeMillis()
                    startTimerForReturnToHome()
                }
            } else {
                // 3미터 범위를 벗어났을 경우 초기화
                reachedTargetTime = null
                isReturningHome = false
            }
        }
    }

    val routeModel: RouteModel = RouteModel(routeListener)

    init {
        flightControlGPSModel.apply {
            subscribeDroneState { _droneState.value = it }
            subscribeVirtualStickState { _virtualStickState.value = it }
        }
    }

    fun startReceivingLocation() = routeModel.startReceivingLocation()
    fun stopReceivingLocation() = routeModel.stopReceivingLocation()

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "[ViewModel] 리소스 해제")
    }


    fun startTakeOff() {
        val callback = createCompletionCallback("이륙 시작", "이륙 실패")
        flightControlGPSModel.startTakeOff(callback)
    }

    fun startLanding() {
        val callback = createCompletionCallback("착륙 시작", "착륙 실패")
        flightControlGPSModel.startLanding(callback)
    }

    fun ascendToCruiseAltitude(altitudeSpeed: Int, targetAltitude: Float = 3.2f) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                adjustAltitudeCoroutine(altitudeSpeed, targetAltitude)
            } catch (e: Exception) {
                Log.e(TAG, "Cruise Altitude 상승 실패: ${e.message}")
            }
        }
    }

    private suspend fun adjustAltitudeCoroutine(altitudeSpeed: Int, targetAltitude: Float) {
        while (true) {
            val currentAltitude = _droneState.value?.altitude ?: break
            if (currentAltitude >= targetAltitude) break

            val altitudeRatio = currentAltitude / targetAltitude
            val adjustmentSpeed = calculateSpeed(altitudeSpeed, altitudeRatio)

            flightControlGPSModel.adjustAltitude(adjustmentSpeed)
            delay(100L)
        }
    }

    private fun calculateSpeed(baseSpeed: Int, ratio: Double): Int {
        return when {
            ratio < 0.2 -> (baseSpeed * 0.3).toInt()
            ratio < 0.8 -> baseSpeed
            else -> (baseSpeed * 0.5).toInt()
        }
    }

    fun enableVirtualStickMode() {
        val callback = createCompletionCallback("VIRTUAL 활성화", "Virtual Stick 활성화 실패")
        flightControlGPSModel.enableVirtualStickMode(callback)
    }

    fun disableVirtualStickMode() {
        val callback = createCompletionCallback("VIRTUAL 비활성화", "Virtual Stick 비활성화 실패")
        flightControlGPSModel.disableVirtualStickMode(callback)
    }


    fun adjustYaw(yawDifference: Double) {
        try {
            flightControlGPSModel.adjustYaw(yawDifference)
        } catch (e: Exception) {
            Log.e(TAG, "Yaw 조정 실패: ${e.message}")
        }
    }

    fun adjustPitch() {
        try {
            flightControlGPSModel.adjustPitch(pitch)
        } catch (e: Exception) {
            Log.e(TAG, "Pitch 조정 실패: ${e.message}")
        }
    }

    fun adjustAltitude() {
        try {
            flightControlGPSModel.adjustAltitude(altitude)
        } catch (e: Exception) {
            Log.e(TAG, "Altitude 조정 실패: ${e.message}")
        }
    }

    fun updatePitch(value: Int) {
        pitch = value
    }

    fun updateAltitude(value: Int) {
        altitude = value
    }

    private fun handleSuccess(message: String): CommonCallbacks.CompletionCallback {
        return object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.value = message
                Log.d(TAG, message)
            }

            override fun onFailure(error: IDJIError) {}
        }
    }

    private fun startReturnToHome() {
        val callback = createCompletionCallback("홈으로 복귀 시작", "홈으로 복귀 실패")
        flightControlGPSModel.startReturnToHome(callback)
    }

    private fun startTimerForReturnToHome() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // 10초 대기
                delay(10_000L)

                // 타이머 종료 후 홈 복귀 호출 (이전 상태 확인)
                if (!isReturningHome && reachedTargetTime != null) {
                    isReturningHome = true
                    startReturnToHome()
                }
            } catch (e: Exception) {
                Log.e(TAG, "타이머 실행 중 오류 발생: ${e.message}")
            }
        }
    }

    private fun createCompletionCallback(
        successMessage: String,
        failureMessage: String
    ): CommonCallbacks.CompletionCallback {
        return object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.value = successMessage
                Log.d(TAG, successMessage)
            }

            override fun onFailure(error: IDJIError) {
                _message.value = "$failureMessage: ${error.description()}"
                Log.e(TAG, "$failureMessage: ${error.description()}")
            }
        }
    }
}