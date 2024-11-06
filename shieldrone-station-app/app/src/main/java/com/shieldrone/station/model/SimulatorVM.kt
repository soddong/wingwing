package com.shieldrone.station.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.v5.common.callback.CommonCallbacks
import dji.v5.manager.aircraft.simulator.InitializationSettings
import dji.v5.manager.aircraft.simulator.SimulatorManager
import dji.v5.manager.aircraft.simulator.SimulatorStatusListener

/**
 * @description: DJI 드론 시뮬레이터의 상태를 관리하는 ViewModel
 */
class SimulatorVM : ViewModel() {
    private val _altitude = MutableLiveData<Double>().apply { value = 0.0 } // 초기 고도 0.0m
    val altitude: LiveData<Double> get() = _altitude

    // 고도 업데이트 메서드
    fun updateAltitude(deltaZ: Double) {
        _altitude.value = deltaZ
    }

    // 시뮬레이터 상태를 저장하고 관찰할 수 있도록 하는 LiveData
    val simulatorStateSb = MutableLiveData(StringBuffer())

    // 시뮬레이터 상태 변경을 감지하는 리스너
    private val simulatorStateListener = SimulatorStatusListener { state ->
        simulatorStateSb.value?.apply {
            setLength(0) // 기존 상태 정보 초기화
            append("Motor On : " + state.areMotorsOn())
            append("\n")
            append("In the Air : " + state.isFlying)
            append("\n")
            append("Roll : " + state.roll)
            append("\n")
            append("Pitch : " + state.pitch)
            append("\n")
            append("Yaw : " + state.yaw)
            append("\n")
            append("PositionX : " + state.positionX)
            append("\n")
            append("PositionY : " + state.positionY)
            append("\n")
            append("PositionZ : " + state.positionZ)
            append("\n")
            append("Latitude : " + state.location.latitude)
            append("\n")
            append("Longitude : " + state.location.longitude)
            append("\n")
        }
        simulatorStateSb.postValue(simulatorStateSb.value) // 업데이트된 상태를 LiveData에 반영
    }

    init {
        addSimulatorListener() // 시뮬레이터 상태 리스너 추가
    }

    // 시뮬레이터 활성화 메서드S11P31A307-304
    //
    //
    //[BE] PC 서버 - 앱 서버간 GPS 위치 정보 타입 확인, 제어
    fun enableSimulator(
        initializationSettings: InitializationSettings,
        callback: CommonCallbacks.CompletionCallback
    ) {
        SimulatorManager.getInstance().enableSimulator(initializationSettings, callback)
    }

    // 시뮬레이터 비활성화 메서드
    fun disableSimulator(callback: CommonCallbacks.CompletionCallback) {
        SimulatorManager.getInstance().disableSimulator(callback)
        removeSimulatorListener() // ViewModel이 소멸될 때 리스너 제거
    }

    // 시뮬레이터 상태 리스너 추가 메서드
    private fun addSimulatorListener() {
        SimulatorManager.getInstance().addSimulatorStateListener(simulatorStateListener)
    }

    // 시뮬레이터 상태 리스너 제거 메서드
    private fun removeSimulatorListener() {
        SimulatorManager.getInstance().removeSimulatorStateListener(simulatorStateListener)
    }
}
