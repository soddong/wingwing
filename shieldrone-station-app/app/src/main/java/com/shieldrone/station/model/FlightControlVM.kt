package com.shieldrone.station.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
}
