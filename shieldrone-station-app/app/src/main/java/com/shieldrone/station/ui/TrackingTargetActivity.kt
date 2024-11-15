package com.shieldrone.station.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.model.CameraStreamVM
import com.shieldrone.station.model.FlightAutoControlVM
import com.shieldrone.station.model.TrackingDataVM
import com.shieldrone.station.service.route.RouteAdapter
import dji.sdk.keyvalue.value.common.ComponentIndexType
import kotlin.math.abs
import kotlin.math.sign

class TrackingTargetActivity : ComponentActivity() {
    private val trackingVM: TrackingDataVM by viewModels()
    private val flightControlVM: FlightAutoControlVM by viewModels() // FlightControlVM 추가
    private val cameraStreamVM: CameraStreamVM by viewModels()
    private lateinit var routeAdapter: RouteAdapter
    private lateinit var routeController: RouteController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 카메라 인덱스 설정
        cameraStreamVM.setCameraIndex(ComponentIndexType.LEFT_OR_MAIN)
        setContent {
            TrackingTargetScreen(trackingVM, flightControlVM, cameraStreamVM)
        }
//        val routeListener = object : RouteAdapter.RouteListener {
//            override fun onRouteUpdate(latitude: Double, longitude: Double, altitude: Double) {
//                val position = Position(latitude = latitude, longitude = longitude, altitude = 1.2)
//                Log.d(FLIGHT_CONTROL_TAG, "Updated Route to: $latitude, $longitude")
//                flightControlVM.setTargetPosition(position)
//
//            }
//        }

//        routeAdapter = RouteAdapter(routeListener)
//        routeController = RouteController(routeAdapter)
//        routeController.startReceivingLocation()


    }

    override fun onDestroy() {
        super.onDestroy()
        cameraStreamVM.removeFrameListener()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingTargetScreen(
    trackingVM: TrackingDataVM,
    flightControlVM: FlightAutoControlVM,
    cameraStreamVM: CameraStreamVM
) {
    val trackingData by trackingVM.trackingDataDiffFlow.collectAsState()
    val droneState by flightControlVM.droneState.collectAsState()
    val gpsSignalLevel by flightControlVM.gpsSignalLevel.collectAsState()
    val targetPosition by flightControlVM.targetPosition.collectAsState()
    val virtualMessage by flightControlVM.virtualMessage.collectAsState()
    val frameInfo by cameraStreamVM.frameInfo.collectAsState()

    var maxYaw by remember {  mutableStateOf(150.0) }   // 최대 회전 속도
    var maxStickValue by remember {  mutableStateOf(220.0) }   // 최대 회전 속도
    var altitudeValue by remember { mutableStateOf(0) }
    // Yaw 조정 시작/중지를 위한 상태 추가
    var isAdjustingYaw by remember { mutableStateOf(false) }

    var KpValue by remember { mutableStateOf(2.0) } // 드론 속도 조절 가중치


    // Yaw 조정 기능 (버튼 클릭 시에만 작동)
    LaunchedEffect(isAdjustingYaw) {
        if (isAdjustingYaw) {
            while (isAdjustingYaw && trackingData != null) {
                val threshold = 0.3
                val minYaw = 10.0    // 최소 회전 속도
                val offsetX = trackingData!!.newData.normalizedOffsetX
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

                val eYFuture = trackingData!!.futureErrorY

                val vY = -KpValue * eYFuture

                val vYLimited = vY.coerceIn(-1.0, 1.0)* maxStickValue
                // 100ms 간격으로 업데이트
                flightControlVM.updatePitch(vYLimited.toInt())
                flightControlVM.adjustPitch()

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
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 드론 상태 정보 표시
            Text("드론 상태:")
            if (droneState != null) {
                Text("롤: ${droneState!!.roll}, 요: ${droneState!!.yaw}, 피치: ${droneState!!.pitch}")
                Text("고도: ${droneState!!.altitude}")
//                Text("속도 X: ${droneState!!.xVelocity}, Y: ${droneState!!.yVelocity},  Z:${droneState!!.zVelocity}")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tracking Data 정보 표시
            Text("Tracking Data:")
            if (trackingData != null) {
                    Text("Box Width: ${trackingData!!.newData.boxWidth}, Box Height: ${trackingData!!.newData.boxHeight}")
                    Text("Normalized Offset X: ${trackingData!!.newData.normalizedOffsetX}")
                    Text("Normalized Offset Y: ${trackingData!!.newData.normalizedOffsetY}")

                }
            else{
                Text("추적 데이터가 존재하지 않습니다.")
            }
            if(virtualMessage != null) {
                Text("VIRTUAL : ${virtualMessage.toString()}")
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text("스트리밍 상태")
            Text("VIRTUAL : $frameInfo")
            Spacer(modifier = Modifier.height(10.dp))


            Row {
                Button(
                    onClick = { trackingVM.startReceivingData() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Start Tracking")
                }
                Button(onClick = { trackingVM.stopReceivingData() }) {
                    Text("Stop Tracking")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Yaw 조정 버튼 추가
            Button(
                onClick = { isAdjustingYaw = !isAdjustingYaw },
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(if (isAdjustingYaw) "Stop Adjusting Yaw" else "Start Adjusting Yaw")
            }


            Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

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
            Spacer(modifier = Modifier.height(10.dp))

            // Slider (Compose의 SeekBar 대체)
            Text("Kp 조절:")
            Slider(
                value = KpValue.toFloat(),
                onValueChange = { value ->
                    KpValue = value.toDouble()
                },
                valueRange = 0f..4f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )
            Text("현재 Kp: $KpValue")
            Spacer(modifier = Modifier.height(10.dp))

            Text("고도 속도 조절:")
            Slider(
                value = altitudeValue.toFloat(),
                onValueChange = { value ->
                    altitudeValue = value.toInt()
                    flightControlVM.updateAltitude(altitudeValue) // 슬라이더 조정 시 altitude 값 업데이트
                    flightControlVM.adjustAltitude()
                },
                valueRange = -100f..200f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )
            Text("현재 Altitude: $altitudeValue")
            Spacer(modifier = Modifier.height(10.dp))

            Text("Yaw 조절:")
            Slider(
                value = maxYaw.toFloat(),
                onValueChange = { value ->
                    maxYaw = value.toDouble()
                },
                valueRange = 0f..660f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )
            Text("현재 Yaw: $maxYaw")
            Spacer(modifier = Modifier.height(10.dp))

            // 두 번째 슬라이더 (maxStickValue 조절)
            Text("Stick 조절:")
            Slider(
                value = maxStickValue.toFloat(),
                onValueChange = { value ->
                    maxStickValue = value.toDouble()
                },
                valueRange = 0f..660f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )
            Text("현재 Stick Value: $maxStickValue")
            Spacer(modifier = Modifier.height(10.dp))
            // targetPosition 정보 표시
            Text("목표 위치:")
            if (targetPosition != null) {
                Text("위도: ${targetPosition!!.latitude}")
                Text("경도: ${targetPosition!!.longitude}")
//                Text("고도: ${targetPosition!!.altitude}")
            } else {
                Text("목표 위치 정보가 없습니다.")
            }
        }
    }
}
