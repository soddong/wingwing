package com.shieldrone.station.model

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.shieldrone.station.constant.FlightContstant.Companion.BTN_DELAY
import com.shieldrone.station.constant.FlightContstant.Companion.INPUT_DEGREE
import com.shieldrone.station.constant.FlightContstant.Companion.INPUT_VELOCITY
import com.shieldrone.station.constant.FlightContstant.Companion.SIMULATOR_TAG
import com.shieldrone.station.data.Controls
import com.shieldrone.station.data.Position
import com.shieldrone.station.data.State
import com.shieldrone.station.data.StickPosition
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import java.util.LinkedList
import java.util.Queue

class FlightControlVM : ViewModel() {

    // 1. 라이브데이터 및 필요한 필드
    private val flightControlModel = FlightControlModel()
    private val handler = Handler(Looper.getMainLooper())

    private var isMoving = false

    private val _droneState = MutableLiveData<State>()
    val droneState: LiveData<State> get() = _droneState

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    private val _droneControls = MutableLiveData<Controls>()
    val droneControls: LiveData<Controls> get() = _droneControls


    private val _gpsSignalLevel = MutableLiveData<Int>()
    val gpsSignalLevel: LiveData<Int> get() = _gpsSignalLevel

    private val _targetPosition = MutableLiveData<Position>()
    val targetPosition: LiveData<Position> get() = _targetPosition

    private val _targetUser = MutableLiveData<TrackingData>()
    val targetUser: LiveData<TrackingData> get() = _targetUser

    // 2. 필요한 초기화
    init {
        flightControlModel.subscribeDroneGpsLevel { gpsLevel ->
            _gpsSignalLevel.postValue(gpsLevel)
        }

        flightControlModel.subscribeDroneState { state ->
            _droneState.postValue(state)
        }

        flightControlModel.subscribeControlValues { controls ->
            _droneControls.postValue(controls)
        }

    }

    fun initVirtualStickValue() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(0, 0)
        )
        setDroneControlValues(controls)
        _droneControls.value = controls
        Log.d(SIMULATOR_TAG, "Virtual Stick values initialized.")

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
                _message.postValue("이륙이 시작되었습니다.")
                flightControlModel.monitorTakeoffStatus()
            }

            override fun onFailure(error: IDJIError) {
                _message.postValue("이륙 실패: ${error.description()}")
            }
        })
    }

    // 착륙 시작
    fun startLanding() {
        flightControlModel.startLanding(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.postValue("착륙이 시작되었습니다.")
            }

            override fun onFailure(error: IDJIError) {
                _message.postValue("착륙 실패: ${error.description()}")
            }
        })
    }
    /**
     * 드론 제어값 설정
     */
    private fun setDroneControlValues(controls: Controls) {
        flightControlModel.setDroneControlValues(controls)
    }

    // 7. 가상 스틱 활성화 및 비 활성화 메서드
    /**
     * Virtual Stick 모드 활성화
     */
    fun enableVirtualStickMode() {
        flightControlModel.enableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.postValue("Virtual Stick 모드가 활성화되었습니다.")
            }

            override fun onFailure(error: IDJIError) {
                _message.postValue("Virtual Stick 활성화 실패: ${error.description()}")
            }
        })
    }

    /**
     * Virtual Stick 모드 비 활성화
     */
    fun disableVirtualStickMode() {
        flightControlModel.disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.postValue("Virtual Stick 모드가 비활성화되었습니다.")
            }

            override fun onFailure(error: IDJIError) {
                _message.postValue("Virtual Stick 비활성화 실패: ${error.description()}")
            }
        })
    }

    // 8. 타겟 설정, 타겟 이동

    /**
     * MoveToTarget 버튼 클릭 시 호출: 목표 위치로 이동
     */
    /**
     * Yaw 조정
     */
    fun adjustYawToTarget(targetBearing: Double) {
        val currentYaw = flightControlModel.getCurrentYaw()
        val yawDifference = flightControlModel.calculateYawDifference(targetBearing, currentYaw)
        flightControlModel.adjustYaw(yawDifference)
        _message.postValue("Yaw 조정 중: 목표 방위각=$targetBearing, 현재 Yaw=$currentYaw, 차이=$yawDifference")
    }

    fun calculateDistanceAndBearing(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Pair<Double, Double> {
        return flightControlModel.calculateDistanceAndBearing(startLat, startLng, endLat, endLng)
    }

    fun subscribeTargetPosition(position: Position) {
        _targetPosition.postValue(position)
    }
    fun subscribeTargetUser() {

    }

}
