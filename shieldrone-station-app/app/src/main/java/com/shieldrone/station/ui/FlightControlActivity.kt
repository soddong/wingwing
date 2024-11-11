package com.shieldrone.station.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.shieldrone.station.databinding.FlightControlActivityBinding
import com.shieldrone.station.model.FlightControlVM
import com.shieldrone.station.service.route.RouteAdapter
import com.shieldrone.station.service.route.RouteListener

class FlightControlActivity : AppCompatActivity(), RouteListener {

    private lateinit var binding: FlightControlActivityBinding
    private val flightControlVM: FlightControlVM by viewModels()
    private lateinit var routeAdapter: RouteAdapter

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FlightControlActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RouteAdapter 초기화 및 Listener 설정
        // RouteListener 구현
        val routeListener = object : RouteAdapter.RouteListener {
            override fun onRouteUpdate(latitude: Double, longitude: Double) {
                // 위치 업데이트에 대한 작업 수행
                Log.i("FlightControlActivity", "Updated Route to: $latitude, $longitude")
            }
        }
        routeAdapter = RouteAdapter(routeListener)
        subscribeDroneValues()
        initUiElements()
        observeData()
    }

    override fun onRouteProcessed(latitude: Double, longitude: Double) {
        flightControlVM.setTargetLocation(latitude, longitude)
    }

    private fun subscribeDroneValues() {
        flightControlVM.subscribeDroneLocation()
        flightControlVM.subscribeDroneControlValues()
        flightControlVM.subscribeDronePositionValues()
    }


    private fun initUiElements() {
        initButtonClickListeners()
        initTextViews()
    }

    private fun initButtonClickListeners() {
        binding.btnTakeOff.setOnClickListener { flightControlVM.startTakeOff() }
        binding.btnLand.setOnClickListener { flightControlVM.startLanding() }
        binding.btnEnableVirtualStick.setOnClickListener { flightControlVM.enableVirtualStickMode() }
        binding.btnDisableVirtualStick.setOnClickListener { flightControlVM.disableVirtualStickMode() }
        binding.btnMoveForward.setOnClickListener { flightControlVM.moveForward() }
        binding.btnMoveBackward.setOnClickListener { flightControlVM.moveBackward() }
        binding.btnMoveUp.setOnClickListener { flightControlVM.moveUp() }
        binding.btnMoveDown.setOnClickListener { flightControlVM.moveDown() }
        binding.btnMoveLeft.setOnClickListener { flightControlVM.moveLeft() }
        binding.btnMoveRight.setOnClickListener { flightControlVM.moveRight() }
        binding.btnRotateLeft.setOnClickListener { flightControlVM.rotateLeft() }
        binding.btnRotateRight.setOnClickListener { flightControlVM.rotateRight() }
        binding.btnInitValue.setOnClickListener { flightControlVM.initVirtualStickValue() }
        binding.btnMoveToTarget.setOnClickListener { flightControlVM.moveToTarget() }
    }

    private fun initTextViews() {
        binding.txtDroneStatus.text = "Drone Status"
        binding.txtDroneControls.text = "Drone Controls"
        binding.txtDronePosition.text = "Drone Position"
        binding.txtMessage.text = "Message"
        binding.txtGpsLevel.text = "GPS Level"
    }

    private fun observeData() {
        // 드론 상태 관찰
        flightControlVM.droneState.observe(this, Observer { state ->
            val statusText = """
                롤: ${state.roll}
                요: ${state.yaw}
                피치: ${state.pitch}
                속도(X): ${state.xVelocity}
                속도(Y): ${state.yVelocity}
                속도(Z): ${state.zVelocity}
                나침반 방향: ${state.compassHeading}
            """.trimIndent()
            binding.txtDroneStatus.text = statusText
        })

        // 메시지 관찰
        flightControlVM.message.observe(this) { message ->
            binding.txtMessage.text = message
            Log.d("FlightControlActivity", message)
        }

        // 드론 제어 정보 관찰
        flightControlVM.droneControls.observe(this) { control ->
            val statusText = """
                leftStick (VERT-고도): ${control.leftStick.verticalPosition}
                leftStick (HORI-좌우회전): ${control.leftStick.horizontalPosition}
                rightStick (VERT-앞뒤): ${control.rightStick.verticalPosition}
                rightStick (HORI-좌우이동): ${control.rightStick.horizontalPosition}
            """.trimIndent()
            binding.txtDroneControls.text = statusText
        }

        // 드론 위치 정보 관찰
        flightControlVM.dronePosition.observe(this) { position ->
            val positionText = """
            위도: ${position.latitude}
            경도: ${position.longitude}
            고도: ${position.altitude}
        """.trimIndent()
            binding.txtDronePosition.text = positionText
        }

        // GPS 신호 레벨 관찰
        flightControlVM.gpsSignalLevel.observe(this) { gpsLevel ->

            binding.txtGpsLevel.text = "GPS Signal Level: $gpsLevel"
        }
    }


}
