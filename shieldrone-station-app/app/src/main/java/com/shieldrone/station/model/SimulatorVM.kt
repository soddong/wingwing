package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.shieldrone.station.data.Controls
import com.shieldrone.station.data.Position
import com.shieldrone.station.data.State
import com.shieldrone.station.data.StickPosition
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.simulator.InitializationSettings

/**
 * @description: DJI 드론 시뮬레이터의 상태를 관리하는 ViewModel
 */
class SimulatorVM : ViewModel() {

    private val simulatorModel = SimulatorModel()
    private val handler = Handler(Looper.getMainLooper())

    // 드론 상태를 관찰하기 위한 LiveData
    private val _simulState = MutableLiveData<State>()
    val simulState: LiveData<State> get() = _simulState

    // 메시지나 이벤트를 관찰하기 위한 LiveData
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message

    // 드론 제어 상태를 관찰하기 위한 LiveData
    private val _simulControls = MutableLiveData<Controls>()
    val simulControls: LiveData<Controls> get() = _simulControls

    private val _simulPositions = MutableLiveData<Position>()
    val simulPositions: LiveData<Position> get() = _simulPositions

    // 드론 GPS 신호를 관찰하기 위한 LiveData
    private val _gpsSignalLevel = MutableLiveData<Int>()
    val gpsSignalLevel: LiveData<Int> get() = _gpsSignalLevel

    private val _targetLat = MutableLiveData<Double>()
    val targetLat: LiveData<Double> get() = _targetLat

    private val _targetLng = MutableLiveData<Double>()
    val targetLng: LiveData<Double> get() = _targetLng
    private var inputValue = 15

    init {

        // 드론 상태 구독 설정
        simulatorModel.subscribeDroneLocation { state ->
            _simulState.postValue(state)
        }

        // 드론 제어 상태 구독 설정
        simulatorModel.subscribeControlValues { controls ->
            _simulControls.postValue(controls)
        }
        simulatorModel.subscribePosition { position ->
            _simulPositions.postValue(position)
        }
    }



    // ViewModel이 해제될 때 호출되는 메서드로 리소스를 정리
    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }

    // 이륙 시작
    fun startTakeOff() {
        simulatorModel.startTakeOff(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.postValue("이륙이 시작되었습니다.")
                simulatorModel.monitorTakeoffStatus()
            }

            override fun onFailure(error: IDJIError) {
                _message.postValue("이륙 실패: ${error.description()}")
            }
        })
    }

    // 착륙 시작
    fun startLanding() {
        simulatorModel.startLanding(object : CommonCallbacks.CompletionCallback {
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
        simulatorModel.enableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
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
        simulatorModel.disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
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
        simulatorModel.setControlValues(controls, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                _message.postValue("드론 제어 값이 성공적으로 설정되었습니다.")
                _simulControls.postValue(controls)
            }

            override fun onFailure(error: IDJIError) {
                _message.postValue("드론 제어 설정 실패: ${error.description()}")
            }
        })
    }
//    // 드론을 앞으로 이동
//    fun moveForward() {
//        val controls = Controls(
//            leftStick = StickPosition(0, 0),
//            rightStick = StickPosition(inputValue, 0)
//        )
//        setDroneControlValues(controls)
//
//        // 일정 시간 후에 값을 초기화하여 정지
//        handler.postDelayed({
//            initVirtualStickValue()
//        }, 200) // 200ms 후 초기화 (시간 조정 가능)
//    }

    // 드론을 뒤로 이동
    fun moveBackward() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(-inputValue, 0)
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
            rightStick = StickPosition(0, -inputValue)
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
            rightStick = StickPosition(0, inputValue)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 200)
    }

    // 드론을 위로 상승
    fun moveUp() {
        val controls = Controls(
            leftStick = StickPosition(inputValue, 0),
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
            leftStick = StickPosition(-inputValue, 0),
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
            leftStick = StickPosition(0, -inputValue),
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
            leftStick = StickPosition(0, inputValue),
            rightStick = StickPosition(0, 0)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 200)
    }


    // 드론 위치 정보 구독 시작
    fun subscribeDroneLocation() {
        simulatorModel.subscribeDroneLocation { state ->
            _simulState.postValue(state)
        }
    }

    fun initVirtualStickValue() {
        simulatorModel.subscribeControlValues { controls: Controls ->
            controls.leftStick.verticalPosition = 0
            controls.leftStick.horizontalPosition = 0
            controls.rightStick.verticalPosition = 0
            controls.rightStick.horizontalPosition = 0
            _simulControls.value = controls

        }
    }

    fun enableSimulatorMode() {
        var lat = 37.5011
        var lng = 127.0388
        var simulatorGPSNum = 15
        val coordinate2D = LocationCoordinate2D(lat, lng)
        val data = InitializationSettings.createInstance(coordinate2D, simulatorGPSNum)
        simulatorModel.enableSimulator(data)

    }

    fun disableSimulatorMode() {
        simulatorModel.disableSimulator()
    }

    // 드론 제어 정보 구독 시작
    fun subscribeDroneControlValues() {
        simulatorModel.subscribeControlValues { control ->
            _simulControls.postValue(control)
        }
    }

    // 드론 위치 정보 구독 시작
    fun subscribeDronePositionValues() {
        simulatorModel.subscribePosition { position ->
            _simulPositions.postValue(position)
        }
    }

    /**
     * MoveToTarget 버튼 클릭 시 호출
     */
    fun moveToTarget() {
        val lat = _targetLat.value
        val lng = _targetLng.value
        if (lat != null && lng != null) {
            simulatorModel.moveToTarget(lat, lng)
            _message.postValue("목표 위치로 이동 중: 위도=$lat, 경도=$lng")
        } else {
            _message.postValue("목표 위치가 설정되지 않았습니다.")
        }
    }

    // 기타 필요한 메서드들
    fun adjustYawToTarget(targetBearing: Double) {
        val currentYaw = simulatorModel.getCurrentYaw()
        val yawDifference = simulatorModel.calculateYawDifference(targetBearing, currentYaw)
        simulatorModel.adjustYaw(yawDifference)
    }

    fun moveToForward() {
        simulatorModel.moveToForward()
    }
}
