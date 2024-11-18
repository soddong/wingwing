package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.shieldrone.station.constant.FlightConstant.Companion.MAX_STICK_VALUE
import com.shieldrone.station.data.LeftStick
import com.shieldrone.station.data.RightStick
import com.shieldrone.station.data.State
import com.shieldrone.station.data.StickPosition
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.flightcontroller.FCGoHomeState
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.ErrorType
import dji.v5.common.error.IDJIError
import dji.v5.et.action
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.IStick
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickState
import dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener

class FlightAutoControlModel {

    // 1. field, companion object
    var isVirtualStickEnabled = false
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "FlightAutoControlModel"

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
        val attitude by lazy { KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude) }

        val goToHome by lazy { KeyTools.createKey(FlightControllerKey.KeyStartGoHome) }
        val stopToHome by lazy { KeyTools.createKey(FlightControllerKey.KeyStopGoHome) }
        val homeState by lazy { KeyTools.createKey(FlightControllerKey.KeyGoHomeState) }
        val homeLocation by lazy { KeyTools.createKey(FlightControllerKey.KeyHomeLocation) }
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
        // Virtual Stick 모드가 활성화되어 있다면 비활성화
        if (isVirtualStickEnabled) {
            disableVirtualStickMode(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.d(TAG, "Virtual Stick 모드가 비활성화되었습니다.")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "Virtual Stick 모드 비활성화 실패: ${error.description()}")
                }
            })
        }

        Log.d(TAG, "FlightControlModel의 리소스가 해제되었습니다.")
    }

    // 3. Virtual Stick
    /**
     * Virtual Stick 모드 활성화
     */
    fun enableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        Log.d(TAG, "[Model] enableVirtualStickMode 시작")
        virtualStickManager.enableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                isVirtualStickEnabled = true
                Log.d(TAG, "[Model] enableVirtualStickMode 성공")
                callback.onSuccess()
            }

            override fun onFailure(error: IDJIError) {
                Log.d(TAG, "[Model] enableVirtualStickMode 실패: ${error.description()}")
                callback.onFailure(error)
            }
        })
    }

    fun disableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        Log.d(TAG, "[Model] disableVirtualStickMode 시작")

        virtualStickManager.disableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                isVirtualStickEnabled = false
                Log.d(TAG, "[Model] disableVirtualStickMode 성공")
                callback.onSuccess()
            }

            override fun onFailure(error: IDJIError) {
                Log.d(TAG, "[Model] disableVirtualStickMode 실패: ${error.description()}")
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
                virtualStickState =
                    """상태: ${stickState.currentFlightControlAuthorityOwner.name} | 활성화 여부: ${stickState.isVirtualStickEnable}""".trimIndent()
                updateCombinedState()
            }

            override fun onChangeReasonUpdate(reason: FlightControlAuthorityChangeReason) {
                Log.d(TAG, "subscribeVirtualStickState: ChangeReason 업데이트 $reason")
                changeReason = """변경 이유: ${reason.name}""".trimIndent()
                updateCombinedState()
            }

            private fun updateCombinedState() {
                val combinedState =
                    """${virtualStickState ?: "N/A"} | ${changeReason ?: "N/A"}""".trimIndent()

                onUpdate(combinedState)
            }
        })
    }

    // 4. Drone: Setting


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

    /**
     * 드론 위치 정보 구독
     */
    fun subscribeDroneState(onUpdate: (State) -> Unit) {
        val state = State()

        // 초기 값 설정
//        state.pitch = attitude.get()?.pitch
//        state.roll = attitude.get()?.roll
//        state.yaw = attitude.get()?.yaw
        state.compassHeading = compassHeading.get()
        state.latitude = location3D.get()?.latitude
        state.longitude = location3D.get()?.longitude
        state.altitude = location3D.get()?.altitude

//        KeyManager.getInstance().listen(attitude, this) { _, data ->
//            data?.let {
//                state.yaw = it.yaw
//                state.roll = it.roll
//                state.pitch = it.pitch
//                onUpdate(state)
//            }
//        }
        // 업데이트 리스너 설정
        KeyManager.getInstance().listen(location3D, this) { _, data ->
            data?.let {
                state.longitude = it.longitude
                state.latitude = it.latitude
                state.altitude = it.altitude
                onUpdate(state)
            }
        }
    }

    // 7. Calculate Help
    // 드론의 Yaw 값을 조정하여 방향 전환
    fun adjustYaw(yawDifference: Double) {
        Log.d(TAG, "[Model] adjustYaw 시작")
        val yawRate =
            yawDifference.coerceIn(-MAX_STICK_VALUE.toDouble(), MAX_STICK_VALUE.toDouble()).toInt()
        val leftStick = LeftStick(StickPosition(0, yawRate))
        setLeftStick(leftStick)
        Log.d(TAG, "[Model] adjustYaw 성공: yawRate=$yawRate")
    }


    /**
     * 드론을 전진시키는 메서드 (Pitch 값 조정)
     * rightStick만 제어하기
     */
    fun adjustPitch(pitch: Int) {
        Log.d(TAG, "[Model] adjustPitch 시작")
        val rightStick = RightStick().apply {
            verticalPosition = pitch
            horizontalPosition = 0
        }
        setRightStick(rightStick)
        Log.d(TAG, "[Model] adjustPitch 성공: pitch=$pitch")
    }

    fun adjustAltitude(altitude: Int) {
        Log.d(TAG, "[Model] adjustAltitude 시작")
        val leftStick = LeftStick().apply {
            verticalPosition = altitude
            horizontalPosition = 0
        }
        try {
            setLeftStick(leftStick)
            Log.d(TAG, "[Model] adjustAltitude 성공: altitude=$altitude")
        } catch (e: Exception) {
            Log.d(TAG, "[Model] adjustAltitude 실패: ${e.message}")
        }
    }

    fun adjustAutoControl(altitudePower: Double, yawPower: Double, pitchPower: Double) {
        val altitude =
            altitudePower.coerceIn(-MAX_STICK_VALUE.toDouble(), MAX_STICK_VALUE.toDouble()).toInt()
        val yaw =
            yawPower.coerceIn(-MAX_STICK_VALUE.toDouble(), MAX_STICK_VALUE.toDouble()).toInt()
        val pitch =
            pitchPower.coerceIn(-MAX_STICK_VALUE.toDouble(), MAX_STICK_VALUE.toDouble()).toInt()
        try {
            virtualStickManager.leftStick.verticalPosition = altitude
            virtualStickManager.leftStick.horizontalPosition = yaw
            virtualStickManager.rightStick.verticalPosition = pitch
            Log.d(TAG, "[Model] adjustAutoAltitude 성공: altitude=$altitude, yaw=$yaw, pitch=$pitch")
        } catch (e: Exception) {
            Log.d(TAG, "[Model] adjustAltitude 실패: ${e.message}")
        }
    }

    private fun setLeftStick(leftStick: IStick) {
        Log.d(TAG, "[Model] setLeftStick 시작")
        try {
            virtualStickManager.leftStick.verticalPosition = leftStick.verticalPosition
            virtualStickManager.leftStick.horizontalPosition = leftStick.horizontalPosition
            Log.d(
                TAG,
                "[Model] setLeftStick 성공: verticalPosition=${leftStick.verticalPosition}, horizontalPosition=${leftStick.horizontalPosition}"
            )
        } catch (e: Exception) {
            Log.d(TAG, "[Model] setLeftStick 실패: ${e.message}")
        }
    }

    private fun setRightStick(rightStick: IStick) {
        Log.d(TAG, "[Model] setRightStick 시작")
        try {
            virtualStickManager.rightStick.verticalPosition = rightStick.verticalPosition
            virtualStickManager.rightStick.horizontalPosition = rightStick.horizontalPosition
            Log.d(
                TAG,
                "[Model] setRightStick 성공: verticalPosition=${rightStick.verticalPosition}, horizontalPosition=${rightStick.horizontalPosition}"
            )
        } catch (e: Exception) {
            Log.d(TAG, "[Model] setRightStick 실패: ${e.message}")
        }
    }


    fun subscribeDroneGpsLevel(onUpdate: (Int) -> Unit) {
        Log.d(TAG, "[Model] subscribeDroneGpsLevel 시작")
        try {
            var currentGPSLevel = KeyManager.getInstance().getValue(keyGPSSignalLevel)?.value() ?: 0
            handler.post(object : Runnable {
                override fun run() {
                    val newGPSLevel =
                        KeyManager.getInstance().getValue(keyGPSSignalLevel)?.value() ?: 0

                    if (currentGPSLevel != newGPSLevel) {
                        currentGPSLevel = newGPSLevel
                        onUpdate(newGPSLevel)
                        Log.d(
                            TAG,
                            "[Model] subscribeDroneGpsLevel 성공: GPS Signal Level updated: $newGPSLevel"
                        )
                    }

                    handler.postDelayed(this, 1000L)
                }
            })
        } catch (e: Exception) {
            Log.d(TAG, "[Model] subscribeDroneGpsLevel 실패: ${e.message}")
        }
    }


    // 6. State, Location Helper

    fun subscribeGoHomeState(onUpdate: (FCGoHomeState) -> Unit) {
        // KeyManager를 통해 GoHome 상태를 구독
        KeyManager.getInstance().listen(homeState, this) { _, data ->
            data?.let { goHomeState ->
                Log.d(TAG, "GoHome 상태 업데이트: $goHomeState")
                onUpdate(goHomeState)
            } ?: Log.e(TAG, "GoHome 상태를 가져오지 못했습니다.")
        }
    }

    /**
     * Home 위치 구독
     */
    fun subscribeHomeLocation(onUpdate: (LocationCoordinate2D) -> Unit) {
        // KeyManager를 통해 GoHome 상태를 구독
        KeyManager.getInstance().listen(homeLocation, this) { _, data ->
            data?.let { homeLocation ->
                Log.d(TAG, "집주소 업데이트: $homeLocation")
                onUpdate(homeLocation)
            } ?: Log.e(TAG, "집주소를 받아오지 못했습니다.")
        }
    }

    /**
     * Home 위치를 Set
     */
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

    fun getHomeLocation() {
        val location = KeyManager.getInstance().getValue(homeLocation)

    }


}
