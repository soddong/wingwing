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
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.flightcontroller.FCGoHomeState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class FlightControlVM : ViewModel() {

    private val TAG = "FlightControlVM"

    // 1. 라이브데이터 및 필요한 필드
    private val flightControlModel = FlightControlModel()
    private val handler = Handler(Looper.getMainLooper())

    private val _virtualMessage = MutableStateFlow<String?>(null)
    val virtualMessage: StateFlow<String?> get() = _virtualMessage.asStateFlow()

    private val _gpsSignalLevel = MutableStateFlow<Int?>(null)
    val gpsSignalLevel: StateFlow<Int?> get() = _gpsSignalLevel.asStateFlow()

    private val _targetPosition = MutableStateFlow<Position?>(null)
    val targetPosition: StateFlow<Position?> get() = _targetPosition.asStateFlow()

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

    private var _goHomeState = MutableStateFlow(FCGoHomeState.UNKNOWN)
    val goHomeState: StateFlow<FCGoHomeState> = _goHomeState.asStateFlow()

    private var _isStart = MutableStateFlow(false)
    val isStart: StateFlow<Boolean> = _isStart.asStateFlow()
    private val _homeLocation =
        MutableStateFlow(LocationCoordinate2D(0.0, 0.0))
    val homeLocation: StateFlow<LocationCoordinate2D> = _homeLocation.asStateFlow()
    var altitude: Int by mutableStateOf(0)
        private set
    var pitch: Int by mutableStateOf(0)
        private set
    private var isReturningHome = false  // 홈 복귀 여부를 추적
    private var reachedTargetTime: Long? = null  // 3미터 이내 도달 시간 기록
    private var altitudeAdjustmentJob: Job? = null
    val routeListener = object : RouteAdapter.RouteListener {
        override fun onRouteUpdate(
            locationLat: Double,
            locationLng: Double,
            destLat: Double,
            destLng: Double,
            altitude: Double,
            startFlag: Boolean
        ) {
            _isStart.value = startFlag
            if (startFlag) {
                try {
                    startTakeOff()
                    Log.i(TAG, "이륙에 성공했습니다.")
                } catch (e: Exception) {
                    Log.e(TAG, "이륙 중 예외 발생: ${e.message}")
                    // 예외 발생 시 필요한 추가 처리 (예: 사용자에게 알림, 재시도 로직 등)
                }
            }
            // 위치 값이 유효한지 확인
            if (locationLat.isNaN() || locationLng.isNaN() || destLat.isNaN() || destLng.isNaN()) {
                Log.e(TAG, "Invalid location data received.")
                return
            }

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
//                    startTimerForReturnToHome()
                    Log.d(TAG, "도달했습니다.")
                }
            } else {
                // 3미터 범위를 벗어났을 경우 초기화
                reachedTargetTime = null
                isReturningHome = false
                Log.d(TAG, "범위를 벗어났습니다.")
            }
        }
    }

    val routeModel: RouteModel = RouteModel(routeListener)

    init {
        flightControlModel.subscribeDroneState {

                state ->
            _droneState.value = state
        }
        flightControlModel.subscribeVirtualStickState { stick ->
            _virtualStickState.value = stick
        }
        flightControlModel.subscribeGoHomeState { home ->
            _goHomeState.value = home
        }
        flightControlModel.subscribeHomeLocation { location ->
            _homeLocation.value = location
        }

    }

    fun startReceivingLocation() = routeModel.startReceivingLocation()
    fun stopReceivingLocation() = routeModel.stopReceivingLocation()

    fun startTakeOff() {
        val callback = createCompletionCallback("이륙 시작", "이륙 실패")
        flightControlModel.startTakeOff(callback)
    }

    fun startLanding() {
        val callback = createCompletionCallback("착륙 시작", "착륙 실패")
        flightControlModel.startLanding(callback)
    }

//    fun ascendToCruiseAltitude(altitudeSpeed: Int, targetAltitude: Float = 3.2f) {
//        CoroutineScope(Dispatchers.Default).launch {
//            try {
//                adjustAltitudeCoroutine(altitudeSpeed, targetAltitude)
//            } catch (e: Exception) {
//                Log.e(TAG, "Cruise Altitude 상승 실패: ${e.message}")
//            }
//        }
//    }
//
//    private suspend fun adjustAltitudeCoroutine(altitudeSpeed: Int, targetAltitude: Float) {
//        while (true) {
//            val currentAltitude = _droneState.value?.altitude ?: break
//            if (currentAltitude >= targetAltitude) break
//
//            val altitudeRatio = currentAltitude / targetAltitude
//            val adjustmentSpeed = calculateSpeed(altitudeSpeed, altitudeRatio)
//
//            flightControlModel.adjustAltitude(adjustmentSpeed)
//            delay(100L)
//        }
//    }

    private fun calculateSpeed(baseSpeed: Int, ratio: Double): Int {
        return when {
            ratio < 0.2 -> (baseSpeed * 0.3).toInt()
            ratio < 0.8 -> baseSpeed
            else -> (baseSpeed * 0.5).toInt()
        }
    }

    fun enableVirtualStickMode() {
        val callback = createCompletionCallback("VIRTUAL 활성화", "Virtual Stick 활성화 실패")
        flightControlModel.enableVirtualStickMode(callback)
    }

    fun disableVirtualStickMode() {
        val callback = createCompletionCallback("VIRTUAL 비활성화", "Virtual Stick 비활성화 실패")
        flightControlModel.disableVirtualStickMode(callback)
    }


//    fun adjustYaw(yawDifference: Double) {
//        try {
//            flightControlModel.adjustYaw(yawDifference)
//        } catch (e: Exception) {
//            Log.e(TAG, "Yaw 조정 실패: ${e.message}")
//        }
//    }
//
//
//    fun adjustAltitude() {
//        try {
//            flightControlModel.adjustAltitude(altitude)
//        } catch (e: Exception) {
//            Log.e(TAG, "Altitude 조정 실패: ${e.message}")
//        }
//    }

    /**
     * yaw와 altitude를 동시에 조절하는 메서드
     */
    fun adjustLeftStick(yawDifference: Double, targetAltitude: Double) {
        if (altitudeAdjustmentJob?.isActive == true) {
            // 이미 고도 조절이 진행 중인 경우 함수 종료
            return
        }
        altitudeAdjustmentJob = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                val altitudeSpeed = 20
                val currentAltitude = _droneState.value?.altitude ?: break
                if (currentAltitude >= targetAltitude) break

                val altitudeRatio = currentAltitude / targetAltitude

                val altitudeDifference = targetAltitude - currentAltitude
                val threshold = 0.1 // 허용 오차 범위 (예: 0.1미터)

                if (abs(altitudeDifference) <= threshold) {
                    break
                }

                val adjustmentSpeed = calculateSpeed(altitudeSpeed, altitudeRatio)
                flightControlModel.adjustLeftStick(yawDifference, adjustmentSpeed)
                delay(100L)
            }
        }
    }

    fun adjustPitch() {
        try {
            flightControlModel.adjustPitch(pitch)
        } catch (e: Exception) {
            Log.e(TAG, "Pitch 조정 실패: ${e.message}")
        }
    }

    fun updatePitch(value: Int) {
        pitch = value
    }

    fun updateAltitude(value: Int) {
        altitude = value
    }

    private fun startReturnToHome() {
        val callback = createCompletionCallback("홈으로 복귀 시작", "홈으로 복귀 실패")
        flightControlModel.startReturnToHome(callback)
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

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        routeModel.stopReceivingLocation()
    }


}
