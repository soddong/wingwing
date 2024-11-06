package com.shieldrone.station.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.shieldrone.station.R
import com.shieldrone.station.databinding.FragmentSimulatorBinding
import com.shieldrone.station.model.SimulatorVM
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.simulator.InitializationSettings
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickState
import dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener


class SimulatorActivity : AppCompatActivity(R.layout.fragment_simulator) {

    private lateinit var binding: FragmentSimulatorBinding
    private val simulatorVM: SimulatorVM by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var verticalThrottle = 0.0  // Z축 이동을 위한 스로틀 값
    private var isVirtualStickEnabled = false

    companion object {
        var throttle = 1.0
        const val OPERATION_SIMULATOR = "simulator"
        const val VALUE_ALTITUDE = "altitude"
    }

    // Virtual Stick 모드를 활성화하는 함수
    private fun enableVirtualStickMode() {
        // 초기화
        VirtualStickManager.getInstance().init()
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

    // Virtual Stick 상태 리스너 설정
    private fun setVirtualStickStateListener() {
        VirtualStickManager.getInstance()
            .setVirtualStickStateListener(object : VirtualStickStateListener {
                override fun onVirtualStickStateUpdate(stickState: VirtualStickState) {
                    // Virtual Stick 상태 업데이트 로그
                    Log.d(
                        "VirtualStickState",
                        "Virtual Stick Enabled: ${stickState.isVirtualStickEnable()}"
                    )
                    Log.d(
                        "VirtualStickState",
                        "Advanced Mode Enabled: ${stickState.isVirtualStickAdvancedModeEnabled()}"
                    )
                }

                override fun onChangeReasonUpdate(reason: FlightControlAuthorityChangeReason) {
                }
            })
    }

    // 일정 간격으로 Z축 이동 명령을 전송하는 함수
    private fun startSendingControlData() {
        handler.post(object : Runnable {
            override fun run() {
                if (isVirtualStickEnabled) {
                    val param = VirtualStickFlightControlParam().apply {
                        pitch = 0.0  // 전진/후진 없음
                        roll = 0.0   // 좌우 이동 없음
                        yaw = 0.0    // 회전 없음
                        verticalThrottle = this@SimulatorActivity.verticalThrottle  // Z축 이동 설정
                    }
                    // 고도 계산 및 업데이트
                    val altitudeChange = verticalThrottle // 1000ms 마다 업데이트
                    simulatorVM.updateAltitude(altitudeChange)
                    Log.d("Simulator", "Sending verticalThrottle value: $verticalThrottle")
                    VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param)

                    handler.postDelayed(this, 1000)  // 1000ms (25Hz) 간격으로 명령 전송
                }
            }
        })
    }

    // 명령 전송을 중지하는 함수
    private fun stopSendingControlData() {
        handler.removeCallbacksAndMessages(null)
    }

    // Z축 이동 값을 설정하는 함수 (상승/하강)
    private fun adjustAltitudeBy(deltaZ: Double) {
        verticalThrottle = deltaZ
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentSimulatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setVirtualStickStateListener()
        simulatorVM.altitude.observe(this) { altitude ->
            binding.tvSimulatorAltitude.text = getString(R.string.view_value,VALUE_ALTITUDE,altitude.toString())
        }
        // Z축 상승 버튼 클릭 리스너
        binding.btnZAxisUp.setOnClickListener {
            adjustAltitudeBy(this.verticalThrottle + throttle) // 1.0m/s 상승
        }

        // Z축 하강 버튼 클릭 리스너
        binding.btnZAxisDown.setOnClickListener {
            adjustAltitudeBy(this.verticalThrottle - throttle) // 1.0m/s 하강
        }

        // 시뮬레이터 활성화 버튼 클릭 리스너
        binding.btnEnableSimulator.setOnClickListener {
            val location = LocationCoordinate2D(37.4219983, -122.084)
            val settings = InitializationSettings.createInstance(location, 10)

            simulatorVM.enableSimulator(settings, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    binding.tvSimulatorConnectionStatus.text = getString(R.string.operation_success,
                        OPERATION_SIMULATOR)
                    enableVirtualStickMode()
                }

                override fun onFailure(error: IDJIError) {
                    binding.tvSimulatorConnectionStatus.text =
                    getString(R.string.operation_failed, OPERATION_SIMULATOR, error.description())
                }
            })
        }

        // 시뮬레이터 비활성화 버튼 클릭 리스너
        binding.btnDisableSimulator.setOnClickListener {
            simulatorVM.disableSimulator(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    disableVirtualStickMode()  // 시뮬레이터 비활성화 시 Virtual Stick 모드도 비활성화
                    binding.tvSimulatorConnectionStatus.text = getString(R.string.operation_disable)
                }

                override fun onFailure(error: IDJIError) {
                    binding.tvSimulatorConnectionStatus.text =
                        getString(R.string.operation_failed,"DISABLE",error.description())
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSendingControlData()  // Activity가 종료될 때 명령 전송 중지
        disableVirtualStickMode()  // Activity가 종료될 때 Virtual Stick 모드 비활성화
        handler.removeCallbacksAndMessages(null)
    }
}