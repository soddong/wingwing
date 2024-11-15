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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlightAutoControlVM : ViewModel() {

    // 1. 라이브데이터 및 필요한 필드
    private val flightControlModel = FlightAutoControlModel()
    private val handler = Handler(Looper.getMainLooper())

    private var isMoving = false

    // StateFlow 변수 선언
    private val _droneState = MutableStateFlow<State?>(null)
    val droneState: StateFlow<State?> get() = _droneState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> get() = _message.asStateFlow()

    private val _virtualMessage = MutableStateFlow<String?>(null)
    val virtualMessage: StateFlow<String?> get() = _virtualMessage.asStateFlow()

    private val _droneControls = MutableStateFlow<Controls?>(null)
    val droneControls: StateFlow<Controls?> get() = _droneControls.asStateFlow()

    private val _gpsSignalLevel = MutableStateFlow<Int?>(null)
    val gpsSignalLevel: StateFlow<Int?> get() = _gpsSignalLevel.asStateFlow()

    private val _targetPosition = MutableStateFlow<Position?>(null)
    val targetPosition: StateFlow<Position?> get() = _targetPosition.asStateFlow()

    var altitude: Int by mutableStateOf(0)
        private set
    var pitch: Int by mutableStateOf(0)
        private set
    var maxYaw: Int by mutableStateOf(0)
        private set
    var maxStickValue: Int by mutableStateOf(0)
        private set
    var KpValue: Double by mutableStateOf(0.0)
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
        flightControlModel.adjustAltitude(altitude,3.2)
    }
}
