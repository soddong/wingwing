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
        val verticalPosition: Float,
        val horizontalPosition: Float
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
}
