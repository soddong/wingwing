package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.shieldrone.station.data.Controls
import com.shieldrone.station.data.Position
import com.shieldrone.station.data.State
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlightAutoControlVM : ViewModel() {
    private val TAG = "FlightAutoControlVM"
    // 1. 라이브데이터 및 필요한 필드
    private val flightControlModel = FlightAutoControlModel()
    private val handler = Handler(Looper.getMainLooper())

    // StateFlow 변수 선언
    private val _droneState = MutableStateFlow<State?>(null)
    val droneState: StateFlow<State?> get() = _droneState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> get() = _message.asStateFlow()

    private val _virtualMessage = MutableStateFlow<String?>(null)
    val virtualMessage: StateFlow<String?> get() = _virtualMessage.asStateFlow()

    private val _gpsSignalLevel = MutableStateFlow<Int?>(null)
    val gpsSignalLevel: StateFlow<Int?> get() = _gpsSignalLevel.asStateFlow()

    private val _targetPosition = MutableStateFlow<Position?>(null)
    val targetPosition: StateFlow<Position?> get() = _targetPosition.asStateFlow()

    var altitude: Int by mutableStateOf(0)
        private set
    var pitch: Int by mutableStateOf(0)
        private set

    // 2. 필요한 초기화
    init {
        flightControlModel.subscribeDroneGpsLevel { gpsLevel ->
            _gpsSignalLevel.value = gpsLevel
        }

        flightControlModel.subscribeDroneState { state ->
            _droneState.value = state
        }

    }

    // 3. 생명주기 관리

    // 생명주기 관리
    override fun onCleared() {
        Log.d(TAG, "[ViewModel] onCleared 시작")
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "[ViewModel] onCleared 성공")
    }

    // 4. 드론 이륙 및 착륙

    fun startTakeOff() {
        Log.d(TAG, "[ViewModel] startTakeOff 시작")
        flightControlModel.startTakeOff(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.value = "이륙이 시작되었습니다."
                Log.d(TAG, "[ViewModel] startTakeOff 성공")
                flightControlModel.monitorTakeoffStatus()
            }

            override fun onFailure(error: IDJIError) {
                _message.value = "이륙 실패: ${error.description()}"
                Log.d(TAG, "[ViewModel] startTakeOff 실패: ${error.description()}")
            }
        })
    }

    // 순항 고도로 상승
    fun ascendToCruiseAltitude(altitudeSpeed: Int, targetAltitude: Float = 3.2f) {
        Log.d(TAG, "[ViewModel] ascendToCruiseAltitude 시작")
        CoroutineScope(Dispatchers.Default).launch {
            try {
                adjustAltitudeCoroutine(altitudeSpeed, targetAltitude)
                Log.d(TAG, "[ViewModel] ascendToCruiseAltitude 성공")
            } catch (e: Exception) {
                Log.d(TAG, "[ViewModel] ascendToCruiseAltitude 실패: ${e.message}")
            }
        }
    }

    suspend fun adjustAltitudeCoroutine(altitudeSpeed: Int, targetAltitude: Float) {
        Log.d(TAG, "[ViewModel] adjustAltitudeCoroutine 시작")
        try {
            while (true) {
                val currentAltitude = _droneState.value!!.altitude!!
                if (currentAltitude >= targetAltitude) {
                    Log.d(TAG, "[ViewModel] adjustAltitudeCoroutine 성공: 목표 고도 도달")
                    break
                }

                val altitudeRatio = currentAltitude / targetAltitude
                val adjustmentSpeed = when {
                    altitudeRatio < 0.2 -> (altitudeSpeed * 0.3).toLong()
                    altitudeRatio < 0.8 -> altitudeSpeed
                    else -> (altitudeSpeed * 0.5).toLong()
                }

                flightControlModel.adjustAltitude(adjustmentSpeed.toInt())
                delay(100L)
            }
        } catch (e: Exception) {
            Log.d(TAG, "[ViewModel] adjustAltitudeCoroutine 실패: ${e.message}")
        }
    }

    fun startLanding() {
        Log.d(TAG, "[ViewModel] startLanding 시작")
        flightControlModel.startLanding(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.value = "착륙이 시작되었습니다."
                Log.d(TAG, "[ViewModel] startLanding 성공")
            }

            override fun onFailure(error: IDJIError) {
                _message.value = "착륙 실패: ${error.description()}"
                Log.d(TAG, "[ViewModel] startLanding 실패: ${error.description()}")
            }
        })
    }



    // 7. 가상 스틱 활성화 및 비 활성화 메서드
    /**
     * Virtual Stick 모드 활성화
     */
    fun enableVirtualStickMode() {
        Log.d(TAG, "[ViewModel] enableVirtualStickMode 시작")
        flightControlModel.enableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _virtualMessage.value = "VIRTUAL 활성화"
                Log.d(TAG, "[ViewModel] enableVirtualStickMode 성공")
            }

            override fun onFailure(error: IDJIError) {
                _message.value = "Virtual Stick 활성화 실패: ${error.description()}"
                Log.d(TAG, "[ViewModel] enableVirtualStickMode 실패: ${error.description()}")
            }
        })
    }

    fun disableVirtualStickMode() {
        Log.d(TAG, "[ViewModel] disableVirtualStickMode 시작")
        flightControlModel.disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.d(TAG, "[ViewModel] disableVirtualStickMode 성공")
            }

            override fun onFailure(error: IDJIError) {
                Log.d(TAG, "[ViewModel] disableVirtualStickMode 실패: ${error.description()}")
            }
        })
    }

    fun adjustYaw(yawDifference: Double) {
        Log.d(TAG, "[ViewModel] adjustYaw 시작")
        try {
            flightControlModel.adjustYaw(yawDifference)
//            Log.d(TAG, "[ViewModel] adjustYaw 성공")
        } catch (e: Exception) {
            Log.d(TAG, "[ViewModel] adjustYaw 실패: ${e.message}")
        }
    }

    fun setTargetPosition(position: Position) {
        Log.d(TAG, "[ViewModel] setTargetPosition 시작")
        _targetPosition.value = position
        Log.d(TAG, "[ViewModel] setTargetPosition 성공: position=$position")
    }

    fun updatePitch(value: Int) {
        Log.d(TAG, "[ViewModel] updatePitch 시작")
        pitch = value
        Log.d(TAG, "[ViewModel] updatePitch 성공: pitch=$value")
    }

    fun adjustPitch() {
        Log.d(TAG, "[ViewModel] adjustPitch 시작")
        try {
            flightControlModel.adjustPitch(pitch)
//            Log.d(TAG, "[ViewModel] adjustPitch 성공")
        } catch (e: Exception) {
            Log.d(TAG, "[ViewModel] adjustPitch 실패: ${e.message}")
        }
    }

    fun updateAltitude(value: Int) {
        Log.d(TAG, "[ViewModel] updateAltitude 시작")
        altitude = value
        Log.d(TAG, "[ViewModel] updateAltitude 성공: altitude=$value")
    }

    fun adjustAltitude() {
        Log.d(TAG, "[ViewModel] adjustAltitude 시작")
        try {
            flightControlModel.adjustAltitude(altitude)
//            Log.d(TAG, "[ViewModel] adjustAltitude 성공")
        } catch (e: Exception) {
            Log.d(TAG, "[ViewModel] adjustAltitude 실패: ${e.message}")
        }
    }
}
