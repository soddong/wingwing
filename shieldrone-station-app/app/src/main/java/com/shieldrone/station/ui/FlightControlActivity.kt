package com.shieldrone.station.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.shieldrone.station.R
import com.shieldrone.station.model.FlightControlVM

class FlightControlActivity : AppCompatActivity() {

    private val flightControlVM: FlightControlVM by viewModels()

    private lateinit var btnTakeOff: Button
    private lateinit var btnLand: Button
    private lateinit var btnMoveForward: Button
    private lateinit var btnMoveBackward: Button
    private lateinit var btnMoveLeft: Button
    private lateinit var btnMoveRight: Button
    private lateinit var btnMoveUp: Button
    private lateinit var btnMoveDown: Button

    private lateinit var btnEnableVirtualStick: Button
    private lateinit var btnDisableVirtualStick: Button
    private lateinit var txtDroneStatus: TextView
    private lateinit var txtDroneControls: TextView
    private lateinit var txtDronePosition: TextView
    private lateinit var txtMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flight_control)

        // UI 요소 초기화
        btnTakeOff = findViewById(R.id.btnTakeOff)
        btnLand = findViewById(R.id.btnLand)
        btnEnableVirtualStick = findViewById(R.id.btnEnableVirtualStick)
        btnDisableVirtualStick = findViewById(R.id.btnDisableVirtualStick)
        txtDroneStatus = findViewById(R.id.txtDroneStatus)
        txtDroneControls = findViewById(R.id.txtDroneControls)
        txtDronePosition = findViewById(R.id.txtDronePosition)
        txtMessage = findViewById(R.id.txtMessage)
        btnMoveForward = findViewById(R.id.btnMoveForward)
        btnMoveBackward = findViewById(R.id.btnMoveBackward)
        btnMoveLeft = findViewById(R.id.btnMoveLeft)
        btnMoveRight = findViewById(R.id.btnMoveRight)
        btnMoveUp = findViewById(R.id.btnMoveUp)
        btnMoveDown = findViewById(R.id.btnMoveDown)

        // 버튼 클릭 리스너 설정
        btnTakeOff.setOnClickListener {
            flightControlVM.startTakeOff()
        }

        btnLand.setOnClickListener {
            flightControlVM.startLanding()
        }

        btnMoveForward.setOnClickListener {
            flightControlVM.moveForward()
        }

        btnMoveBackward.setOnClickListener {
            flightControlVM.moveBackward()
        }

        btnMoveUp.setOnClickListener {
            flightControlVM.moveUp()
        }

        btnMoveDown.setOnClickListener {
            flightControlVM.moveDown()
        }

        btnMoveLeft.setOnClickListener {
            flightControlVM.moveLeft()
        }

        btnMoveRight.setOnClickListener {
            flightControlVM.moveRight()
        }

        btnEnableVirtualStick.setOnClickListener {
            flightControlVM.enableVirtualStickMode()
        }

        btnDisableVirtualStick.setOnClickListener {
            flightControlVM.disableVirtualStickMode()
        }

        // 드론 상태 관찰
        flightControlVM.droneState.observe(this, Observer { state ->
            val statusText = """
                위도: ${state.latitude}
                경도: ${state.longitude}
                고도: ${state.altitude}
                속도(X): ${state.xVelocity}
                속도(Y): ${state.yVelocity}
                속도(Z): ${state.zVelocity}
                나침반 방향: ${state.compassHeading}
            """.trimIndent()
            txtDroneStatus.text = statusText
        })

        // 메시지 관찰
        flightControlVM.message.observe(this, Observer { message ->
            txtMessage.text = message
            Log.d("FlightControlActivity", message)
        })

        // 드론 제어 정보 관찰
        flightControlVM.droneControls.observe(this, Observer { control ->
            val statusText = """
                leftStick (VERT-고도): ${control.leftStick.verticalPosition}
                leftStick (HORI-좌우회전): ${control.leftStick.horizontalPosition}
                rightStick (VERT-앞뒤): ${control.rightStick.verticalPosition}
                rightStick (HORI-좌우이동): ${control.rightStick.horizontalPosition}
            """.trimIndent()
            txtDroneControls.text = statusText
        })

        // 드론 위치 정보 관찰
        flightControlVM.dronePosition.observe(this, Observer { position ->
            val positionText = """
            위도: ${position.latitude}
            경도: ${position.longitude}
            고도: ${position.altitude}
        """.trimIndent()
            txtDronePosition.text = positionText
        })

        // 드론 위치/제어 정보 구독 시작
        flightControlVM.subscribeDroneLocation()
        flightControlVM.subscribeDroneControlValues()
        flightControlVM.subscribeDronePositionValues()
    }
}
