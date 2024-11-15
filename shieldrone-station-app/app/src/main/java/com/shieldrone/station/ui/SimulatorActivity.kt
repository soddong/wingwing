package com.shieldrone.station.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.shieldrone.station.constant.FlightContstant.Companion.SIMULATOR_TAG
import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.data.Position
import com.shieldrone.station.databinding.SimulatorActivityBinding
import com.shieldrone.station.model.SimulatorVM
import com.shieldrone.station.service.route.RouteAdapter

class SimulatorActivity : AppCompatActivity() {

    private lateinit var binding: SimulatorActivityBinding
    private val simulatorVM: SimulatorVM by viewModels()
    private lateinit var routeAdapter: RouteAdapter
    private lateinit var routeController: RouteController

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SimulatorActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val routeListener = object : RouteAdapter.RouteListener {
//            override fun onRouteUpdate(latitude: Double, longitude: Double, altitude: Double) {
//                val position = Position(latitude = latitude, longitude = longitude, altitude = 1.2)
//                simulatorVM.addTargetPosition(position)
//                Log.d(SIMULATOR_TAG, "Updated Route to: $latitude, $longitude, altitude: 1.2")
//            }
//        }
//        routeAdapter = RouteAdapter(routeListener)
//        routeController = RouteController(routeAdapter)
//        routeController.startReceivingLocation()

        initUiElements()
        observeData()
    }

    private fun initUiElements() {
        initButtonClickListeners()
    }

    private fun initButtonClickListeners() {
        binding.btnSimulatorTakeOff.setOnClickListener { simulatorVM.startTakeOff() }
        binding.btnSimulatorLand.setOnClickListener { simulatorVM.startLanding() }
        binding.btnSimulatorEnableVirtualStick.setOnClickListener { simulatorVM.enableVirtualStickMode() }
        binding.btnSimulatorDisableVirtualStick.setOnClickListener { simulatorVM.disableVirtualStickMode() }
        binding.btnEnableSimulator.setOnClickListener { simulatorVM.enableSimulatorMode() }
        binding.btnDisableSimulator.setOnClickListener { simulatorVM.disableSimulatorMode() }
        binding.btnSimulatorMoveUp.setOnClickListener { simulatorVM.moveUp() }
        binding.btnSimulatorMoveDown.setOnClickListener { simulatorVM.moveDown() }
        binding.btnSimulatorMoveLeft.setOnClickListener { simulatorVM.moveLeft() }
        binding.btnSimulatorMoveRight.setOnClickListener { simulatorVM.moveRight() }
        binding.btnSimulatorRotateLeft.setOnClickListener { simulatorVM.rotateLeft() }
        binding.btnSimulatorRotateRight.setOnClickListener { simulatorVM.rotateRight() }
        binding.btnSimulatorInitValue.setOnClickListener { simulatorVM.initVirtualStickValue() }

    }

    @SuppressLint("SetTextI18n")
    private fun observeData() {
        // 드론 상태 관찰
        simulatorVM.droneState.observe(this, Observer { state ->

            val statusText = """
                롤: ${state.roll}
                요: ${state.yaw}
                피치: ${state.pitch}
                속도(X): ${state.xVelocity}
                속도(Y): ${state.yVelocity}
                속도(Z): ${state.zVelocity}
                lat: ${state.latitude}
                lng: ${state.longitude}
                alt: ${state.altitude}
            """.trimIndent()
            binding.txtSimulatorState.text = statusText
        })

        // 메시지 관찰
        simulatorVM.message.observe(this) { message ->
            binding.txtSimulatorMessage.text = message
        }

        // 드론 제어 정보 관찰
        simulatorVM.droneControls.observe(this) { control ->
            val statusText = """
                leftStick (고도): ${control.leftStick.verticalPosition}
                leftStick (좌우회전): ${control.leftStick.horizontalPosition}
                rightStick (앞뒤): ${control.rightStick.verticalPosition}
                rightStick (좌우이동): ${control.rightStick.horizontalPosition}
            """.trimIndent()
            binding.txtSimulatorControls.text = statusText
        }

        // GPS 신호 레벨 관찰
        simulatorVM.gpsSignalLevel.observe(this) { gpsLevel ->
            binding.txtSimulatorGpsLevel.text = "GPS Signal Level: $gpsLevel"
        }

        simulatorVM.targetPosition.observe(this) {
            updateTargetLocation()
        }
        simulatorVM.targetDistance.observe(this) { distance ->
            binding.txtSimulatorDistance.text = "거리: $distance"
        }
//        1. targetLocationQueue가 비어있지 않은지 확인
//        2. 비어있지 않다면 moveToTarget() 실행-> 종료될때까지 대기
//        3. 종료되면 다시 큐에서 하나 꺼내서 실행
    }

    @SuppressLint("DefaultLocale")
    private fun updateTargetLocation() {
        val currentLat = simulatorVM.droneState.value?.latitude
        val currentLng = simulatorVM.droneState.value?.longitude
        val targetPosition = simulatorVM.targetPosition.value
        val targetLat = targetPosition?.latitude
        val targetLng = targetPosition?.longitude

        if (currentLat != null && currentLng != null && targetLat != null && targetLng != null) {
            val distance = simulatorVM.calculateDistanceAndBearing(
                currentLat,
                currentLng,
                targetLat,
                targetLng
            ).first
            val targetLocationText = """
                목표 위도: $targetLat
                목표 경도: $targetLng
                거리: ${String.format("%.2f", distance)} m
            """.trimIndent()
            binding.txtSimulatorTargetLocation.text = targetLocationText
        } else if (targetLat != null && targetLng != null) {
            val targetLocationText = """
                목표 위도: $targetLat
                목표 경도: $targetLng
            """.trimIndent()
            binding.txtSimulatorTargetLocation.text = targetLocationText
        }
    }
}
