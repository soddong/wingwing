package com.shieldrone.station.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.shieldrone.station.constant.FlightContstant.Companion.EARTH_RADIUS
import com.shieldrone.station.constant.FlightContstant.Companion.FLIGHT_CONTROL_TAG
import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.databinding.FlightControlActivityBinding
import com.shieldrone.station.model.FlightControlVM
import com.shieldrone.station.service.route.RouteAdapter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

        // RouteAdapter 초기화 및 Listener 설정
        // RouteListener 구현
        val routeListener = object : RouteAdapter.RouteListener {
            override fun onRouteUpdate(latitude: Double, longitude: Double) {
                flightControlVM.setTargetLocation(latitude, longitude)
                Log.d(FLIGHT_CONTROL_TAG, "Updated Route to: $latitude, $longitude")
            }
        }
        routeAdapter = RouteAdapter(routeListener)
        routeController = RouteController(routeAdapter)
        subscribeDroneValues()
        initUiElements()
        observeData()
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
        // 버튼 클릭 시 위치 수신 시작
        binding.btnGetTargetLocation.setOnClickListener {
            routeController.startReceivingLocation()
            Log.d(FLIGHT_CONTROL_TAG, "Started receiving location updates")
        }
    }

    private fun initTextViews() {
        binding.txtDroneStatus.text = "Drone Status"
        binding.txtDroneControls.text = "Drone Controls"
        binding.txtDronePosition.text = "Drone Position"
        binding.txtMessage.text = "Message"
        binding.txtGpsLevel.text = "GPS Level"
        binding.txtTargetLocation.text = "타겟 위치 표시"
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
        // 목표 위도와 경도 관찰 및 거리 업데이트
        flightControlVM.targetLat.observe(this) {
            Log.d("FlightControlActivity", "targetLat updated: $it")
            updateTargetLocation()
        }

        flightControlVM.targetLng.observe(this) {
            Log.d("FlightControlActivity", "targetLng updated: $it")
            updateTargetLocation()
        }

    }

    @SuppressLint("DefaultLocale")
    private fun updateTargetLocation() {
        val currentLat = flightControlVM.dronePosition.value?.latitude
        val currentLng = flightControlVM.dronePosition.value?.longitude
        val targetLat = flightControlVM.targetLat.value
        val targetLng = flightControlVM.targetLng.value

        if (currentLat != null && currentLng != null && targetLat != null && targetLng != null) {
            val distance = calculateDistance(currentLat, currentLng, targetLat, targetLng)
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

    // 두 지점 간의 거리를 미터 단위로 계산
    private fun calculateDistance(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Double {


        val dLat = Math.toRadians(endLat - startLat)
        val dLng = Math.toRadians(endLng - startLng)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
                sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }


}
