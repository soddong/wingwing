package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.shieldrone.station.constant.FlightContstant.Companion.MAX_DEGREE
import com.shieldrone.station.constant.FlightContstant.Companion.MAX_STICK_VALUE
import com.shieldrone.station.constant.FlightContstant.Companion.SIMULATOR_TAG
import com.shieldrone.station.constant.FlightContstant.Companion.VIRTUAL_STICK_TAG
import com.shieldrone.station.data.Controls
import com.shieldrone.station.data.Position
import com.shieldrone.station.data.State
import com.shieldrone.station.data.StickPosition
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.ErrorType
import dji.v5.common.error.IDJIError
import dji.v5.et.action
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.simulator.InitializationSettings
import dji.v5.manager.aircraft.simulator.SimulatorManager
import dji.v5.manager.aircraft.simulator.SimulatorStatusListener
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SimulatorModel {
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        val keyConnection by lazy { KeyTools.createKey(FlightControllerKey.KeyConnection) }
        val keyIsFlying by lazy { KeyTools.createKey(FlightControllerKey.KeyIsFlying) }
        val keyStartTakeoff by lazy { KeyTools.createKey(FlightControllerKey.KeyStartTakeoff) }
        val keyStartAutoLanding by lazy { KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding) }
        val virtualStickManager by lazy { VirtualStickManager.getInstance() }
        val location3D by lazy { KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D) }
        val velocity3D by lazy { KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity) }
        val compassHeading by lazy { KeyTools.createKey(FlightControllerKey.KeyCompassHeading) }
        val keyGPSSignalLevel by lazy { KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel) }

        val simulatorManager by lazy { SimulatorManager.getInstance() }

    }

    /**
     * 이륙 시작 함수
     */
    fun startTakeOff(callback: CommonCallbacks.CompletionCallback) {
        if (!checkPreconditionsForTakeoff()) {
            callback.onFailure(object : IDJIError {
                override fun errorType() = ErrorType.UNKNOWN
                override fun errorCode() = "TAKE_OFF_FAILED"
                override fun innerCode() = "TAKE_OFF_FAILED"
                override fun hint() = "이륙에 실패했습니다."
                override fun description() = "이륙에 실패했습니다. 이륙 조건을 확인해보세요."
                override fun isError(p0: String?) = true

            })
            return
        }

        KeyManager.getInstance().run {
            keyStartTakeoff
                .action({
                    // 이륙 성공 시 콜백 호출
                    callback.onSuccess()
                }, { e: IDJIError ->
                    callback.onFailure(e)
                })
        }
    }

    /**
     * 착륙 시작 함수
     */
    fun startLanding(callback: CommonCallbacks.CompletionCallback) {
        KeyManager.getInstance().run {
            keyStartAutoLanding
                .action({
                    onDestroy()
                    callback.onSuccess()
                }, { e: IDJIError ->
                    callback.onFailure(e)
                })
        }
    }

    /**
     * 이륙 전 조건 검사
     */
    private fun checkPreconditionsForTakeoff(): Boolean {
        val isConnected = KeyManager.getInstance().getValue(keyConnection) ?: false
        val isFlying = KeyManager.getInstance().getValue(keyIsFlying) ?: false
        return isConnected && !isFlying
    }

    /**
     * 이륙 상태 모니터링
     */
    fun monitorTakeoffStatus() {
        KeyManager.getInstance().listen(keyIsFlying, this) { _, isFlying ->
            if (isFlying == true) {
                Log.d(SIMULATOR_TAG, "Drone is now flying")

            }
        }
    }

    /**
     * Virtual Stick 모드 활성화
     */
    fun enableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        virtualStickManager.enableVirtualStick(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.d(SIMULATOR_TAG, "Virtual Stick 모드가 활성화되었습니다.")
                callback.onSuccess()
            }

            override fun onFailure(error: IDJIError) {
                Log.d(SIMULATOR_TAG, "Virtual Stick 활성화 실패: ${error.description()}")

                callback.onFailure(error)
            }
        })
    }

    /**
     * Virtual Stick 모드 비활성화
     */
    fun disableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        virtualStickManager.disableVirtualStick(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.d(VIRTUAL_STICK_TAG, "Virtual Stick 모드가 비활성화되었습니다.")
                callback.onSuccess()
            }

            override fun onFailure(error: IDJIError) {
                Log.d(VIRTUAL_STICK_TAG, "Virtual Stick 비활성화 실패: ${error.description()}")
                callback.onFailure(error)
            }
        })
    }

    /**
     * 드론 위치 정보 구독
     */
    fun subscribeDroneLocation(onUpdate: (State) -> Unit) {
        val state = State()
        state.xVelocity = velocity3D.get()?.x
        state.yVelocity = velocity3D.get()?.y
        state.zVelocity = velocity3D.get()?.z
        state.compassHeading = compassHeading.get()
        val simulatorListener = SimulatorStatusListener { simulatorState ->
            state.roll = simulatorState.roll.toDouble()
            state.pitch = simulatorState.pitch.toDouble()
            state.yaw = simulatorState.yaw.toDouble()
            onUpdate(state)
            Log.d(
                SIMULATOR_TAG, "Roll: ${state.roll}, Pitch: ${state.pitch}, Yaw: ${state.yaw}, " +
                        "Longitude: ${state.xVelocity}, Latitude: ${state.yVelocity}, Altitude: ${state.zVelocity}"
            )
        }
        simulatorManager.addSimulatorStateListener(simulatorListener)

        KeyManager.getInstance().listen(velocity3D, this) { _, data ->
            data?.let {
                state.xVelocity = it.x
                state.yVelocity = it.y
                state.zVelocity = it.z
                onUpdate(state)
            }
        }

        KeyManager.getInstance().listen(compassHeading, this) { _, heading ->
            heading?.let {
                state.compassHeading = it
                onUpdate(state)
            }
        }
    }

    /**
     * 드론의 leftStick과 rightStick 위치 값을 설정하여 제어하는 함수
     */
    fun setControlValues(controls: Controls, callback: CommonCallbacks.CompletionCallback) {

        val stickManager = virtualStickManager

        stickManager.leftStick.verticalPosition = controls.leftStick.verticalPosition
        stickManager.leftStick.horizontalPosition = controls.leftStick.horizontalPosition
        stickManager.rightStick.verticalPosition = controls.rightStick.verticalPosition
        stickManager.rightStick.horizontalPosition = controls.rightStick.horizontalPosition

        handler.post {
            callback.onSuccess()
        }

        Log.d(
            VIRTUAL_STICK_TAG,
            "leftStick (vertical-고도: ${controls.leftStick.verticalPosition}, horizontal-좌우회전: ${controls.leftStick.horizontalPosition}), " +
                    "rightStick (vertical-앞뒤: ${controls.rightStick.verticalPosition}, horizontal-좌우이동: ${controls.rightStick.horizontalPosition})"
        )
    }

    /**
     * 드론의 control 값을 구독하고 지속적으로 업데이트하여 제어하는 함수
     */
    fun subscribeControlValues(onUpdate: (Controls) -> Unit) {

        val stickManager = virtualStickManager

        // 초기 control 값을 설정
        var currentControls = Controls(
            StickPosition(
                stickManager.leftStick.verticalPosition,
                stickManager.leftStick.horizontalPosition
            ),
            StickPosition(
                stickManager.rightStick.verticalPosition,
                stickManager.rightStick.horizontalPosition
            )
        )

        // 일정 간격으로 control 값을 갱신하고 콜백 호출
        handler.post(object : Runnable {
            override fun run() {
                // 새로운 Controls 상태를 구독
                val newControls = Controls(
                    StickPosition(
                        stickManager.leftStick.verticalPosition,
                        stickManager.leftStick.horizontalPosition
                    ),
                    StickPosition(
                        stickManager.rightStick.verticalPosition,
                        stickManager.rightStick.horizontalPosition
                    )
                )

                // 새로운 control 값을 onUpdate 콜백으로 전달
                onUpdate(newControls)

                // 이전 상태와 새로운 상태 비교 후 변경이 있을 때만 적용
                if (currentControls != newControls) {
                    stickManager.leftStick.verticalPosition = newControls.leftStick.verticalPosition
                    stickManager.leftStick.horizontalPosition =
                        newControls.leftStick.horizontalPosition
                    stickManager.rightStick.verticalPosition =
                        newControls.rightStick.verticalPosition
                    stickManager.rightStick.horizontalPosition =
                        newControls.rightStick.horizontalPosition
                    Log.d(
                        VIRTUAL_STICK_TAG,
                        "Control updated: leftStick(vertical: ${newControls.leftStick.verticalPosition}, horizontal: ${newControls.leftStick.horizontalPosition}), " +
                                "rightStick(vertical: ${newControls.rightStick.verticalPosition}, horizontal: ${newControls.rightStick.horizontalPosition})"
                    )

                    // 현재 상태를 업데이트
                    currentControls = newControls
                }

                // 구독을 지속적으로 수행
                handler.postDelayed(this, 100) // 100ms 주기로 업데이트
            }
        })
    }

    /**
     * 드론의 위치 정보 (altitude, latitude, longitude)를 구독하여 지속적으로 업데이트하는 함수
     */
    fun subscribePosition(onUpdate: (Position) -> Unit) {
        // 초기 위치 설정
        var currentPosition = Position(
            altitude = location3D.get()?.altitude ?: 0.0,
            latitude = location3D.get()?.latitude ?: 0.0,
            longitude = location3D.get()?.longitude ?: 0.0
        )

        // 위치 정보를 일정 간격으로 업데이트
        handler.post(object : Runnable {
            override fun run() {
                // 새로운 위치 정보를 가져옴
                val newAltitude = location3D.get()?.altitude ?: 0.0
                val newLatitude = location3D.get()?.latitude ?: 0.0
                val newLongitude = location3D.get()?.longitude ?: 0.0

                // 새로운 위치 정보로 Position 객체 생성
                val newPosition = Position(newAltitude, newLatitude, newLongitude)

                // 위치 값이 변경된 경우에만 업데이트
                if (currentPosition != newPosition) {
                    currentPosition = newPosition
                    onUpdate(newPosition)
                    Log.d(
                        SIMULATOR_TAG,
                        "Position updated: altitude = $newAltitude, latitude = $newLatitude, longitude = $newLongitude"
                    )
                }

                // 100ms 주기로 위치 정보를 업데이트
                handler.postDelayed(this, 100)
            }
        })
    }

    fun subscribeDroneGpsLevel(onUpdate: (Int) -> Unit) {
        var currentGPSLevel =
            KeyManager.getInstance().getValue(keyGPSSignalLevel)?.value() ?: 0
        Log.d(SIMULATOR_TAG, "current GPS Level: $currentGPSLevel")
        handler.post(object : Runnable {
            override fun run() {
                // 새로운 GPS 신호 레벨 가져오기
                val newGPSLevel =
                    KeyManager.getInstance().getValue(keyGPSSignalLevel)?.value()
                        ?: 0

                // GPS 신호 레벨이 변경되었을 때만 업데이트
                if (currentGPSLevel != newGPSLevel) {
                    currentGPSLevel = newGPSLevel
                    onUpdate(newGPSLevel)
                    Log.d(SIMULATOR_TAG, "GPS Signal Level updated: $newGPSLevel")
                }

                // 100ms 마다 반복
                handler.postDelayed(this, 100)
            }
        })
    }


    fun enableSimulator(initializationSettings: InitializationSettings) {
        simulatorManager.enableSimulator(
            initializationSettings,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.d(SIMULATOR_TAG, "SimulatorMode 연결 성공")
                }

                override fun onFailure(p0: IDJIError) {
                    Log.d(SIMULATOR_TAG, "SimulatorMode 연결 실패 ${p0.description()}")

                }
            })
    }

    fun disableSimulator() {
        simulatorManager.disableSimulator(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.d(SIMULATOR_TAG, "SimulatorMode 비 활성화 성공 ")

            }

            override fun onFailure(p0: IDJIError) {
                Log.d(SIMULATOR_TAG, "SimulatorMode 비 활성화 실패 ${p0.description()}")

            }

        })
    }

    /**
     * 리소스 해제 및 메모리 누수 방지 메서드
     */
    private fun onDestroy() {
        // 핸들러의 모든 콜백 제거
        handler.removeCallbacksAndMessages(null)
        // KeyManager의 모든 구독 제거
        KeyManager.getInstance().cancelListen(keyConnection)
        KeyManager.getInstance().cancelListen(keyIsFlying)
        KeyManager.getInstance().cancelListen(location3D)
        KeyManager.getInstance().cancelListen(velocity3D)
        KeyManager.getInstance().cancelListen(compassHeading)
        KeyManager.getInstance().cancelListen(keyGPSSignalLevel)

        disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.d(VIRTUAL_STICK_TAG, "Virtual Stick 모드가 비활성화되었습니다.")
            }

            override fun onFailure(error: IDJIError) {
                Log.e(
                    VIRTUAL_STICK_TAG,
                    "Virtual Stick 모드 비활성화 실패: ${error.description()}"
                )
            }
        })
        Log.d(SIMULATOR_TAG, "SimulatorModel의 리소스가 해제되었습니다.")
    }

    // 드론의 현재 위치를 가져오는 메서드
    fun getCurrentDronePosition(): Position {
        val locationKey = KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D)
        val location = KeyManager.getInstance().getValue(locationKey)
        return if (location != null) {
            Position(location.latitude, location.longitude, location.altitude)
        } else {
            Position(0.0, 0.0, 0.0)
        }
    }

    // 드론의 현재 Yaw 값을 가져오는 메서드
    fun getCurrentYaw(): Double {
        val yawKey = KeyTools.createKey(FlightControllerKey.KeyCompassHeading)
        val yaw = KeyManager.getInstance().getValue(yawKey) ?: 0.0
        return (yaw + MAX_DEGREE) % MAX_DEGREE // 0~360도 사이로 변환
    }

    // 두 지점 간의 거리와 방위각 계산
    fun calculateDistanceAndBearing(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Pair<Double, Double> {
        val earthRadius = 6371000.0 // 지구 반지름 (미터 단위)

        val dLat = Math.toRadians(endLat - startLat)
        val dLng = Math.toRadians(endLng - startLng)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
                sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distance = earthRadius * c // 거리 (미터 단위)

        // 방위각 계산
        val y = sin(dLng) * cos(Math.toRadians(endLat))
        val x = cos(Math.toRadians(startLat)) * sin(Math.toRadians(endLat)) -
                sin(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) * cos(dLng)
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + MAX_DEGREE) % MAX_DEGREE // 방위각을 0~360도로 변환

        return Pair(distance, bearing)
    }

    // 목표 방위각과 현재 Yaw 값의 차이 계산
    fun calculateYawDifference(targetBearing: Double, currentYaw: Double): Double {
        val difference = ((targetBearing - currentYaw + 540) % 360) - 180
        return difference
    }

    // 드론의 Yaw 값을 조정하여 방향 전환
    fun adjustYaw(yawDifference: Double) {
        val maxYawSpeed = 100.0 // 드론의 최대 Yaw 속도 (deg/s)
        val yawInput = (yawDifference / maxYawSpeed).coerceIn(-1.0, 1.0) * MAX_STICK_VALUE

        val controls = Controls(
            leftStick = StickPosition(0, yawInput.toInt()), // Yaw 제어
            rightStick = StickPosition(0, 0)
        )
        setDroneControlValues(controls)

        // 일정 시간 후 Yaw 입력 값 초기화
        handler.postDelayed({
            initVirtualStickValue()
        }, 500) // 500ms 후 초기화
    }

    // 드론을 전진시키는 메서드
    fun moveToForward() {
        val pitchInput = 0.5 * MAX_STICK_VALUE // 전진 속도 설정 (-660 ~ +660 범위)
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(pitchInput.toInt(), 0)
        )
        setDroneControlValues(controls)

        // 일정 시간 후 입력 값 초기화
        handler.postDelayed({
            initVirtualStickValue()
        }, 500) // 500ms 후 초기화
    }

    // 드론을 목표 위치로 이동시키는 메서드
    fun moveToTarget(targetLat: Double, targetLng: Double) {
        val checkInterval = 1000L // 1초마다 위치 확인
        handler.post(object : Runnable {
            override fun run() {
                val currentPosition = getCurrentDronePosition()
                val currentYaw = getCurrentYaw()
                val (distance, targetBearing) = calculateDistanceAndBearing(
                    currentPosition.latitude, currentPosition.longitude,
                    targetLat, targetLng
                )
                val yawDifference = calculateYawDifference(targetBearing, currentYaw)

                if (abs(yawDifference) > 5) {
                    // 드론의 방향을 조정
                    adjustYaw(yawDifference)
                } else if (distance > 1.0) {
                    // 드론을 전진시킴
                    moveToForward()
                } else {
                    // 목표 지점에 도달하면 정지
                    initVirtualStickValue()
                    Log.d(SIMULATOR_TAG, "목표 지점에 도달했습니다.")
                    return
                }

                // 다음 체크를 위해 다시 호출
                handler.postDelayed(this, checkInterval)
            }
        })
    }

    // 드론 제어 값을 설정하는 메서드
    private fun setDroneControlValues(controls: Controls) {
        virtualStickManager.leftStick.verticalPosition =
            controls.leftStick.verticalPosition.toDouble().toInt()
        virtualStickManager.leftStick.horizontalPosition =
            controls.leftStick.horizontalPosition.toDouble().toInt()
        virtualStickManager.rightStick.verticalPosition =
            controls.rightStick.verticalPosition.toDouble().toInt()
        virtualStickManager.rightStick.horizontalPosition =
            controls.rightStick.horizontalPosition.toDouble().toInt()

        Log.d(SIMULATOR_TAG, "Control values set: $controls")
    }

    // Virtual Stick 입력 값을 초기화하는 메서드
    fun initVirtualStickValue() {
        setDroneControlValues(
            Controls(
                leftStick = StickPosition(0, 0),
                rightStick = StickPosition(0, 0)
            )
        )
        Log.d(SIMULATOR_TAG, "Virtual Stick values initialized.")
    }

}