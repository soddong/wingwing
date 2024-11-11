package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.shieldrone.station.constant.FlightContstant.Companion.INPUT_DEGREE
import com.shieldrone.station.constant.FlightContstant.Companion.INPUT_VELOCITY
import com.shieldrone.station.data.Controls
import com.shieldrone.station.data.Position
import com.shieldrone.station.data.State
import com.shieldrone.station.data.StickPosition
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

class FlightControlVM : ViewModel() {

    private val flightControlModel = FlightControlModel()
    private val handler = Handler(Looper.getMainLooper())

    // 드론 상태를 관찰하기 위한 LiveData
    private val _droneState = MutableLiveData<State>()
    val droneState: LiveData<State> get() = _droneState

    // 메시지나 이벤트를 관찰하기 위한 LiveData
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    // 드론 제어 상태를 관찰하기 위한 LiveData
    private val _droneControls = MutableLiveData<Controls>()
    val droneControls: LiveData<Controls> get() = _droneControls

    // 드론 위치를 관찰하기 위한 LiveData
    private val _dronePosition = MutableLiveData<Position>()
    val dronePosition: LiveData<Position> get() = _dronePosition

    // 드론 GPS 신호를 관찰하기 위한 LiveData
    private val _gpsSignalLevel = MutableLiveData<Int>()
    val gpsSignalLevel: LiveData<Int> get() = _gpsSignalLevel

    private val _targetLat = MutableLiveData<Double>()
    val targetLat: LiveData<Double> get() = _targetLat

    private val _targetLng = MutableLiveData<Double>()
    val targetLng: LiveData<Double> get() = _targetLng
    init {
        // 드론 GPS 신호 수준을 구독하고 LiveData 업데이트
        flightControlModel.subscribeDroneGpsLevel { gpsLevel ->
            _gpsSignalLevel.postValue(gpsLevel)
        }

        // 드론 상태 구독 설정
        flightControlModel.subscribeDroneLocation { state ->
            _droneState.postValue(state)
        }

        // 드론 제어 상태 구독 설정
        flightControlModel.subscribeControlValues { controls ->
            _droneControls.postValue(controls)
        }

        // 드론 위치 구독 설정
        flightControlModel.subscribePosition { position ->
            _dronePosition.postValue(position)
        }
    }
    /**
     * RouteAdapter로부터 받은 목표 위치 설정
     */
    fun setTargetLocation(lat: Double, lng: Double) {
        _targetLat.postValue(lat)
        _targetLng.postValue(lng)
    }

    /**
     * MoveToTarget 버튼 클릭 시 호출
     */
    fun moveToTarget() {
        val lat = _targetLat.value
        val lng = _targetLng.value
        if (lat != null && lng != null) {
            flightControlModel.moveToTarget(lat, lng)
            _message.postValue("목표 위치로 이동 중: 위도=$lat, 경도=$lng")
        } else {
            _message.postValue("목표 위치가 설정되지 않았습니다.")
        }
    }
    // ViewModel이 해제될 때 호출되는 메서드로 리소스를 정리
    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }

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

    // Virtual Stick 모드 활성화
    fun enableVirtualStickMode() {
        flightControlModel.enableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                initVirtualStickValue()
                _message.postValue("Virtual Stick 모드가 활성화되었습니다.")
            }

            override fun onFailure(error: IDJIError) {
                _message.postValue("Virtual Stick 활성화 실패: ${error.description()}")
            }
        })
    }

    // Virtual Stick 모드 비활성화
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


    // 드론 제어 정보 구독 시작
    private fun setDroneControlValues(controls: Controls) {
        flightControlModel.setControlValues(controls, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.postValue("드론 제어 값이 성공적으로 설정되었습니다.")
                _droneControls.postValue(controls)
            }

            override fun onFailure(error: IDJIError) {
                _message.postValue("드론 제어 설정 실패: ${error.description()}")
            }
        })
    }
        // 드론을 앞으로 이동
    fun moveForward() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(INPUT_VELOCITY, 0)
        )
        setDroneControlValues(controls)

        // 일정 시간 후에 값을 초기화하여 정지
        handler.postDelayed({
            initVirtualStickValue()
        }, 200) // 200ms 후 초기화 (시간 조정 가능)
    }

    // 드론을 뒤로 이동
    fun moveBackward() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(-INPUT_VELOCITY, 0)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 200)
    }

    // 드론을 왼쪽으로 이동
    fun moveLeft() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(0, -INPUT_VELOCITY)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 200)
    }

    // 드론을 오른쪽으로 이동
    fun moveRight() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(0, INPUT_VELOCITY)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 200)
    }

    // 드론을 위로 상승
    fun moveUp() {
        val controls = Controls(
            leftStick = StickPosition(INPUT_VELOCITY, 0),
            rightStick = StickPosition(0, 0)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 200)
    }

    // 드론을 아래로 하강
    fun moveDown() {
        val controls = Controls(
            leftStick = StickPosition(-INPUT_VELOCITY, 0),
            rightStick = StickPosition(0, 0)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 200)
    }

    // 드론을 왼쪽으로 회전
    fun rotateLeft() {
        val controls = Controls(
            leftStick = StickPosition(0, -INPUT_DEGREE),
            rightStick = StickPosition(0, 0)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 200)
    }

    // 드론을 오른쪽으로 회전
    fun rotateRight() {
        val controls = Controls(
            leftStick = StickPosition(0, INPUT_DEGREE),
            rightStick = StickPosition(0, 0)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 200)
    }

    // 드론 위치 정보 구독 시작
    fun subscribeDroneLocation() {
        flightControlModel.subscribeDroneLocation { state ->
            _droneState.postValue(state)
        }
    }
    // 드론 제어 정보 구독 시작
    fun subscribeDroneControlValues() {
        flightControlModel.subscribeControlValues { control ->
            _droneControls.postValue(control)
        }
    }

    // 드론 위치 정보 구독 시작
    fun subscribeDronePositionValues() {
        flightControlModel.subscribePosition { position ->
            _dronePosition.postValue(position)
        }
    }
    fun initVirtualStickValue() {
        flightControlModel.subscribeControlValues { controls: Controls ->
            controls.leftStick.verticalPosition = 0
            controls.leftStick.horizontalPosition = 0
            controls.rightStick.verticalPosition = 0
            controls.rightStick.horizontalPosition = 0
            _droneControls.value = controls

        }
    }


    /**
     * Yaw 조정
     */
    fun adjustYawToTarget(targetBearing: Double) {
        val currentYaw = flightControlModel.getCurrentYaw()
        val yawDifference = flightControlModel.calculateYawDifference(targetBearing, currentYaw)
        flightControlModel.adjustYaw(yawDifference)
        _message.postValue("Yaw 조정 중: 목표 방위각=$targetBearing, 현재 Yaw=$currentYaw, 차이=$yawDifference")
    }

    fun moveToForward() {
        flightControlModel.moveToForward()
    }
}
