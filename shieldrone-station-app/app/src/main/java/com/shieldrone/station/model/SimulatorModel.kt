package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.shieldrone.station.constant.FlightContstant.Companion.ANGLE_THRESHOLD
import com.shieldrone.station.constant.FlightContstant.Companion.EARTH_RADIUS
import com.shieldrone.station.constant.FlightContstant.Companion.LANDING_DELAY_MILLISECONDS
import com.shieldrone.station.constant.FlightContstant.Companion.MAX_STICK_VALUE
import com.shieldrone.station.constant.FlightContstant.Companion.SIMULATOR_TAG
import com.shieldrone.station.constant.FlightContstant.Companion.VIRTUAL_STICK_TAG
import com.shieldrone.station.data.Controls
import com.shieldrone.station.data.Position
import com.shieldrone.station.data.State
import com.shieldrone.station.data.StickPosition
import com.shieldrone.station.model.FlightControlModel.Companion.attitude
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
import dji.v5.manager.interfaces.IVirtualStickManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SimulatorModel {

    // 1. field, companion object
    val isVirtualStickEnabled = false
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        val keyConnection by lazy { KeyTools.createKey(FlightControllerKey.KeyConnection) }
        val keyIsFlying by lazy { KeyTools.createKey(FlightControllerKey.KeyIsFlying) }
        val keyStartTakeoff by lazy { KeyTools.createKey(FlightControllerKey.KeyStartTakeoff) }
        val keyStartAutoLanding by lazy { KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding) }
        val location3D by lazy { KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D) }
        val velocity3D by lazy { KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity) }
        val compassHeading by lazy { KeyTools.createKey(FlightControllerKey.KeyCompassHeading) }
        val keyGPSSignalLevel by lazy { KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel) }
        val location2D by lazy { KeyTools.createKey(FlightControllerKey.KeyAircraftLocation) }
        val virtualStickManager by lazy { VirtualStickManager.getInstance() }
        val simulatorManager by lazy { SimulatorManager.getInstance() }

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
    // 2. LifeCycle

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
        KeyManager.getInstance().cancelListen(attitude)
        KeyManager.getInstance().cancelListen(location2D)

        KeyManager.getInstance().cancelListen(keyStartTakeoff)
        KeyManager.getInstance().cancelListen(keyStartAutoLanding)
        // Virtual Stick 모드가 활성화되어 있다면 비활성화
        if (isVirtualStickEnabled) {
            disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.d(VIRTUAL_STICK_TAG, "Virtual Stick 모드가 비활성화되었습니다.")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(VIRTUAL_STICK_TAG, "Virtual Stick 모드 비활성화 실패: ${error.description()}")
                }
            })
        }

        disableSimulator()

        Log.d(SIMULATOR_TAG, "SimulatorModel의 리소스가 해제되었습니다.")
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
            keyStartTakeoff.action({
                // 이륙 성공 시 Yaw 조정
                adjustYawToNorth(callback)
            }, { e: IDJIError ->
                callback.onFailure(e)
            })
        }
    }

    /**
     * 착륙 시작 함수
     */
    fun startLanding(callback: CommonCallbacks.CompletionCallback, retryCount: Int = 3) {
        if (retryCount <= 0) {
            callback.onFailure(object : IDJIError {
                override fun errorType() = ErrorType.UNKNOWN
                override fun errorCode() = "LANDING_FAILED"
                override fun innerCode() = "LANDING_FAILED"
                override fun hint() = "착륙 시도 횟수를 초과했습니다."
                override fun description() = "착륙에 반복 실패하였습니다."
                override fun isError(p0: String?) = true
            })
            return
        }
        KeyManager.getInstance().run {
            keyStartAutoLanding
                .action({
                    onDestroy()
                    callback.onSuccess()
                }, { e: IDJIError ->
                    handler.postDelayed({
                        startLanding(callback, retryCount - 1)
                    }, LANDING_DELAY_MILLISECONDS.toLong())
                    callback.onFailure(e)
                })
        }
    }


    /**
     * 이륙 전 조건 검사
     */
    private fun checkPreconditionsForTakeoff(): Boolean {
        val isConnected = KeyManager.getInstance()
            .getValue(keyConnection) ?: false
        val isFlying = KeyManager.getInstance()
            .getValue(keyIsFlying) ?: false
        Log.d(
            SIMULATOR_TAG,
            "Preconditions for takeoff - isConnected: $isConnected, isFlying: $isFlying"
        )

        return isConnected && !isFlying
    }

    /**
     * 이륙 상태 모니터링
     */
    fun monitorTakeoffStatus() {
        KeyManager.getInstance().listen(
            keyIsFlying,
            this
        ) { _, isFlying ->
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
    fun subscribeDroneState(onUpdate: (State) -> Unit) {
        val state = State()
        // 초기 값 설정
        state.latitude = location2D.get()?.latitude
        state.longitude = location2D.get()?.longitude
        state.pitch = attitude.get()?.pitch
        state.roll = attitude.get()?.roll
        state.yaw = attitude.get()?.yaw
        state.xVelocity = velocity3D.get()?.x
        state.yVelocity = velocity3D.get()?.y
        state.zVelocity = velocity3D.get()?.z
        state.compassHeading = compassHeading.get()

        val simulatorListener = SimulatorStatusListener { simulatorState ->
            // Roll, Pitch, Yaw의 변화량 계산
            val rollDelta = abs((state.roll ?: 0.0) - simulatorState.roll.toDouble())
            val pitchDelta = abs((state.pitch ?: 0.0) - simulatorState.pitch.toDouble())
            val yawDelta = abs((state.yaw ?: 0.0) - simulatorState.yaw.toDouble())

            // 오차 범위 초과 여부 확인
            if (rollDelta >= ANGLE_THRESHOLD || pitchDelta >= ANGLE_THRESHOLD || yawDelta >= ANGLE_THRESHOLD) {
                state.roll = simulatorState.roll.toDouble()
                state.pitch = simulatorState.pitch.toDouble()
                state.yaw = simulatorState.yaw.toDouble()
                onUpdate(state)
//                Log.d(
//                    SIMULATOR_TAG,
//                    "Roll: ${state.roll}, Pitch: ${state.pitch}, Yaw: ${state.yaw}"
//                )
            }
        }
        simulatorManager.addSimulatorStateListener(simulatorListener)
//        KeyManager.getInstance().listen(attitude, this) { _, data ->
//            data?.let {
//                state.yaw = it.yaw
//                state.roll = it.roll
//                state.pitch = it.pitch
//                onUpdate(state)
//            }
//        }
        KeyManager.getInstance()
            .listen(velocity3D, this) { _, data ->
                data?.let {
                    state.xVelocity = it.x
                    state.yVelocity = it.y
                    state.zVelocity = it.z
                    onUpdate(state)
                }
            }
//
        KeyManager.getInstance()
            .listen(location3D, this) { _, data ->
                data?.let {
//                    state.latitude = it.latitude
//                    state.longitude = it.longitude
//                    state.altitude = it.altitude
                    onUpdate(state)
//                    Log.d(
//                        SIMULATOR_TAG,
//                        "Location updated: lat=${it.latitude}, lng=${it.longitude}, alt=${it.altitude}"
//                    )
                }
            }
        KeyManager.getInstance()
            .listen(location2D,this) {_, data ->
                data?.let {
                    state.latitude = it.latitude
                    state.longitude = it.longitude
                    onUpdate(state)
//                    Log.d(
//                        SIMULATOR_TAG,
//                        "Location updated: lat=${it.latitude}, lng=${it.longitude}"
//                    )
                }
            }
//        KeyManager.getInstance().listen(
//            compassHeading,
//            this
//        ) { _, heading ->
//            heading?.let {
//                state.compassHeading = it
//                onUpdate(state)
//            }
//        }
    }

    /**
     * 드론의 control 값을 구독하고 지속적으로 업데이트하여 제어하는 함수
     */
    fun subscribeControlValues(onUpdate: (Controls) -> Unit) {
        if (!isVirtualStickEnabled) {
            Log.e(VIRTUAL_STICK_TAG, "Virtual Stick 모드가 활성화되지 않았습니다.")
            return
        }

        val stickManager = virtualStickManager

        // 초기 control 값을 설정
        var currentControls = getCurrentStickPositions(stickManager)

        // 일정 간격으로 control 값을 갱신하고 콜백 호출
        handler.post(object : Runnable {
            override fun run() {
                // 새로운 Controls 상태를 구독
                val newControls = getCurrentStickPositions(stickManager)

                // 새로운 control 값을 onUpdate 콜백으로 전달
                onUpdate(newControls)

                // 이전 상태와 새로운 상태 비교 후 변경이 있을 때만 적용
                if (currentControls != newControls) {
                    updateStickPositions(stickManager, newControls)
                }

                // 구독을 지속적으로 수행
                handler.postDelayed(this, 1000) // 1000ms 주기로 업데이트
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
                handler.postDelayed(this, 1000)
            }
        })
    }


    /**
     * Control 값을 설정하는 메서드
     */
    fun setDroneControlValues(controls: Controls) {
        virtualStickManager?.let { stickManager ->
            applyControlValues(stickManager, controls, SIMULATOR_TAG)
        } ?: Log.e(SIMULATOR_TAG, "Virtual Stick Manager가 null입니다.")
    }

    /**
     * 드론을 전진시키는 메서드 (Pitch 값 조정)
     */
    fun moveToForward() {
        val pitch = 60 // 적절한 전진 속도 값 설정 (범위: -660 ~ 660)
        val controls = Controls(
            leftStick = StickPosition(0, 0),
            rightStick = StickPosition(pitch, 0)
        )
        setDroneControlValues(controls)
        Log.d(SIMULATOR_TAG, "Moving forward with pitchAngle: $pitch")
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
                simulatorManager.clearAllSimulatorStateListener()
            }

            override fun onFailure(p0: IDJIError) {
                Log.d(SIMULATOR_TAG, "SimulatorMode 비 활성화 실패 ${p0.description()}")

            }

        })
    }


    // 6. State, Location Helper
    /**
     * 드론의 현재 위치를 가져오는 메서드
     *
     */
    fun getCurrentDronePosition(): Position {

        val location = KeyManager.getInstance().getValue(location3D)

        Log.d(
            "LOCATION",
            String.format("현재 위치의 lat : %.4f lng : %.4f ", location?.latitude, location?.longitude)
        )
        return if (location != null) {
            Position(location.latitude, location.longitude, location.altitude)
        } else {
            Position(0.0, 0.0, 0.0)
        }
    }

    // 현재 Yaw 값을 0도에서 360도 사이로 변환
    fun getCurrentYaw(): Double {
        val yaw = attitude.get()?.yaw ?: 0.0
        return (yaw + 360) % 360
    }

    /**
     * 드론의 Yaw 값을 북쪽으로 조정
     */
    private fun adjustYawToNorth(callback: CommonCallbacks.CompletionCallback) {
        val currentYaw = getCurrentYaw()
        val yawDifference = calculateYawDifference(0.0, currentYaw)
        adjustYaw(yawDifference)

        handler.postDelayed({
            callback.onSuccess()
        }, 2000)
    }

    /**
     * 현재 스틱의 위치를 리턴
     */
    private fun getCurrentStickPositions(stickManager: IVirtualStickManager): Controls {
        return Controls(
            StickPosition(
                stickManager.leftStick.verticalPosition,
                stickManager.leftStick.horizontalPosition
            ),
            StickPosition(
                stickManager.rightStick.verticalPosition,
                stickManager.rightStick.horizontalPosition
            )
        )
    }
    // 드론이 현재 목표 지점에 도달했는지 여부를 나타내는 변수

    fun moveToTarget(position: Position, onComplete: () -> Unit) {
        // 이동 상태를 초기화
        val targetLat = position.latitude
        val targetLng = position.longitude
        val targetAti = position.altitude
        Log.d("DISTANCE", String.format("target: %.5f, targetLng: %.5f", targetLat, targetLng))

        handler.post(object : Runnable {
            override fun run() {
//                val currentPosition = getCurrentDronePosition()
                val currentPosition = getCurrentDronePositionForSimulation()
                val currentYaw = getCurrentYaw()
                Log.d(
                    "DEBUG", String.format(
                        "slat : %.4f slng : %.4f tlat : %.4f tlng : %.4f ",
                        currentPosition.latitude,
                        currentPosition.longitude,
                        targetLat,
                        targetLng
                    )
                )
                val result = calculateDistanceAndBearing(
                    currentPosition.latitude, currentPosition.longitude,
                    targetLat, targetLng
                )

                val distance = result.first
                val targetBearing = result.second
                val yawDifference = calculateYawDifference(targetBearing, currentYaw)

                Log.d(
                    "DISTANCE",
                    String.format(
                        " DISTANCE : %.4f, 런 안 target: %.4f, targetLng: %.4f",
                        distance,
                        targetLat,
                        targetLng
                    )
                )

                if (distance <= 1.0) {
                    // 목표 지점에 도달하면 정지하고 플래그 설정
                    initVirtualStickValue()
                    Log.d(SIMULATOR_TAG, "목표 지점에 도달했습니다.")
                    // 이동 종료
                    onComplete()
                } else {
                    Log.d("MOVE", "yaw Diff : ${abs(yawDifference)}")
                    if (abs(yawDifference) > 5) {
                        // 드론의 방향을 조정
                        adjustYaw(yawDifference)
                    } else {
                        // 드론을 전진시킴
                        moveToForward()
                    }
                    // 다음 체크를 위해 간격을 1초로 설정
                    handler.postDelayed(this, 100)
                }
            }
        })
    }

    private fun getCurrentDronePositionForSimulation(): Position {
        var location = location2D.get()
        return if (location != null) {
            Position(location.latitude, location.longitude, 1.2)
        } else {
            Position(0.0, 0.0, 0.0)
        }
    }


    // 7. Calculate Help
    fun calculateDistanceAndBearing(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Pair<Double, Double> {
        // 위도와 경도의 차이를 구한 후 라디안으로 변환
        val dLat = Math.toRadians(endLat - startLat)
        val dLng = Math.toRadians(endLng - startLng)
//        Log.d("DEBUG", "dLat: $dLat, dLng: $dLng")

        // 시작 및 끝 위도도 라디안으로 변환
        val startLatRad = Math.toRadians(startLat)
        val endLatRad = Math.toRadians(endLat)

        // Haversine 공식의 중간 계산 과정
        val a = sin(dLat / 2).pow(2.0) +
                cos(startLatRad) * cos(endLatRad) *
                sin(dLng / 2).pow(2.0)
//        Log.d("DEBUG", "a: $a")

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
//        Log.d("DEBUG", "c: $c")

        // 거리 계산 (미터 단위)
        val distance = EARTH_RADIUS * c
//        Log.d("DEBUG", "distance: $distance")

        // 방위각 계산
        val y = sin(dLng) * cos(endLatRad)
        val x = cos(startLatRad) * sin(endLatRad) -
                sin(startLatRad) * cos(endLatRad) * cos(dLng)
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360 // 방위각을 0~360도로 변환
//        Log.d("DEBUG", "bearing after normalization: $bearing")

        return Pair(distance, bearing)
    }


    // calculateYawDifference 함수 수정
    fun calculateYawDifference(targetBearing: Double, currentYaw: Double): Double {
        var difference = targetBearing - currentYaw
        // 각도 차이를 -180도에서 180도 사이로 조정
        difference = (difference + 540) % 360 - 180
        return difference
    }

    // 드론의 Yaw 값을 조정하여 방향 전환
    fun adjustYaw(yawDifference: Double) {
        val yawRate =
            yawDifference.coerceIn(-MAX_STICK_VALUE.toDouble(), MAX_STICK_VALUE.toDouble())
                .toInt() // Yaw 속도 제한 (deg/s)
        val controls = Controls(
            leftStick = StickPosition(0, yawRate), // Throttle, Yaw Rate
            rightStick = StickPosition(0, 0) // Pitch, Roll
        )
        setDroneControlValues(controls)
//        Log.d(SIMULATOR_TAG, "Adjusting yaw with yawRate: $yawRate")
    }


// 8. Control Value Settings, Print Logs
    /**
     * Control 값을 설정하고 로그를 출력하는 메서드
     */
    private fun applyControlValues(
        stickManager: IVirtualStickManager,
        controls: Controls,
        logTag: String
    ) {
        stickManager.leftStick.verticalPosition = controls.leftStick.verticalPosition
        stickManager.leftStick.horizontalPosition = controls.leftStick.horizontalPosition
        stickManager.rightStick.verticalPosition = controls.rightStick.verticalPosition
        stickManager.rightStick.horizontalPosition = controls.rightStick.horizontalPosition

//        Log.d(logTag, "Control values set: $controls")
    }

    /**
     * Stick 위치 값을 업데이트하는 메서드
     */
    private fun updateStickPositions(stickManager: IVirtualStickManager, controls: Controls) {
        applyControlValues(stickManager, controls, VIRTUAL_STICK_TAG)
    }

    fun subscribeTargetDistance(distance: Double) {

    }
}