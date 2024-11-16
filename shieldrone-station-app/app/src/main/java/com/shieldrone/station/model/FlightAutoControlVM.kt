package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
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

    /**
     * ViewModel이 해제될 때 호출되는 메서드로 리소스를 정리
     */
    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }

    // 4. 드론 이륙 및 착륙
    // 이륙 시작
    fun startTakeOff() {
        flightControlModel.startTakeOff(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.value = "이륙이 시작되었습니다."
                flightControlModel.monitorTakeoffStatus()
            }

            override fun onFailure(error: IDJIError) {
                _message.value = "이륙 실패: ${error.description()}"
            }
        })
    }

    // 순항 고도로 상승
    fun ascendToCruiseAltitude(
        altitudeSpeed: Int,
        targetAltitude: Float = 3.2f // 기본 목표 고도 (예: 3200m)
    ) {
        // 코루틴을 사용해 비동기로 드론 상승을 조정
        CoroutineScope(Dispatchers.Default).launch {
            adjustAltitudeCoroutine(altitudeSpeed, targetAltitude)
        }
    }

    suspend fun adjustAltitudeCoroutine(
        altitudeSpeed: Int,
        targetAltitude: Float
    ) {
        while (true) {
            // 현재 드론 고도를 가져옴
            val currentAltitude = _droneState.value!!.altitude!!

            // 목표 고도에 도달했는지 확인
            if (currentAltitude >= targetAltitude) {
                println("Reached cruise altitude.")
                break
            }

            // 목표 고도 대비 현재 고도의 비율을 계산 (0.0 ~ 1.0)
            val altitudeRatio = currentAltitude / targetAltitude

            // 상승 속도를 동적으로 조절
            val adjustmentSpeed = when {
                altitudeRatio < 0.2 -> {
                    // 목표 고도의 20% 미만에서는 천천히 상승
                    (altitudeSpeed * 0.3).toLong()
                }
                altitudeRatio < 0.8 -> {
                    // 목표 고도의 20% ~ 80% 구간에서는 최대 속도로 상승
                    altitudeSpeed
                }
                else -> {
                    // 목표 고도의 80% 이상에서는 속도를 줄여 부드럽게 도달
                    (altitudeSpeed * 0.5).toLong()
                }
            }

            // 고도 조정 (동기 함수 호출)
            flightControlModel.adjustAltitude(adjustmentSpeed.toInt())

            // 잠시 대기 (0.5초 대기 후 다시 확인)
            delay(100L)
        }
    }

    // 착륙 시작
    fun startLanding() {
        flightControlModel.startLanding(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.value = "착륙이 시작되었습니다."
            }

            override fun onFailure(error: IDJIError) {
                _message.value = "착륙 실패: ${error.description()}"
            }
        })
    }



    // 7. 가상 스틱 활성화 및 비 활성화 메서드
    /**
     * Virtual Stick 모드 활성화
     */
    fun enableVirtualStickMode() {
        flightControlModel.enableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
//                _message.value = "Virtual Stick 모드가 활성화되었습니다."
                _virtualMessage.value = "VIRTUAL 활성화"
            }

            override fun onFailure(error: IDJIError) {
                _message.value = "Virtual Stick 활성화 실패: ${error.description()}"
                
            }
        })
    }

    /**
     * Virtual Stick 모드 비 활성화
     */
    fun disableVirtualStickMode() {
        flightControlModel.disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
//                _message.value = "Virtual Stick 모드가 비활성화되었습니다."
                _virtualMessage.value = "VIRTUAL 비활성화"
                
            }

            override fun onFailure(error: IDJIError) {
                _message.value = "Virtual Stick 비활성화 실패: ${error.description()}"
            }
        })
    }

    fun adjustYaw(yawDifference: Double) {
        flightControlModel.adjustYaw(yawDifference)
    }
    fun setTargetPosition(position: Position) {
        _targetPosition.value = position
    }

    fun updatePitch(value: Int) {
        pitch = value
    }

    fun adjustPitch() {
        flightControlModel.adjustPitch(pitch)
    }
    fun updateAltitude(value: Int) {
        altitude = value
    }
    fun adjustAltitude() {
        flightControlModel.adjustAltitude(altitude)
    }
}
