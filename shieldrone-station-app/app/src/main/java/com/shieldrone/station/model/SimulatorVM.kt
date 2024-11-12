package com.shieldrone.station.model

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.shieldrone.station.constant.FlightContstant.Companion.INPUT_DEGREE
import com.shieldrone.station.constant.FlightContstant.Companion.INPUT_VELOCITY
import com.shieldrone.station.constant.FlightContstant.Companion.SIMULATOR_TAG
import com.shieldrone.station.data.Controls
import com.shieldrone.station.data.Position
import com.shieldrone.station.data.State
import com.shieldrone.station.data.StickPosition
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.simulator.InitializationSettings
import java.util.LinkedList
import java.util.Queue

/**
 * @description: DJI 드론 시뮬레이터의 상태를 관리하는 ViewModel
 */
class SimulatorVM : ViewModel() {

    private val simulatorModel = SimulatorModel()
    private val handler = Handler(Looper.getMainLooper())
    private val targetPositionQueue: Queue<Position> = LinkedList()

    private var isMoving = false

    // 드론 상태를 관찰하기 위한 LiveData
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

    private val _targetPosition = MutableLiveData<Position>()
    val targetPosition: LiveData<Position> get() = _targetPosition



    init {
        // targetPosition의 변경을 관찰

        simulatorModel.subscribeDroneGpsLevel { gpsLevel ->
            _gpsSignalLevel.postValue(gpsLevel)
        }

        simulatorModel.subscribeDroneState { state ->
            _droneState.postValue(state)
        }

        simulatorModel.subscribeControlValues { controls ->
            _droneControls.postValue(controls)
        }

//        simulatorModel.subscribePosition { position ->
//            _dronePosition.postValue(position)
//        }

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

    /**
     * ViewModel이 해제될 때 호출되는 메서드로 리소스를 정리
     */
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
                _message.postValue("이륙 실패: $error")
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
        }, 1000) // 1000ms 후 초기화 (시간 조정 가능)
    }

    /**
     * 드론을 뒤로 이동
     */
    fun moveBackward() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(-INPUT_VELOCITY, 0)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 1000)
    }

    /**
     * 드론을 왼쪽으로 이동
     */
    fun moveLeft() {
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(0, -INPUT_VELOCITY)
        )
        setDroneControlValues(controls)

        handler.postDelayed({
            initVirtualStickValue()
        }, 1000)
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
        }, 1000)
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
        }, 1000)
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
        }, 1000)
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
        }, 1000)
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
        }, 1000)
    }

    /**
     * 드론 제어값 설정
     */
    private fun setDroneControlValues(controls: Controls) {
        simulatorModel.setDroneControlValues(controls)
    }


    // 7. 가상 스틱 활성화 및 비 활성화 메서드
    /**
     * Virtual Stick 모드 활성화
     */
    fun enableVirtualStickMode() {
        simulatorModel.enableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
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
        simulatorModel.disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
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
     * Yaw 조정
     */
    fun adjustYawToTarget(targetBearing: Double) {
        val currentYaw = simulatorModel.getCurrentYaw()
        val yawDifference = simulatorModel.calculateYawDifference(targetBearing, currentYaw)
        simulatorModel.adjustYaw(yawDifference)
        _message.postValue("Yaw 조정 중: 목표 방위각=$targetBearing, 현재 Yaw=$currentYaw, 차이=$yawDifference")
    }

    fun calculateDistanceAndBearing(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Pair<Double, Double> {
        return simulatorModel.calculateDistanceAndBearing(startLat, startLng, endLat, endLng)
    }

    fun enableSimulatorMode() {
        val lat = 37.396959
        val lng = 127.0358512
        val simulatorGPSNum = 15
        val coordinate2D = LocationCoordinate2D(lat, lng)
        val data = InitializationSettings.createInstance(coordinate2D, simulatorGPSNum)
        simulatorModel.enableSimulator(data)
    }

    fun disableSimulatorMode() {
        simulatorModel.disableSimulator()
    }

    @SuppressLint("DefaultLocale")
    fun addTargetPosition(position: Position) {
        // 목표 위치를 큐에 추가
        targetPositionQueue.add(position)
        _message.postValue("새로운 목표 위치가 큐에 추가되었습니다. 현재 큐 크기: ${targetPositionQueue.size}")
        Log.d("QueueInfo", "새 목표 위치: 위도=${position.latitude}, 경도=${position.longitude}")

        // 이동 중이 아니라면 새 위치로 이동 시작
        if (!isMoving) {
            startNextTarget()

        }
    }
    // 큐의 다음 위치로 이동하는 메서드
    private fun startNextTarget() {
        if (targetPositionQueue.isNotEmpty()) {
            isMoving = true // 이동 시작
            Log.d("NextTarget", "isMoving : $isMoving")

            val nextPosition = targetPositionQueue.poll() // 큐에서 첫 번째 위치를 가져옴
            _targetPosition.postValue(nextPosition)
            if (nextPosition != null) {
                Log.d("DEBUG","next Position : $nextPosition")
                simulatorModel.moveToTarget(nextPosition) {
                    isMoving = false // 이동 완료 시 플래그 해제
                    // 이동 완료 후 큐에 남은 위치가 있으면 다음 위치로 이동
                    startNextTarget()
                }
            }
        }
    }
}
