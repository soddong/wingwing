package com.shieldrone.station.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.shieldrone.station.constant.FlightContstant.Companion.SIMULATOR_TAG
import com.shieldrone.station.databinding.SimulatorActivityBinding
import com.shieldrone.station.model.SimulatorVM


class SimulatorActivity : AppCompatActivity() {

    private lateinit var binding: SimulatorActivityBinding
    private val simulatorVM: SimulatorVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SimulatorActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // UI 요소 초기화
        initUiElements()
        observeData()

        simulatorVM.subscribeDroneControlValues()
        simulatorVM.subscribeDroneLocation()
        simulatorVM.subscribeDronePositionValues()

    }

    private fun initButtonClickListeners() {
        binding.btnTakeOff.setOnClickListener { simulatorVM.startTakeOff() }
        binding.btnLand.setOnClickListener { simulatorVM.startLanding() }
        binding.btnEnableVirtualStick.setOnClickListener { simulatorVM.enableVirtualStickMode() }
        binding.btnDisableVirtualStick.setOnClickListener { simulatorVM.disableVirtualStickMode() }
//        binding.btnMoveForward.setOnClickListener { simulatorVM.moveForward() }
//        binding.btnMoveBackward.setOnClickListener { simulatorVM.moveBackward() }
        binding.btnMoveUp.setOnClickListener { simulatorVM.moveUp() }
        binding.btnMoveDown.setOnClickListener { simulatorVM.moveDown() }
        binding.btnMoveLeft.setOnClickListener { simulatorVM.moveLeft() }
        binding.btnMoveRight.setOnClickListener { simulatorVM.moveRight() }
        binding.btnRotateLeft.setOnClickListener { simulatorVM.rotateLeft() }
        binding.btnRotateRight.setOnClickListener { simulatorVM.rotateRight() }
        binding.btnInitValue.setOnClickListener { simulatorVM.initVirtualStickValue() }
        binding.btnEnableSimulator.setOnClickListener { simulatorVM.enableSimulatorMode() }
        binding.btnDisableSimulator.setOnClickListener { simulatorVM.disableSimulatorMode() }
        binding.btnMoveToTarget.setOnClickListener {
            simulatorVM.moveToTarget()
        }
        // 기타 버튼들에 대한 리스너 설정
        binding.btnAdjustYaw.setOnClickListener {
            val targetBearing = 90.0  // 목표 방위각 (예시)
            simulatorVM.adjustYawToTarget(targetBearing)
        }
    }

    private fun initTextViews() {
        binding.txtMessage.text = "Message"
        binding.txtSimulatorState.text = "SimulatorState"
        binding.txtSimulatorControls.text = "SimulatorControls"

    }

    private fun observeData() {
        // 드론 상태 관찰
        simulatorVM.simulState.observe(this, Observer { state ->
            val statusText = """
                롤: ${state.roll}
                요: ${state.yaw}
                피치: ${state.pitch}
                속도(X): ${state.xVelocity}
                속도(Y): ${state.yVelocity}
                속도(Z): ${state.zVelocity}
                나침반 방향: ${state.compassHeading}
            """.trimIndent()
            binding.txtSimulatorState.text = statusText
        })

        // 메시지 관찰
        simulatorVM.message.observe(this) { message ->
            binding.txtMessage.text = message
            Log.d(SIMULATOR_TAG, message)
        }

        // 드론 제어 정보 관찰
        simulatorVM.simulControls.observe(this) { control ->
            val statusText = """
                leftStick (VERT-고도): ${control.leftStick.verticalPosition}
                leftStick (HORI-좌우회전): ${control.leftStick.horizontalPosition}
                rightStick (VERT-앞뒤): ${control.rightStick.verticalPosition}
                rightStick (HORI-좌우이동): ${control.rightStick.horizontalPosition}
            """.trimIndent()
            binding.txtSimulatorControls.text = statusText
        }
        simulatorVM.simulPositions.observe(this) { position ->
            val positionText = """
                위도: ${position.latitude}
                경도: ${position.longitude}
                고도: ${position.altitude}
            """.trimIndent()
            binding.txtSimulatorPosition.text = positionText
        }


        // GPS 신호 레벨 관찰
    }

    private fun initUiElements() {
        initButtonClickListeners()
        initTextViews()
    }

}