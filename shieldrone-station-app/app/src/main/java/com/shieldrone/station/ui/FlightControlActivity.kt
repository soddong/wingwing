package com.shieldrone.station.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.shieldrone.station.R
import com.shieldrone.station.databinding.FragmentSimulatorBinding
import com.shieldrone.station.model.FlightControlVM
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager

class FlightControlActivity : AppCompatActivity(R.layout.activity_flight_control) {

    private lateinit var binding: FragmentSimulatorBinding
    private var isVirtualStickEnabled = false
    private val flightControlVM: FlightControlVM by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var verticalThrottle = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flight_control)

        // 고도 관찰 및 UI 업데이트
        flightControlVM.altitude.observe(this) { altitude ->
            findViewById<TextView>(R.id.tvAltitude).text = "Altitude: $altitude"
        }

        // Z축 상승 버튼 클릭 리스너
        findViewById<Button>(R.id.btnZAxisUp).setOnClickListener {
            adjustAltitudeBy(0.1) // 1.0m/s 상승
        }

        // Z축 하강 버튼 클릭 리스너
        findViewById<Button>(R.id.btnZAxisDown).setOnClickListener {
            adjustAltitudeBy(-0.1) // 1.0m/s 하강
        }

        startSendingControlData()
    }

    // 명령 전송을 중지하는 함수
    private fun stopSendingControlData() {
        handler.removeCallbacksAndMessages(null)
    }

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

    // Virtual Stick 모드를 활성화하고 명령 전송을 주기적으로 실행
    private fun startSendingControlData() {
        handler.post(object : Runnable {
            override fun run() {
                if (isVirtualStickEnabled) {
                    val param = VirtualStickFlightControlParam().apply {
                        pitch = 0.0  // 전진/후진 없음
                        roll = 0.0   // 좌우 이동 없음
                        yaw = 0.0    // 회전 없음
                        verticalThrottle = this@FlightControlActivity.verticalThrottle  // Z축 이동 설정
                    }
                    // 고도 계산 및 업데이트
                    val altitudeChange = verticalThrottle * 0.2 // 200ms 마다 업데이트되므로 초당 변화량 조정
                    flightControlVM.updateAltitude(altitudeChange)
                    Log.d("Simulator", "Sending verticalThrottle value: $verticalThrottle")
                    VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param)

                    handler.postDelayed(this, 200)  // 200ms (5Hz) 간격으로 명령 전송
                }
            }
        })
    }

    private fun adjustAltitudeBy(deltaZ: Double) {
        verticalThrottle = deltaZ
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        VirtualStickManager.getInstance()
            .disableVirtualStick(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    disableVirtualStickMode()  // 시뮬레이터 비활성화 시 Virtual Stick 모드도 비활성화
                    binding.tvSimulatorConnectionStatus.text = "Simulator disabled successfully."
                }

                override fun onFailure(error: IDJIError) {
                    binding.tvSimulatorConnectionStatus.text =
                        "Failed to disable simulator: ${error.description()}"
                }
            })
    }
}
