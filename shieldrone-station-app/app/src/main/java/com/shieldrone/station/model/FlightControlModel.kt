package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.ErrorType
import dji.v5.common.error.IDJIError
import dji.v5.et.action
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager

class FlightControlModel {

    data class Position(
        val altitude: Double,
        val latitude: Double,
        val longitude: Double
    )

    data class State(
        var longitude: Double? = null,
        var latitude: Double? = null,
        var altitude: Double? = null,
        var xVelocity: Double? = null,
        var yVelocity: Double? = null,
        var zVelocity: Double? = null,
        var compassHeading: Double? = null,
        var sticks: Controls? = null,
    )

    data class Controls(
        val leftStick: StickPosition,
        val rightStick: StickPosition
    )

    data class StickPosition(
        val verticalPosition: Int,
        val horizontalPosition: Int
    )

    var isVirtualStickEnabled = false
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        val keyConnection by lazy { KeyTools.createKey(FlightControllerKey.KeyConnection) }
        val keyIsFlying by lazy { KeyTools.createKey(FlightControllerKey.KeyIsFlying) }
        val keyFlightMode by lazy { KeyTools.createKey(FlightControllerKey.KeyFlightMode) }
        val keyAircraftLocation3D by lazy { KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D) }
        val keyStartTakeoff by lazy { KeyTools.createKey(FlightControllerKey.KeyStartTakeoff) }
        val keyStartAutoLanding by lazy { KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding) }
        val virtualStickManager by lazy { VirtualStickManager.getInstance() }
        val location3D by lazy { KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D) }
        val velocity3D by lazy { KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity) }
        val compassHeading by lazy { KeyTools.createKey(FlightControllerKey.KeyCompassHeading) }

        const val flightControlTag = "DJI"
        const val virtualStickTag = "VIRTUAL_STICK"
        const val simulatorTag = "SIMULATOR"
    }

    /**
     * 이륙 시작 함수
     */
    fun startTakeOff(callback: CommonCallbacks.CompletionCallback) {
        if (!checkPreconditionsForTakeoff()) {
            callback.onFailure(object : IDJIError {
                override fun errorType(): ErrorType {
                    TODO("Not yet implemented")
                }

                override fun errorCode(): String {
                    TODO("Not yet implemented")
                }

                override fun innerCode(): String {
                    TODO("Not yet implemented")
                }

                override fun hint(): String {
                    TODO("Not yet implemented")
                }

                override fun description(): String {
                    return "이륙 전 조건이 충족되지 않았습니다."
                }

                override fun isError(p0: String?): Boolean {
                    TODO("Not yet implemented")
                }

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
        val isConnected = KeyManager.getInstance().getValue(keyConnection) as? Boolean ?: false
        val isFlying = KeyManager.getInstance().getValue(keyIsFlying) as? Boolean ?: false
        return isConnected && !isFlying
    }

    /**
     * 이륙 상태 모니터링
     */
    fun monitorTakeoffStatus() {
        KeyManager.getInstance().listen(keyIsFlying, this) { _, isFlying ->
            if (isFlying == true) {
                Log.d(flightControlTag, "Drone is now flying")
                // 추가 로직
            }
        }
    }

    /**
     * Virtual Stick 모드 활성화
     */
    fun enableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        virtualStickManager.enableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                isVirtualStickEnabled = true
                virtualStickManager.setVirtualStickAdvancedModeEnabled(true)
                callback.onSuccess()
            }

            override fun onFailure(error: IDJIError) {
                callback.onFailure(error)
            }
        })
    }

    /**
     * Virtual Stick 모드 비활성화
     */
    fun disableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        virtualStickManager.disableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                isVirtualStickEnabled = false
                callback.onSuccess()
            }

            override fun onFailure(error: IDJIError) {
                callback.onFailure(error)
            }
        })
    }

    /**
     * 드론 위치 정보 구독
     */
    fun subscribeDroneLocation(onUpdate: (State) -> Unit) {
        val state = State()

        // 초기 값 설정
        state.longitude = location3D.get()?.longitude
        state.latitude = location3D.get()?.latitude
        state.altitude = location3D.get()?.altitude
        state.xVelocity = velocity3D.get()?.x
        state.yVelocity = velocity3D.get()?.y
        state.zVelocity = velocity3D.get()?.z
        state.compassHeading = compassHeading.get()

        // 업데이트 리스너 설정
        KeyManager.getInstance().listen(location3D, this) { _, data ->
            data?.let {
                state.longitude = it.longitude
                state.latitude = it.latitude
                state.altitude = it.altitude
                onUpdate(state)
            }
        }

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
        if (!isVirtualStickEnabled) {
            callback.onFailure(object : IDJIError {
                override fun errorType() = ErrorType.UNKNOWN
                override fun errorCode() = "VIRTUAL_STICK_DISABLED"
                override fun innerCode() = "VIRTUAL_STICK_DISABLED"
                override fun hint() = "Virtual Stick 모드가 활성화되지 않았습니다."
                override fun description() = "Virtual Stick 모드를 활성화 후 시도하세요."
                override fun isError(p0: String?) = true
            })
            return
        }

        val stickManager = virtualStickManager

        stickManager.leftStick.verticalPosition = controls.leftStick.verticalPosition
        stickManager.leftStick.horizontalPosition = controls.leftStick.horizontalPosition
        stickManager.rightStick.verticalPosition = controls.rightStick.verticalPosition
        stickManager.rightStick.horizontalPosition = controls.rightStick.horizontalPosition

        handler.post {
            callback.onSuccess()
        }

        Log.d(virtualStickTag, "leftStick (vertical-고도: ${controls.leftStick.verticalPosition}, horizontal-좌우회전: ${controls.leftStick.horizontalPosition}), " +
                "rightStick (vertical-앞뒤: ${controls.rightStick.verticalPosition}, horizontal-좌우이동: ${controls.rightStick.horizontalPosition})")
    }

    /**
     * 드론의 control 값을 구독하고 지속적으로 업데이트하여 제어하는 함수
     */
    fun subscribeControlValues(onUpdate: (Controls) -> Unit) {
        if (!isVirtualStickEnabled) {
            Log.e(virtualStickTag, "Virtual Stick 모드가 활성화되지 않았습니다.")
            return
        }

        val stickManager = virtualStickManager

        // 초기 control 값을 설정
        var currentControls = Controls(
            StickPosition(stickManager.leftStick.verticalPosition, stickManager.leftStick.horizontalPosition),
            StickPosition(stickManager.rightStick.verticalPosition, stickManager.rightStick.horizontalPosition)
        )

        // 일정 간격으로 control 값을 갱신하고 콜백 호출
        handler.post(object : Runnable {
            override fun run() {
                // 새로운 Controls 상태를 구독
                val newControls = Controls(
                    StickPosition(stickManager.leftStick.verticalPosition, stickManager.leftStick.horizontalPosition),
                    StickPosition(stickManager.rightStick.verticalPosition, stickManager.rightStick.horizontalPosition)
                )

                // 새로운 control 값을 onUpdate 콜백으로 전달
                onUpdate(newControls)

                // 이전 상태와 새로운 상태 비교 후 변경이 있을 때만 적용
                if (currentControls != newControls) {
                    stickManager.leftStick.verticalPosition = newControls.leftStick.verticalPosition
                    stickManager.leftStick.horizontalPosition = newControls.leftStick.horizontalPosition
                    stickManager.rightStick.verticalPosition = newControls.rightStick.verticalPosition
                    stickManager.rightStick.horizontalPosition = newControls.rightStick.horizontalPosition
                    Log.d(virtualStickTag, "Control updated: leftStick(vertical: ${newControls.leftStick.verticalPosition}, horizontal: ${newControls.leftStick.horizontalPosition}), " +
                            "rightStick(vertical: ${newControls.rightStick.verticalPosition}, horizontal: ${newControls.rightStick.horizontalPosition})")

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
        val handler = Handler(Looper.getMainLooper())

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
                        flightControlTag,
                        "Position updated: altitude = $newAltitude, latitude = $newLatitude, longitude = $newLongitude"
                    )
                }

                // 100ms 주기로 위치 정보를 업데이트
                handler.postDelayed(this, 100)
            }
        })
    }
}
