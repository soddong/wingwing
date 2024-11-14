package com.shieldrone.station.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.shieldrone.station.constant.FlightContstant.Companion.FLIGHT_CONTROL_TAG
import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.controller.TrackingTargetController
import com.shieldrone.station.data.Position
import com.shieldrone.station.databinding.FlightControlActivityBinding
import com.shieldrone.station.model.FlightControlVM
import com.shieldrone.station.model.TrackingTargetViewModel
import com.shieldrone.station.service.route.RouteAdapter

class FlightControlActivity : AppCompatActivity() {

    private lateinit var binding: FlightControlActivityBinding
    private val flightControlVM: FlightControlVM by viewModels()
    private lateinit var routeAdapter: RouteAdapter
    private lateinit var routeController: RouteController
    private lateinit var trackingController: TrackingTargetController
    private val trackingVM: TrackingTargetViewModel by viewModels()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FlightControlActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val routeListener = object : RouteAdapter.RouteListener {
            override fun onRouteUpdate(latitude: Double, longitude: Double, altitude: Double) {
                val position = Position(latitude = latitude, longitude = longitude, altitude = 1.2)
                Log.d(FLIGHT_CONTROL_TAG, "Updated Route to: $latitude, $longitude")
                binding.txtTargetLocation.text = "사용자 위도: ${position.latitude}, 경도: ${position.longitude}"
            }
        }

        routeAdapter = RouteAdapter(routeListener)
        routeController = RouteController(routeAdapter)
        routeController.startReceivingLocation()

        trackingController = TrackingTargetController(trackingVM)

        initUiElements()
        observeData()
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingController.stopReceivingData()
    }
    private fun initUiElements() {
        initButtonClickListeners()
    }

    private fun initButtonClickListeners() {
        binding.btnTakeOff.setOnClickListener { flightControlVM.startTakeOff() }
        binding.btnLand.setOnClickListener { flightControlVM.startLanding() }
        binding.btnEnableVirtualStick.setOnClickListener { flightControlVM.enableVirtualStickMode() }
        binding.btnDisableVirtualStick.setOnClickListener { flightControlVM.disableVirtualStickMode() }
        binding.btnGoToHome.setOnClickListener{ flightControlVM.startReturnToHome() }
        binding.btnSetHome.setOnClickListener{ flightControlVM.setHomeLocation() }
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

        flightControlVM.goHomeState.observe(this) { message ->
            binding.txtGoHomeMessage.text = message.toString()
        }

        flightControlVM.homeLocation.observe(this) { message ->
            binding.txtHomeLocation.text = message.toString()
        }

        trackingVM.trackingData.asLiveData().observe(this){message ->
            binding.txtTargetLocationInFrame.text = message.toString()
        }

    }

}
