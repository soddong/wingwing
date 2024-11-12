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

    // 1. 라이브데이터 및 필요한 필드
    private val flightControlModel = FlightControlModel()
    private val handler = Handler(Looper.getMainLooper())

    private val _droneState = MutableLiveData<State>()
    val droneState: LiveData<State> get() = _droneState

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    private val _droneControls = MutableLiveData<Controls>()
    val droneControls: LiveData<Controls> get() = _droneControls

    private val _dronePosition = MutableLiveData<Position>()
    val dronePosition: LiveData<Position> get() = _dronePosition

    private val _gpsSignalLevel = MutableLiveData<Int>()
    val gpsSignalLevel: LiveData<Int> get() = _gpsSignalLevel

    private val _targetLat = MutableLiveData<Double>()
    val targetLat: LiveData<Double> get() = _targetLat

    private val _targetLng = MutableLiveData<Double>()
    val targetLng: LiveData<Double> get() = _targetLng

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
    // 5. 드론 버튼 클릭해서 움직이는 메서드

    /**
     * 드론을 앞으로 이동
     */
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

    /**
     *  드론을 뒤로 이동
     */
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

    /**
     *  드론을 왼쪽으로 이동
     */
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

    /**
     * 드론을 오른쪽으로 이동
     */
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

    /**
     * 드론을 위로 상승
     */
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

    /**
     * 드론을 아래로 하강
     */
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

    /**
     * 드론을 왼쪽으로 회전
     */
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

    /**
     * 드론을 오른쪽으로 회전
     */
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

    /**
     * 드론 제어값 설정
     */
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

    // 6. 구독 메서드
    /**
     * 드론 상태 정보 구독 시작(State: attitude, velocity, location)
     */
    fun subscribeDroneState() {
        flightControlModel.subscribeDroneState { state ->
            _droneState.postValue(state)
        }
    }

    /**
     * 드론 제어 정보 구독 시작(Virtual Stick)
     */
    fun subscribeDroneControlValues() {
        flightControlModel.subscribeControlValues { control ->
            _droneControls.postValue(control)
        }
    }

    /**
     * 드론 위치 정보 구독 시작(Only Position)
     */
    fun subscribeDronePositionValues() {
        flightControlModel.subscribePosition { position ->
            _dronePosition.postValue(position)
        }
    }


    // 7. 가상 스틱 활성화 및 비 활성화 메서드
    /**
     * Virtual Stick 모드 활성화
     */
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
     * RouteAdapter로부터 받은 목표 위치 설정
     */
    fun setTargetLocation(lat: Double, lng: Double) {
        _targetLat.postValue(lat)
        _targetLng.postValue(lng)
    }

    /**
     * MoveToTarget 버튼 클릭 시 호출: 목표 위치로 이동
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
}
