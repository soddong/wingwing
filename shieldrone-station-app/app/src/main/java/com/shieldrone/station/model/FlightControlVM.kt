package com.shieldrone.station.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.shieldrone.station.model.FlightControlModel.Controls
import com.shieldrone.station.model.FlightControlModel.Position
import com.shieldrone.station.model.FlightControlModel.State
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

    private val _gpsSignalLevel = MutableLiveData<Int>()
    val gpsSignalLevel: LiveData<Int> get() = _gpsSignalLevel

    init {
        flightControlModel.initVirtualStickMode()
        flightControlModel.subscribeDroneGpsLevel { gpsLevel ->
            _gpsSignalLevel.postValue(gpsLevel)
        }
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
                flightControlModel.subscribeAndSendControlValues { controls ->
                    controls.leftStick.verticalPosition = 0
                    controls.leftStick.horizontalPosition = 0
                    controls.rightStick.verticalPosition = 0
                    controls.rightStick.horizontalPosition = 0
                    _droneControls.value = controls
                }
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

    fun moveForward() {
        flightControlModel.subscribeAndSendControlValues { controls ->
            controls.rightStick.verticalPosition = 100  // 전진
            _droneControls.value = controls  // UI 업데이트
        }
    }

    fun moveBackward() {
        flightControlModel.subscribeAndSendControlValues { controls ->
            controls.rightStick.verticalPosition = -100  // 후진
            _droneControls.value = controls
        }
    }

    fun moveLeft() {
        flightControlModel.subscribeAndSendControlValues { controls ->
            controls.rightStick.horizontalPosition = 100  // 좌측 이동
            _droneControls.value = controls
        }
    }

    fun moveRight() {
        flightControlModel.subscribeAndSendControlValues { controls ->
            controls.rightStick.horizontalPosition = -100  // 우측 이동
            _droneControls.value = controls
        }
    }

    fun moveUp() {
        flightControlModel.subscribeAndSendControlValues { controls ->
            controls.leftStick.verticalPosition = 100  // 상승
            _droneControls.value = controls
        }
    }

    fun moveDown() {
        flightControlModel.subscribeAndSendControlValues { controls ->
            controls.leftStick.verticalPosition = -100  // 하강
            _droneControls.value = controls
        }
    }

    fun rotateLeft() {
        flightControlModel.subscribeAndSendControlValues { controls ->
            controls.leftStick.horizontalPosition = -100  // 좌회전
            _droneControls.value = controls
        }
    }

    fun rotateRight() {
        flightControlModel.subscribeAndSendControlValues { controls ->
            controls.leftStick.horizontalPosition = 100  // 우회전
            _droneControls.value = controls
        }
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

    fun subscribeControlValues(onUpdate: (Controls) -> Unit) {
        flightControlModel.subscribeAndSendControlValues(onUpdate)
    }

    fun subscribeDroneGpsLevel() {
        flightControlModel.subscribeDroneGpsLevel { gpsLevel ->
            _gpsSignalLevel.postValue(gpsLevel)
        }
    }
}
