package com.shieldrone.station.model

import android.util.Log
import com.shieldrone.station.constant.FlightConstant.Companion.MAX_STICK_VALUE
import com.shieldrone.station.data.LeftStick
import com.shieldrone.station.data.RightStick
import com.shieldrone.station.data.State
import com.shieldrone.station.data.StickPosition
import com.shieldrone.station.model.FlightAutoControlModel.Companion.attitude
import com.shieldrone.station.model.FlightAutoControlModel.Companion.compassHeading
import com.shieldrone.station.model.FlightAutoControlModel.Companion.goToHome
import com.shieldrone.station.model.FlightAutoControlModel.Companion.homeLocation
import com.shieldrone.station.model.FlightAutoControlModel.Companion.keyConnection
import com.shieldrone.station.model.FlightAutoControlModel.Companion.keyGPSSignalLevel
import com.shieldrone.station.model.FlightAutoControlModel.Companion.keyIsFlying
import com.shieldrone.station.model.FlightAutoControlModel.Companion.keyStartAutoLanding
import com.shieldrone.station.model.FlightAutoControlModel.Companion.keyStartTakeoff
import com.shieldrone.station.model.FlightAutoControlModel.Companion.location3D
import com.shieldrone.station.model.FlightAutoControlModel.Companion.velocity3D
import com.shieldrone.station.model.FlightAutoControlModel.Companion.virtualStickManager
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.ErrorType
import dji.v5.common.error.IDJIError
import dji.v5.et.action
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.IStick
import dji.v5.manager.aircraft.virtualstick.VirtualStickState
import dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class FlightControlGPSModel {
    private val TAG = "FlightControlGPSModel"
    private var isVirtualStickEnabled = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    fun onDestroy() {
        removeAllKeySubscriptions()
        disableVirtualStickIfNeeded()
        coroutineScope.cancel()
        Log.d(TAG, "FlightControlGPSModel 리소스 해제 완료")
    }

    private fun removeAllKeySubscriptions() {
        listOf(
            keyConnection, keyIsFlying, location3D, velocity3D,
            compassHeading, keyGPSSignalLevel, attitude
        ).forEach { key ->
            KeyManager.getInstance().cancelListen(key)
        }
        Log.d(TAG, "KeyManager 모든 구독 해제 완료")
    }

    private fun disableVirtualStickIfNeeded() {
        if (isVirtualStickEnabled) {
            disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.d(TAG, "Virtual Stick 모드 비활성화 완료")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "Virtual Stick 모드 비활성화 실패: ${error.description()}")
                }
            })
        }
    }

    // 3. Virtual Stick
    // Virtual Stick 관리
    fun enableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        executeVirtualStickAction("enable", callback)
    }

    fun disableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        executeVirtualStickAction("disable", callback)
    }

    private fun executeVirtualStickAction(
        action: String,
        callback: CommonCallbacks.CompletionCallback
    ) {
        val virtualStickAction = when (action) {
            "enable" -> virtualStickManager::enableVirtualStick
            "disable" -> virtualStickManager::disableVirtualStick
            else -> return
        }

        Log.d(TAG, "[Model] Virtual Stick $action 시작")
        virtualStickAction.invoke(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                isVirtualStickEnabled = (action == "enable")
                Log.d(TAG, "[Model] Virtual Stick $action 성공")
                callback.onSuccess()
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "[Model] Virtual Stick $action 실패: ${error.description()}")
                callback.onFailure(error)
            }
        })
    }

    fun subscribeVirtualStickState(onUpdate: (String) -> Unit) {
        var virtualStickState: String? = null
        var changeReason: String? = null


        virtualStickManager.setVirtualStickStateListener(object : VirtualStickStateListener {
            override fun onVirtualStickStateUpdate(stickState: VirtualStickState) {
                Log.d(TAG, "subscribeVirtualStickState: VirtualStickState 업데이트 $stickState")
                virtualStickState = """
                가상 스틱 상태: ${stickState.currentFlightControlAuthorityOwner.name}
                가상 스틱 활성화 여부: ${stickState.isVirtualStickEnable}
            """.trimIndent()
                updateCombinedState()
            }

            override fun onChangeReasonUpdate(reason: FlightControlAuthorityChangeReason) {
                Log.d(TAG, "subscribeVirtualStickState: ChangeReason 업데이트 $reason")
                changeReason = """
                가상 스틱 상태 변경 이유: ${reason.name}
            """.trimIndent()
                updateCombinedState()
            }

            private fun updateCombinedState() {
                val combinedState = """
                VirtualStickState:
                ${virtualStickState ?: "N/A"}

                ChangeReason:
                ${changeReason ?: "N/A"}
            """.trimIndent()

                onUpdate(combinedState)
            }
        })
    }

    // 4. Drone: Setting


    // 드론 동작 관리
    /**
     * 이륙 시작 함수
     */
    fun startTakeOff(callback: CommonCallbacks.CompletionCallback) {
        Log.d(TAG, "[Model] startTakeOff 시작")
        if (!checkPreconditionsForTakeoff()) {
            Log.d(TAG, "[Model] startTakeOff 실패: 이륙 조건 불충족")
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
                Log.d(TAG, "[Model] startTakeOff 성공")
                callback.onSuccess()
            }, { e: IDJIError ->
                Log.d(TAG, "[Model] startTakeOff 실패: ${e.description()}")
                callback.onFailure(e)
            })
        }
    }

    /**
     * 착륙 시작 함수
     */
    fun startLanding(callback: CommonCallbacks.CompletionCallback) {
        Log.d(TAG, "[Model] startLanding 시작")
        KeyManager.getInstance().run {
            keyStartAutoLanding.action({
                Log.d(TAG, "[Model] startLanding 성공")
                callback.onSuccess()
            }, { e: IDJIError ->
                Log.d(TAG, "[Model] startLanding 실패: ${e.description()}")
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
        Log.d(
            TAG,
            "Preconditions for takeoff - isConnected: $isConnected, isFlying: $isFlying"
        )

        return isConnected && !isFlying
    }

    /**
     * 이륙 상태 모니터링
     */
    fun monitorTakeoffStatus() {
        KeyManager.getInstance().listen(keyIsFlying, this) { _, isFlying ->
            if (isFlying == true) {
                Log.d(TAG, "Drone is now flying")
            }
        }
    }

    fun subscribeDroneState(onUpdate: (State) -> Unit) {
        val state = State()
        updateStateFromKeys(state, onUpdate)

        // 구독 설정
        subscribeKey(attitude) { state.updateAttitude(it, onUpdate) }
        subscribeKey(location3D) { state.updateLocation(it, onUpdate) }
        subscribeKey(velocity3D) { state.updateVelocity(it, onUpdate) }
    }

    private fun <T> subscribeKey(key: DJIKey<T>, update: (T?) -> Unit) {
        KeyManager.getInstance().listen(key, this) { _, value ->
            update(value)
        }
    }

    private fun updateStateFromKeys(state: State, onUpdate: (State) -> Unit) {
        state.pitch = KeyManager.getInstance().getValue(attitude)?.pitch
        state.roll = KeyManager.getInstance().getValue(attitude)?.roll
        state.yaw = KeyManager.getInstance().getValue(attitude)?.yaw
        state.latitude = KeyManager.getInstance().getValue(location3D)?.latitude
        state.longitude = KeyManager.getInstance().getValue(location3D)?.longitude
        state.altitude = KeyManager.getInstance().getValue(location3D)?.altitude
        state.xVelocity = KeyManager.getInstance().getValue(velocity3D)?.x
        state.yVelocity = KeyManager.getInstance().getValue(velocity3D)?.y
        state.zVelocity = KeyManager.getInstance().getValue(velocity3D)?.z
        state.compassHeading = KeyManager.getInstance().getValue(compassHeading)

        onUpdate(state)
    }

    /**
     * 드론 위치 정보 구독
     */
//    fun subscribeDroneState(onUpdate: (State) -> Unit) {
//        val state = State()
//
//        // 초기 값 설정
//        state.pitch = attitude.get()?.pitch
//        state.roll = attitude.get()?.roll
//        state.yaw = attitude.get()?.yaw
//        state.xVelocity = velocity3D.get()?.x
//        state.yVelocity = velocity3D.get()?.y
//        state.zVelocity = velocity3D.get()?.z
//        state.compassHeading = compassHeading.get()
//        state.latitude = location3D.get()?.latitude
//        state.longitude = location3D.get()?.longitude
//        state.altitude = location3D.get()?.altitude
//
//        KeyManager.getInstance().listen(attitude, this) { _, data ->
//            data?.let {
//                state.yaw = it.yaw
//                state.roll = it.roll
//                state.pitch = it.pitch
//                onUpdate(state)
//            }
//        }
//        // 업데이트 리스너 설정
//        KeyManager.getInstance().listen(location3D, this) { _, data ->
//            data?.let {
//                state.longitude = it.longitude
//                state.latitude = it.latitude
//                state.altitude = it.altitude
//                onUpdate(state)
//            }
//        }
//
//        KeyManager.getInstance().listen(velocity3D, this) { _, data ->
//            data?.let {
//                state.xVelocity = it.x
//                state.yVelocity = it.y
//                state.zVelocity = it.z
//                // 조건 체크: 속도가 1m/s 이상인 경우 handleInvalidVelocity 호출
//                if (it.x.absoluteValue >= 1.0 || it.y.absoluteValue >= 1.0 || it.z.absoluteValue >= 1.0) {
//                    handleInvalidVelocity()
//                }
//                onUpdate(state)
//            }
//        }
//
//    }

    // 스틱 제어
    fun adjustYaw(yawDifference: Double) {
        val yawRate =
            yawDifference.coerceIn(-MAX_STICK_VALUE.toDouble(), MAX_STICK_VALUE.toDouble()).toInt()
        setLeftStick(LeftStick(StickPosition(0, yawRate)))
    }

    fun adjustPitch(pitch: Int) {
        setRightStick(RightStick().apply { verticalPosition = pitch })
    }

    fun adjustAltitude(altitude: Int) {
        setLeftStick(LeftStick().apply { verticalPosition = altitude })
    }

    private fun setLeftStick(leftStick: IStick) {
        setStick(leftStick, virtualStickManager.leftStick)
    }

    private fun setRightStick(rightStick: IStick) {
        setStick(rightStick, virtualStickManager.rightStick)
    }

    private fun setStick(source: IStick, target: IStick) {
        target.apply {
            verticalPosition = source.verticalPosition
            horizontalPosition = source.horizontalPosition
        }
    }

    private fun handleInvalidVelocity() {
        Log.d(TAG, "비 정상적입니다. 모든 값을 0으로 초기화하고 착륙을 시작합니다.")

        // 모든 값을 0으로 초기화
        virtualStickManager.rightStick.verticalPosition = 0
        virtualStickManager.rightStick.horizontalPosition = 0
        virtualStickManager.leftStick.verticalPosition = 0
        virtualStickManager.leftStick.horizontalPosition = 0

        // 착륙 강제 호출
        startLanding(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.d(TAG, "강제 착륙 성공")
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "강제 착륙 실패: ${error.description()}")
            }
        })
    }

    fun setHomeLocation(callback: CommonCallbacks.CompletionCallback) {
        // Retrieve the current 3D location (latitude, longitude, altitude) from the drone
        val currentLocation3D: LocationCoordinate3D? = KeyManager.getInstance().getValue(location3D)

        if (currentLocation3D != null) {
            // Convert LocationCoordinate3D to LocationCoordinate2D (latitude, longitude only)
            val homeLocation2D =
                LocationCoordinate2D(currentLocation3D.latitude, currentLocation3D.longitude)

            // Set the 2D location as the home location
            KeyManager.getInstance().setValue(
                homeLocation,
                homeLocation2D,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        Log.d(
                            TAG,
                            "Home location successfully set to: $homeLocation2D"
                        )
                        callback.onSuccess()
                    }

                    override fun onFailure(error: IDJIError) {
                        Log.e(
                            TAG,
                            "Failed to set home location: ${error.description()}"
                        )
                        callback.onFailure(error)
                    }
                })
        } else {
            Log.e(TAG, "Current location not available to set as home location.")
            callback.onFailure(object : IDJIError {
                override fun errorType() = ErrorType.UNKNOWN
                override fun errorCode() = "LOCATION_NOT_AVAILABLE"
                override fun innerCode() = "LOCATION_NOT_AVAILABLE"
                override fun hint() = "Could not retrieve current location."
                override fun description() = "Drone's current location is unavailable."
                override fun isError(p0: String?) = true
            })
        }
    }


    /**
     * 드론을 홈 포인트로 복귀시키는 메서드 (Return to Home) - homeLocation
     */
    fun startReturnToHome(callback: CommonCallbacks.CompletionCallback) {
        KeyManager.getInstance().run {
            goToHome
                .action({
                    onDestroy()
                    callback.onSuccess()
                }, { e: IDJIError ->
                    callback.onFailure(e)
                })
        }
    }
}