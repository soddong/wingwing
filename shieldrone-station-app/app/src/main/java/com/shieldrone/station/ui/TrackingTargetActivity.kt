package com.shieldrone.station.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shieldrone.station.constant.FlightContstant.Companion.FLIGHT_CONTROL_TAG
import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.controller.TrackingTargetController
import com.shieldrone.station.data.Position
import com.shieldrone.station.model.FlightAutoControlVM
import com.shieldrone.station.model.TrackingTargetViewModel
import com.shieldrone.station.service.route.RouteAdapter
import kotlin.math.abs
import kotlin.math.sign

class TrackingTargetActivity : ComponentActivity() {
    private val trackingVM: TrackingTargetViewModel by viewModels()
    private lateinit var trackingController: TrackingTargetController
    private val flightControlVM: FlightAutoControlVM by viewModels() // FlightControlVM 추가
    private lateinit var routeAdapter: RouteAdapter
    private lateinit var routeController: RouteController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trackingController = TrackingTargetController(trackingVM)
        setContent {
            TrackingTargetScreen(trackingVM, trackingController, flightControlVM)
        }
        val routeListener = object : RouteAdapter.RouteListener {
            override fun onRouteUpdate(latitude: Double, longitude: Double, altitude: Double) {
                val position = Position(latitude = latitude, longitude = longitude, altitude = 1.2)
                Log.d(FLIGHT_CONTROL_TAG, "Updated Route to: $latitude, $longitude")
                flightControlVM.setTargetPosition(position)
                flightControlVM.moveForward()

            }
        }

        routeAdapter = RouteAdapter(routeListener)
        routeController = RouteController(routeAdapter)
        routeController.startReceivingLocation()


    }

    override fun onDestroy() {
        super.onDestroy()
        trackingController.stopReceivingData()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingTargetScreen(
    trackingVM: TrackingTargetViewModel,
    trackingController: TrackingTargetController,
    flightControlVM: FlightAutoControlVM
) {
    val trackingData by trackingVM.trackingData.collectAsState()
    val droneState by flightControlVM.droneState.collectAsState()
    val droneControls by flightControlVM.droneControls.collectAsState()
    val gpsSignalLevel by flightControlVM.gpsSignalLevel.collectAsState()
    val targetPosition by flightControlVM.targetPosition.collectAsState()

    var pitchValue by remember { mutableStateOf(0) }
    // Yaw 조정 시작/중지를 위한 상태 추가
    var isAdjustingYaw by remember { mutableStateOf(false) }


    // Yaw 조정 기능 (버튼 클릭 시에만 작동)
    LaunchedEffect(isAdjustingYaw) {
        if (isAdjustingYaw) {
            while (isAdjustingYaw) {
                val threshold = 0.3
                val minYaw = 10.0    // 최소 회전 속도
                val maxYaw = 150.0   // 최대 회전 속도
                val offsetX = trackingData.normalizedOffsetX
                val absOffsetX = abs(offsetX)

                if (absOffsetX > threshold) {
                    // 임계값을 초과한 부분을 0부터 1 사이로 정규화
                    val scaledOffset = (absOffsetX - threshold) / (1.0 - threshold)
                    // 최소 및 최대 회전 속도 사이에서 보간
                    val adjustmentValue = scaledOffset * (maxYaw - minYaw) + minYaw
                    // 방향에 따라 부호 적용
                    flightControlVM.adjustYaw(adjustmentValue * sign(offsetX))
                } else {
                    flightControlVM.adjustYaw(0.0)
                }

                // 100ms 간격으로 업데이트
                kotlinx.coroutines.delay(100)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Tracking Target & Drone Status") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 드론 상태 정보 표시
            Text("드론 상태:")
            if (droneState != null) {
                Text("롤: ${droneState!!.roll}, 요: ${droneState!!.yaw}, 피치: ${droneState!!.pitch}")
                Text("속도 X: ${droneState!!.xVelocity}, Y: ${droneState!!.yVelocity},  Z:${droneState!!.zVelocity}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 드론 제어 정보 표시
            Text("드론 제어:")
            if (droneControls != null) {
                Text("leftStick (고도): ${droneControls!!.leftStick.verticalPosition}")
                Text("leftStick (회전): ${droneControls!!.leftStick.horizontalPosition}")
                Text("rightStick (앞뒤): ${droneControls!!.rightStick.verticalPosition}")
                Text("rightStick (좌우): ${droneControls!!.rightStick.horizontalPosition}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tracking Data 정보 표시
            Text("Tracking Data:")
            Text("Offset X: ${trackingData.offsetX}, Offset Y: ${trackingData.offsetY}")
            Text("Movement: ${trackingData.movement}")
            Text("Box Width: ${trackingData.boxWidth}, Box Height: ${trackingData.boxHeight}")
            Text("Normalized Offset X: ${trackingData.normalizedOffsetX}")
            Text("Normalized Offset Y: ${trackingData.normalizedOffsetY}")

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = { trackingController.startReceivingData() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Start Tracking")
                }
                Button(onClick = { trackingController.stopReceivingData() }) {
                    Text("Stop Tracking")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Yaw 조정 버튼 추가
            Button(
                onClick = { isAdjustingYaw = !isAdjustingYaw },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(if (isAdjustingYaw) "Stop Adjusting Yaw" else "Start Adjusting Yaw")
            }


            Spacer(modifier = Modifier.height(16.dp))

            // 이륙, 착륙, 가상 스틱 모드 제어 버튼 추가
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { flightControlVM.startTakeOff() }) {
                    Text("Take Off")
                }
                Button(onClick = { flightControlVM.startLanding() }) {
                    Text("Land")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { flightControlVM.enableVirtualStickMode() }) {
                    Text("Enable Virtual Stick")
                }
                Button(onClick = { flightControlVM.disableVirtualStickMode() }) {
                    Text("Disable Virtual Stick")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Slider (Compose의 SeekBar 대체)
            Text("Pitch 조절:")
            Slider(
                value = pitchValue.toFloat(),
                onValueChange = { value ->
                    pitchValue = value.toInt()
                    flightControlVM.setPitch(pitchValue)
                },
                valueRange = -100f..100f, // pitch 범위 설정
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Text("현재 Pitch: $pitchValue")
            Spacer(modifier = Modifier.height(16.dp))
            // targetPosition 정보 표시
            Text("목표 위치:")
            if (targetPosition != null) {
                Text("위도: ${targetPosition!!.latitude}")
                Text("경도: ${targetPosition!!.longitude}")
                Text("고도: ${targetPosition!!.altitude}")
            } else {
                Text("목표 위치 정보가 없습니다.")
            }
        }
    }
}
