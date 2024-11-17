package com.shieldrone.station.model

import android.util.Log
import com.shieldrone.station.constant.FlightConstant.Companion.MAX_ASCENT_SPEED
import com.shieldrone.station.constant.FlightConstant.Companion.MAX_DESCENT_SPEED
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class FlightControlModel {

    private val TAG = "FlightControlModel"

    // 1. field, companion object
    private var isVirtualStickEnabled = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
    val state = State()
    var currentVerticalPosition: Int = 0
    var currentYawPosition: Int = 0
    // 2. LifeCycle
    /**
     * 리소스 해제 및 메모리 누수 방지 메서드
     */
    private fun onDestroy() {
        // 핸들러의 모든 콜백 제거
        removeAllKeySubscriptions()
        disableVirtualStickIfNeeded()
        coroutineScope.cancel()
        Log.d(TAG, "FlightControlModel의 리소스가 해제되었습니다.")
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

    private fun removeAllKeySubscriptions() {
        listOf(
            FlightAutoControlModel.keyConnection,
            FlightAutoControlModel.keyIsFlying,
            FlightAutoControlModel.location3D,
            FlightAutoControlModel.velocity3D,
            FlightAutoControlModel.compassHeading,
            FlightAutoControlModel.keyGPSSignalLevel,
            FlightAutoControlModel.attitude
        ).forEach { key ->
            KeyManager.getInstance().cancelListen(key)
        }
        Log.d(TAG, "KeyManager 모든 구독 해제 완료")
    }
    // 3. Virtual Stick
    /**
     * Virtual Stick 모드 활성화
     */
    // 3. Virtual Stick
    // Virtual Stick 관리
    fun enableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        executeVirtualStickAction("enable", callback)
    }

    fun disableVirtualStickMode(callback: CommonCallbacks.CompletionCallback) {
        executeVirtualStickAction("disable", callback)
    }

    fun subscribeVirtualStickState(onUpdate: (String) -> Unit) {
        var virtualStickState: String? = null
        var changeReason: String? = null


        FlightAutoControlModel.virtualStickManager.setVirtualStickStateListener(object :
            VirtualStickStateListener {
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


//    // 스틱 제어
//    fun adjustYaw(yawDifference: Double) {
//        val yawRate =
//            yawDifference.coerceIn(-MAX_STICK_VALUE.toDouble(), MAX_STICK_VALUE.toDouble()).toInt()
//        setLeftStick(LeftStick(StickPosition(0, yawRate)))
//    }
    fun adjustAltitude(altitude: Int) {
        setLeftStick(LeftStick().apply { verticalPosition = altitude })
    }

    fun adjustPitch(pitch: Int) {
        setRightStick(RightStick().apply { verticalPosition = pitch })
    }
    /**
     * yaw와 altitude를 동시에 조절하는 메서드
     */
    fun adjustLeftStick(yawDifference: Double, desiredAltitude: Double) {
        // 최대 스틱 값과 상승/하강 속도 정의
        val altitudeKp = 0.5            // 고도 제어를 위한 비례 이득 (적절히 조정 필요)

        // yawDifference를 기반으로 yawRate 계산
        val yawRate = yawDifference.coerceIn(-MAX_STICK_VALUE.toDouble(), MAX_STICK_VALUE.toDouble()).toInt()
        currentYawPosition = yawRate

        // 현재 고도 가져오기 (state 객체를 통해)
        val currentAltitude = state.altitude ?: 0.0

        // 고도 오차 계산
        val altitudeError = desiredAltitude - currentAltitude

        // 원하는 상승/하강 속도 계산 (m/s 단위)
        var verticalSpeed = altitudeKp * altitudeError

        // 상승/하강 속도를 제한
        verticalSpeed = verticalSpeed.coerceIn(MAX_DESCENT_SPEED, MAX_ASCENT_SPEED)

        // 속도 명령을 스틱 입력 값으로 변환하는 계수 계산
        val SPEED_TO_STICK = MAX_STICK_VALUE / MAX_ASCENT_SPEED // 660 / 5.0 = 132.0

        // 속도 명령을 스틱 입력 값으로 변환
        val verticalSpeedCommand = (verticalSpeed * SPEED_TO_STICK).toInt()

        // 스틱 입력 값을 제한된 범위 내로 조정
        val verticalSpeedCommandLimited = verticalSpeedCommand.coerceIn(-MAX_STICK_VALUE, MAX_STICK_VALUE)
        currentVerticalPosition = verticalSpeedCommandLimited

        // LeftStick 생성 (pitch는 0으로 설정)
        val leftStick = LeftStick(StickPosition(0, currentYawPosition)).apply {
            verticalPosition = currentVerticalPosition
        }

        // 드론의 컨트롤 업데이트
        setLeftStick(leftStick)
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

}
