package com.shieldrone.station.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.shieldrone.station.model.FlightControlModel.Controls
import com.shieldrone.station.model.FlightControlModel.Position
import com.shieldrone.station.model.FlightControlModel.State
import com.shieldrone.station.model.FlightControlModel.StickPosition
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

class FlightControlVM : ViewModel() {

    private val flightControlModel = FlightControlModel()

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

    // 드론 위치 정보 구독 시작
    fun subscribeDroneLocation() {
        flightControlModel.subscribeDroneLocation { state ->
            _droneState.postValue(state)
        }
    }

    // 드론을 앞으로 이동
    fun moveForward() {
        val controls = Controls(
            leftStick = StickPosition(0, 0), // leftStick: 유지
            rightStick = StickPosition(100, 0) // rightStick: 앞쪽으로 이동
        )
        setDroneControlValues(controls)
    }

    // 드론을 뒤로 이동
    fun moveBackward() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(-100, 0) // 뒤로 이동
        )
        setDroneControlValues(controls)
    }

    // 드론을 위로 상승
    fun moveUp() {
        val controls = Controls(
            leftStick = StickPosition(100, 0), // 상승
            rightStick = StickPosition(0, 0)
        )
        setDroneControlValues(controls)
    }

    // 드론을 아래로 하강
    fun moveDown() {
        val controls = Controls(
            leftStick = StickPosition(-100, 0), // 하강
            rightStick = StickPosition(0, 0)
        )
        setDroneControlValues(controls)
    }

    // 드론을 왼쪽으로 이동
    fun moveLeft() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(0, -100) // 왼쪽으로 이동
        )
        setDroneControlValues(controls)
    }

    // 드론을 오른쪽으로 이동
    fun moveRight() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(0, 100) // 오른쪽으로 이동
        )
        setDroneControlValues(controls)
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

    // 드론 위치 정보 구독 시작
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


}
