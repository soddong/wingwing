package com.shieldrone.station.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.shieldrone.station.R
import com.shieldrone.station.databinding.FragmentFlightControlBinding
import com.shieldrone.station.model.FlightControlVM
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.SDKManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickState
import dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener

class FlightControlActivity : AppCompatActivity(R.layout.fragment_flight_control) {

    private lateinit var binding: FragmentFlightControlBinding
    private var isVirtualStickEnabled = false
    private val flightControlVM: FlightControlVM by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var verticalThrottle = 0.0

    companion object {
        const val ALTITUDE_INCREMENT = 0.02   // 매 200ms마다 고도가 0.02m 증가
        const val UPDATE_INTERVAL_MS = 200    // 200ms 간격으로 업데이트 (5Hz)
        const val VERTICAL_THROTTLE = 0.1     // 초당 0.1m 상승 속도
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentFlightControlBinding.inflate(layoutInflater)
        VirtualStickManager.getInstance().init()

        setContentView(binding.root)
        setVirtualStickStateListener()
        if (KeyManager.getInstance() == null) {
            Log.e("DJI", "KeyManager is not initialized")
            return
        }

        // Z축 상승 버튼 클릭 리스너
        findViewById<Button>(R.id.btnZAxisUp).setOnClickListener {
            adjustAltitudeBy(VERTICAL_THROTTLE) // 0.1m/s 상승
        }

        // Z축 하강 버튼 클릭 리스너
        findViewById<Button>(R.id.btnZAxisDown).setOnClickListener {
            adjustAltitudeBy(-VERTICAL_THROTTLE) // 0.1m/s 하강
        }

        findViewById<Button>(R.id.btnEnableVirtualStick).setOnClickListener {
            enableVirtualStickMode()
        }
        findViewById<Button>(R.id.btnDisableVirtualStick).setOnClickListener {
            disableVirtualStickMode()
        }
        findViewById<Button>(R.id.checkAltitude).setOnClickListener {
//            checkAltitude()
        }
        findViewById<Button>(R.id.btnTakeoffStart).setOnClickListener {
            checkAndStartTakeoff()
        }

        findViewById<Button>(R.id.btnTakeoffStop).setOnClickListener {
            checkAndStopTakeoff()
        }
        startSendingControlData()
    }

//    private fun observeDroneAltitude() {
//        val keyAircraftLocation3D = KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D)
//
//
//        // listen() 메서드를 사용하여 고도 데이터 지속적으로 수신
//        keyAircraftLocation3D.listen(
//            holder = this,      // 현재 Activity 또는 Fragment를 holder로 전달
//            getOnce = false     // 지속적으로 데이터를 수신하기 위해 false 설정
//        ) { location: LocationCoordinate3D? ->
//            location?.let {
//                val altitude = it.altitude
//                val latitude = it.latitude
//                val longitude = it.longitude
//                Log.d("DJI", "현재 고도: $altitude 미터")
//                Log.d("DJI", "현재 위도: $latitude")
//                Log.d("DJI", "현재 경도: $longitude")
//
//                flightControlVM.updateAltitude(altitude)  // ViewModel의 LiveData 업데이트
//            } ?: run {
//                Log.d("DJI", "위치 정보를 가져올 수 없습니다.")
//            }
//        }
//    }

//    private fun checkAltitude() {
//        // 드론 고도 관찰
//        val keyAircraftLocation3D = KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D)
//
//        if (keyAircraftLocation3D.canGet()) {
//            Log.d("DJI", "3D NOT NULL")
//        }
//        val keyConnection = KeyTools.createKey(FlightControllerKey.KeyConnection)
//        val keyIsFlying = KeyTools.createKey(FlightControllerKey.KeyIsFlying)
//        val keyAreMotorsOn = KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn)
//        val keyFlightMode = KeyTools.createKey(FlightControllerKey.KeyFlightMode)
//        // 드론 상태 체크
//        Log.d("DJI", "Connection: ${keyConnection.get()}")
//        Log.d("DJI", "Is Flying: ${keyIsFlying.get()}")
//        Log.d("DJI", "Motors On: ${keyAreMotorsOn.get()}")
//        Log.d("DJI", "Flight Mode: ${keyFlightMode.get()}")
//
//
//        // 약간의 지연을 주어 시도
//        Handler(Looper.getMainLooper()).postDelayed({
//            if (keyConnection.get() == true) {
//                keyAircraftLocation3D.get(
//                    onSuccess = { location: LocationCoordinate3D? ->
//                        Log.d("DJI", "get 호출 성공: ${location?.altitude}")
//                        binding.tvAltitude.text = location?.altitude.toString()
//                        observeDroneAltitude()
//                    },
//                    onFailure = { error: IDJIError ->
//                        Log.e(
//                            "DJI",
//                            "get() 호출 실패: ${error.description()},${keyAircraftLocation3D.keyInfo}"
//                        )
//                        Log.e("DJI", "등록 여부 : ${SDKManager.getInstance().isRegistered}")
//                    }
//                )
//            }
//        }, 1000) // 1초 지연
//    }

    private fun checkAndStartTakeoff() {
        val keyConnection = KeyTools.createKey(FlightControllerKey.KeyConnection)
        val keyIsFlying = KeyTools.createKey(FlightControllerKey.KeyIsFlying)
        val keyStartTakeoff = KeyTools.createKey(FlightControllerKey.KeyStartTakeoff)

        // 상태 체크
        if (keyConnection.get() != true) {
            Log.e("DJI", "Flight Controller not connected")
            return
        }

        if (keyIsFlying.get() == true) {
            Log.e("DJI", "Drone is already flying")
            return
        }

        // Virtual Stick 모드 체크
        if (!isVirtualStickEnabled) {
            enableVirtualStickMode()
        }

        // 이륙 시작
        keyStartTakeoff.run {

            // 상태 체크
            if (keyConnection.get() != true) {
                Log.e("DJI", "Flight Controller not connected")
                return
            }

            if (keyIsFlying.get() == true) {
                Log.e("DJI", "Drone is already flying")
                return
            }

            // Virtual Stick 모드 체크
            if (!isVirtualStickEnabled) {
                enableVirtualStickMode()
            }

            KeyManager.getInstance().performAction(
                keyStartTakeoff,
                EmptyMsg(),
                object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                    override fun onSuccess(result: EmptyMsg?) {
                        Log.d("DJI", "Takeoff started successfully")
                        // 이륙 상태 모니터링 시작
                        startSendingControlData()
                        monitorTakeoffStatus()
                    }

                    override fun onFailure(error: IDJIError) {
                        Log.e("DJI", "Failed to start takeoff: ${error.description()}")
                    }
                }
            )
        }
    }
    private fun checkAndStopTakeoff() {
        val keyConnection = KeyTools.createKey(FlightControllerKey.KeyConnection)
        val keyIsFlying = KeyTools.createKey(FlightControllerKey.KeyIsFlying)
        val keyStopTakeOff = KeyTools.createKey(FlightControllerKey.KeyStopTakeoff)

        KeyManager.getInstance().listen(keyIsFlying, this) { _, isFlying ->
            if (isFlying == true) {
                Log.d("DJI", "Drone is now flying")
                // 이륙 성공 후 Virtual Stick 제어 시작
                isVirtualStickEnabled = true
                stopSendingControlData()
            }
            // Virtual Stick 모드 체크
            if (!isVirtualStickEnabled) {
                enableVirtualStickMode()
            }


        }
        if (keyConnection.get() != true) {
            Log.e("DJI", "Flight Controller not connected")
            return
        }

        if (keyIsFlying.get() == false) {
            Log.e("DJI", "Drone is not flying")
            return
        }
        keyStopTakeOff.run {

            KeyManager.getInstance().performAction(
                keyStopTakeOff,
                EmptyMsg(),
                object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                    override fun onSuccess(result: EmptyMsg?) {
                        Log.d("DJI", "Takeoff started successfully")
                        // 착륙 상태 모니터링 시작
                        monitorTakeoffStatus()
                    }

                    override fun onFailure(error: IDJIError) {
                        Log.e("DJI", "Failed to takeoff: ${error.errorType()}")
                    }
                }
            )
            // 상태 체크

        }
    }
    private fun monitorTakeoffStatus() {
        val keyIsFlying = KeyTools.createKey(FlightControllerKey.KeyIsFlying)
        val keyFlightMode = KeyTools.createKey(FlightControllerKey.KeyFlightMode)
        val keyAircraftLocation3D = KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D)
        KeyManager.getInstance().listen(keyIsFlying, this) { _, isFlying ->
            if (isFlying == true) {
                Log.d("DJI", "Drone is now flying")
                // 이륙 성공 후 Virtual Stick 제어 시작
                isVirtualStickEnabled = true
            }
        }

        KeyManager.getInstance().listen(keyFlightMode, this) { _, mode ->
            Log.d("DJI", "Flight mode: $mode")
        }
        KeyManager.getInstance().listen(keyAircraftLocation3D, this) {_, data ->
            Log.d("DJI", "now data: $data")
        }
    }

    // Virtual Stick 상태 리스너 설정
    private fun setVirtualStickStateListener() {
        VirtualStickManager.getInstance()
            .setVirtualStickStateListener(object : VirtualStickStateListener {
                override fun onVirtualStickStateUpdate(stickState: VirtualStickState) {
                    // Virtual Stick 상태 업데이트 로그
                    Log.d(
                        "VirtualStickState",
                        "Virtual Stick Enabled: ${stickState.isVirtualStickEnable}"
                    )
                    Log.d(
                        "VirtualStickState",
                        "Advanced Mode Enabled: ${stickState.isVirtualStickAdvancedModeEnabled}"
                    )
                }

                override fun onChangeReasonUpdate(reason: FlightControlAuthorityChangeReason) {
                }
            })
    }

    // Virtual Stick 모드를 활성화하는 함수
    private fun enableVirtualStickMode() {
        // 초기화
        VirtualStickManager.getInstance()
            .enableVirtualStick(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.d("Simulator", "Virtual Stick mode enabled successfully.")
                    isVirtualStickEnabled = true
                    // 성공시 AdvancedMode로 설정
                    VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true)
                    // Virtual Stick 활성화 후 주기적으로 명령 전송 시작
                    startSendingControlData()
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(
                        "Simulator",
                        "Failed to enable Virtual Stick mode: ${error.description()}"
                    )
                }
            })
    }

    // Virtual Stick 모드를 비활성화하는 함수
    private fun disableVirtualStickMode() {
        VirtualStickManager.getInstance()
            .disableVirtualStick(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.d("Simulator", "Virtual Stick mode disabled successfully.")
                    isVirtualStickEnabled = false
                    stopSendingControlData()  // Virtual Stick 비활성화 시 명령 전송 중지
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(
                        "Simulator",
                        "Failed to disable Virtual Stick mode: ${error.description()}"
                    )
                }
            })
    }

    // 명령 전송을 중지하는 함수
    private fun stopSendingControlData() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun startSendingControlData() {
        handler.post(object : Runnable {
            override fun run() {
                if (isVirtualStickEnabled) {
                    val param = VirtualStickFlightControlParam().apply {
                        pitch = 0.0  // 전진/후진 없음
                        roll = 0.0   // 좌우 이동 없음
                        yaw = 0.0    // 회전 없음
                        verticalThrottle = VERTICAL_THROTTLE  // 초당 0.1m 상승을 위한 스로틀 값
                    }

                    // 고도 값 누적 업데이트
                    val currentAltitude = flightControlVM.altitude.value ?: 0.0
                    flightControlVM.updateAltitude(currentAltitude + ALTITUDE_INCREMENT)

                    Log.d(
                        "Simulator",
                        "Increasing altitude by $ALTITUDE_INCREMENT per ${UPDATE_INTERVAL_MS}ms, verticalThrottle set to $VERTICAL_THROTTLE m/s"
                    )

                    // Virtual Stick 명령 전송
                    VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param)
                    // 200ms 간격으로 재호출
                    handler.postDelayed(this, UPDATE_INTERVAL_MS.toLong())
                }
            }
        })
    }


    private fun adjustAltitudeBy(altitude: Double) {
        verticalThrottle = altitude
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        VirtualStickManager.getInstance()
            .disableVirtualStick(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    disableVirtualStickMode()  // 시뮬레이터 비활성화 시 Virtual Stick 모드도 비활성화
                }

                override fun onFailure(error: IDJIError) {
                }
            })
    }
}
