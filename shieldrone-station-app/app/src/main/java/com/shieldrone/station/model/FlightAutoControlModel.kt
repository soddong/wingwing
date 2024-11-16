package com.shieldrone.station.model

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.shieldrone.station.constant.FlightContstant.Companion.BTN_DELAY
import com.shieldrone.station.constant.FlightContstant.Companion.FLIGHT_CONTROL_TAG
import com.shieldrone.station.constant.FlightContstant.Companion.MAX_STICK_VALUE
import com.shieldrone.station.constant.FlightContstant.Companion.SIMULATOR_TAG
import com.shieldrone.station.constant.FlightContstant.Companion.VIRTUAL_STICK_TAG
import com.shieldrone.station.data.Controls
import com.shieldrone.station.data.LeftStick
import com.shieldrone.station.data.RightStick
import com.shieldrone.station.data.State
import com.shieldrone.station.data.StickPosition
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.flightcontroller.FCGoHomeState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.ErrorType
import dji.v5.common.error.IDJIError
import dji.v5.et.action
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.IStick
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager

class FlightAutoControlModel {

    // 1. field, companion object
    var isVirtualStickEnabled = false
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
                    Log.d(VIRTUAL_STICK_TAG, "Virtual Stick 모드가 비활성화되었습니다.")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(VIRTUAL_STICK_TAG, "Virtual Stick 모드 비활성화 실패: ${error.description()}")
                }
            })
        }

        Log.d(FLIGHT_CONTROL_TAG, "FlightControlModel의 리소스가 해제되었습니다.")
    }

    // 3. Virtual Stick
    /**
     * Virtual Stick 모드 활성화
     */
    fun enableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        virtualStickManager.enableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                isVirtualStickEnabled = true
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


    // 4. Drone: Setting


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

            }, { e: IDJIError ->
                callback.onFailure(e)
            })
        }
    }

    /**
     * 착륙 시작 함수
     */
    fun startLanding(callback: CommonCallbacks.CompletionCallback) {
        callback.onFailure(object : IDJIError {
            override fun errorType() = ErrorType.UNKNOWN
            override fun errorCode() = "LANDING_FAILED"
            override fun innerCode() = "LANDING_FAILED"
            override fun hint() = "착륙 시도 횟수를 초과했습니다."
            override fun description() = "착륙에 반복 실패하였습니다."
            override fun isError(p0: String?) = true
        })
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
        Log.d(
            FLIGHT_CONTROL_TAG,
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
                Log.d(FLIGHT_CONTROL_TAG, "Drone is now flying")
            }
        }
    }

    /**
     * 드론 위치 정보 구독
     */
    fun subscribeDroneState(onUpdate: (State) -> Unit) {
        val state = State()

        // 초기 값 설정
        state.pitch = attitude.get()?.pitch
        state.roll = attitude.get()?.roll
        state.yaw = attitude.get()?.yaw
        state.xVelocity = velocity3D.get()?.x
        state.yVelocity = velocity3D.get()?.y
        state.zVelocity = velocity3D.get()?.z
        state.compassHeading = compassHeading.get()
        state.latitude = location3D.get()?.latitude
        state.longitude = location3D.get()?.longitude
        state.altitude = location3D.get()?.altitude

        KeyManager.getInstance().listen(attitude, this) { _, data ->
            data?.let {
                state.yaw = it.yaw
                state.roll = it.roll
                state.pitch = it.pitch
                onUpdate(state)
            }
        }
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

    }

    // 7. Calculate Help
    // 드론의 Yaw 값을 조정하여 방향 전환
    fun adjustYaw(yawDifference: Double) {
        val yawRate =
            yawDifference.coerceIn(-MAX_STICK_VALUE.toDouble(), MAX_STICK_VALUE.toDouble())
                .toInt() // Yaw 속도 제한 (deg/s)
        val leftStick = LeftStick(
            StickPosition(0, yawRate), // Throttle, Yaw Rate
        )
        setLeftStick(leftStick)
        Log.d(SIMULATOR_TAG, "Adjusting yaw with yawRate: $yawRate")
    }

    /**
     * 드론을 전진시키는 메서드 (Pitch 값 조정)
     * rightStick만 제어하기
     */
    fun adjustPitch(pitch: Int) {
        val rightStick = RightStick().apply {
            verticalPosition = pitch
            horizontalPosition = 0
        }
        setRightStick(rightStick)
    }

    fun adjustAltitude(altitude: Int) {

        val leftStick = LeftStick().apply {
            verticalPosition = altitude
            horizontalPosition = 0
        }
        setLeftStick(leftStick)
    }

    private fun setLeftStick(leftStick: IStick) {
        virtualStickManager.leftStick.verticalPosition = leftStick.verticalPosition
        virtualStickManager.leftStick.horizontalPosition = leftStick.horizontalPosition

        Log.d(VIRTUAL_STICK_TAG, "left stick values set: ${virtualStickManager.leftStick}")

    }

    private fun setRightStick(rightStick: IStick) {
        virtualStickManager.rightStick.verticalPosition = rightStick.verticalPosition
        virtualStickManager.rightStick.horizontalPosition = rightStick.horizontalPosition

        Log.d(VIRTUAL_STICK_TAG, "right stick values set: ${virtualStickManager.rightStick}")
    }


    fun subscribeDroneGpsLevel(onUpdate: (Int) -> Unit) {
        var currentGPSLevel = KeyManager.getInstance().getValue(keyGPSSignalLevel)?.value() ?: 0
        handler.post(object : Runnable {
            override fun run() {
                val newGPSLevel = KeyManager.getInstance().getValue(keyGPSSignalLevel)?.value() ?: 0

                if (currentGPSLevel != newGPSLevel) {
                    currentGPSLevel = newGPSLevel
                    onUpdate(newGPSLevel)
                    Log.d(FLIGHT_CONTROL_TAG, "GPS Signal Level updated: $newGPSLevel")
                }

                handler.postDelayed(this, BTN_DELAY)
            }
        })
    }

    // 6. State, Location Helper
    fun getCurrentStickPositions(): Controls {
        return Controls(
            StickPosition(
                virtualStickManager.leftStick.verticalPosition,
                virtualStickManager.leftStick.horizontalPosition
            ),
            StickPosition(
                virtualStickManager.rightStick.verticalPosition,
                virtualStickManager.rightStick.horizontalPosition
            )
        )
    }


    fun subscribeGoHomeState(onUpdate: (FCGoHomeState) -> Unit) {
        // KeyManager를 통해 GoHome 상태를 구독
        KeyManager.getInstance().listen(homeState, this) { _, data ->
            data?.let { goHomeState ->
                Log.d(FLIGHT_CONTROL_TAG, "GoHome 상태 업데이트: $goHomeState")
                onUpdate(goHomeState)
            } ?: Log.e(FLIGHT_CONTROL_TAG, "GoHome 상태를 가져오지 못했습니다.")
        }
    }

    /**
     * Home 위치 구독
     */
    fun subscribeHomeLocation(onUpdate: (LocationCoordinate2D) -> Unit) {
        // KeyManager를 통해 GoHome 상태를 구독
        KeyManager.getInstance().listen(homeLocation, this) { _, data ->
            data?.let { homeLocation ->
                Log.d(FLIGHT_CONTROL_TAG, "집주소 업데이트: $homeLocation")
                onUpdate(homeLocation)
            } ?: Log.e(FLIGHT_CONTROL_TAG, "집주소를 받아오지 못했습니다.")
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
                            FLIGHT_CONTROL_TAG,
                            "Home location successfully set to: $homeLocation2D"
                        )
                        callback.onSuccess()
                    }

                    override fun onFailure(error: IDJIError) {
                        Log.e(
                            FLIGHT_CONTROL_TAG,
                            "Failed to set home location: ${error.description()}"
                        )
                        callback.onFailure(error)
                    }
                })
        } else {
            Log.e(FLIGHT_CONTROL_TAG, "Current location not available to set as home location.")
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

    /**
     * Return to Home 중지
     */
//    fun stopReturnToHome(callback: CommonCallbacks.CompletionCallback) {
//        KeyManager.getInstance().performAction(stopToHome, object : CommonCallbacks.CompletionCallback {
//            override fun onSuccess() {
//                Log.d(FLIGHT_CONTROL_TAG, "Return to Home 중지됨")
//                callback.onSuccess()
//            }
//
//            override fun onFailure(error: IDJIError) {
//                Log.e(FLIGHT_CONTROL_TAG, "Return to Home 중지 실패: ${error.description()}")
//                callback.onFailure(error)
//            }
//        })
//    }


}
