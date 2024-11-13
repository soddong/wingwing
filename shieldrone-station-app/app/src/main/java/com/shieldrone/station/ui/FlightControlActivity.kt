package com.shieldrone.station.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.shieldrone.station.constant.FlightContstant.Companion.FLIGHT_CONTROL_TAG
import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.data.Position
import com.shieldrone.station.databinding.FlightControlActivityBinding
import com.shieldrone.station.model.FlightControlVM
import com.shieldrone.station.service.route.RouteAdapter

class FlightControlActivity : AppCompatActivity() {

    private lateinit var binding: FlightControlActivityBinding
    private val flightControlVM: FlightControlVM by viewModels()
    private lateinit var routeAdapter: RouteAdapter
    private lateinit var routeController: RouteController

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FlightControlActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val routeListener = object : RouteAdapter.RouteListener {
            override fun onRouteUpdate(latitude: Double, longitude: Double, altitude: Double) {
                val position = Position(latitude = latitude, longitude = longitude, altitude = 1.2)
                flightControlVM.addTargetPosition(position)
                Log.d(FLIGHT_CONTROL_TAG, "Updated Route to: $latitude, $longitude")
            }
        }
        routeAdapter = RouteAdapter(routeListener)
        routeController = RouteController(routeAdapter)
        routeController.startReceivingLocation()

        initUiElements()
        observeData()
    }

    private fun initUiElements() {
        initButtonClickListeners()
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
    }

    @SuppressLint("SetTextI18n")
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
                lat: ${state.latitude}
                lng: ${state.longitude}
                alt: ${state.altitude}
            """.trimIndent()
            binding.txtDroneState.text = statusText
        })

        // 메시지 관찰
        flightControlVM.message.observe(this) { message ->
            binding.txtDroneMessage.text = message
        }

        // 드론 제어 정보 관찰
        flightControlVM.droneControls.observe(this) { control ->
            val statusText = """
                leftStick (고도): ${control.leftStick.verticalPosition}
                leftStick (좌우회전): ${control.leftStick.horizontalPosition}
                rightStick (앞뒤): ${control.rightStick.verticalPosition}
                rightStick (좌우이동): ${control.rightStick.horizontalPosition}
            """.trimIndent()
            binding.txtDroneControls.text = statusText
        }

        // GPS 신호 레벨 관찰
        flightControlVM.gpsSignalLevel.observe(this) { gpsLevel ->
            binding.txtGpsLevel.text = "GPS Signal Level: $gpsLevel"
        }
        flightControlVM.targetPosition.observe(this) {
            updateTargetLocation()
        }

    }

    @SuppressLint("DefaultLocale")
    private fun updateTargetLocation() {
        val currentLat = flightControlVM.droneState.value?.latitude
        val currentLng = flightControlVM.droneState.value?.longitude
        val targetPosition = flightControlVM.targetPosition.value
        val targetLat = targetPosition?.latitude
        val targetLng = targetPosition?.longitude

        if (currentLat != null && currentLng != null && targetLat != null && targetLng != null) {
            val distance = flightControlVM.calculateDistanceAndBearing(
                currentLat,
                currentLng,
                targetLat,
                targetLng
            ).first
            val targetLocationText = """
            목표 위도: $targetLat
            목표 경도: $targetLng
            거리: ${String.format("%.2f", distance)} 
        """.trimIndent()
            binding.txtTargetLocation.text = targetLocationText
        } else if (targetLat != null && targetLng != null) {
            val targetLocationText = """
            목표 위도: $targetLat
            목표 경도: $targetLng
        """.trimIndent()
            binding.txtTargetLocation.text = targetLocationText
        }
    }
}
