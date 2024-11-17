package com.shieldrone.station.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shieldrone.station.controller.RouteController
import com.shieldrone.station.model.CameraStreamVM
import com.shieldrone.station.model.FlightAutoControlVM
import com.shieldrone.station.model.GimbalVM
import com.shieldrone.station.model.TrackingDataVM
import com.shieldrone.station.service.route.RouteAdapter
import dji.sdk.keyvalue.value.common.ComponentIndexType
import kotlin.math.abs
import kotlin.math.sign

class TrackingTargetActivity : ComponentActivity() {
    private val trackingVM: TrackingDataVM by viewModels()
    private val flightControlVM: FlightAutoControlVM by viewModels()
    private val cameraStreamVM: CameraStreamVM by viewModels()
    private val gimbalVM: GimbalVM by viewModels()
    private lateinit var routeAdapter: RouteAdapter
    private lateinit var routeController: RouteController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("TrackingTargetActivity", "Activity created")

        // 카메라 인덱스 설정
        cameraStreamVM.setCameraModeAndIndex(ComponentIndexType.LEFT_OR_MAIN)
        Log.i("TrackingTargetActivity", "Camera index set to LEFT_OR_MAIN")

        setContent {
            TrackingTargetScreen(trackingVM, flightControlVM, cameraStreamVM, gimbalVM)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.i("TrackingTargetActivity", "Activity destroyed")
        cameraStreamVM.removeFrameListener()
        Log.i("TrackingTargetActivity", "Camera frame listener removed")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingTargetScreen(
    trackingVM: TrackingDataVM,
    flightControlVM: FlightAutoControlVM,
    cameraStreamVM: CameraStreamVM,
    gimbalVM: GimbalVM
) {
    val trackingData by trackingVM.trackingDataDiffFlow.collectAsState()
    val droneState by flightControlVM.droneState.collectAsState()
    val gpsSignalLevel by flightControlVM.gpsSignalLevel.collectAsState()
    val targetPosition by flightControlVM.targetPosition.collectAsState()
    val virtualMessage by flightControlVM.virtualMessage.collectAsState()
    val frameInfo by cameraStreamVM.frameInfo.collectAsState()
    val virtualStickState by flightControlVM.virtualStickState.collectAsState()
    val gimbalInfo by gimbalVM.gimbalInfo.collectAsState()

    var maxYaw by remember { mutableStateOf(150.0) }   // 최대 회전 속도
    var maxStickValue by remember { mutableStateOf(20.0) }   // 최대 전진 속도
    var altitudeValue by remember { mutableStateOf(0) } // 순항 고도 상승 속도
    var isAdjustingYaw by remember { mutableStateOf(false) } // Yaw 조정 시작/중지를 위한 상태 추가
    var KpValue by remember { mutableStateOf(1.0) } // 드론 속도 조절 가중치


    // Yaw 조정 기능 (버튼 클릭 시에만 작동)
    LaunchedEffect(isAdjustingYaw) {
        if (isAdjustingYaw) {
            Log.i("TrackingTargetActivity", "Yaw adjustment started")
            while (isAdjustingYaw) {
                val threshold = 0.3
                val minYaw = 10.0    // 최소 회전 속도
                val offsetX = trackingData!!.newData.normalizedOffsetX
                val absOffsetX = abs(offsetX)

                Log.d("TrackingTargetActivity", "OffsetX: $offsetX, AbsOffsetX: $absOffsetX")

                if (absOffsetX > threshold) {
                    // 임계값을 초과한 부분을 0부터 1 사이로 정규화
                    val scaledOffset = (absOffsetX - threshold) / (1.0 - threshold)
                    // 최소 및 최대 회전 속도 사이에서 보간
                    val adjustmentValue = scaledOffset * (maxYaw - minYaw) + minYaw
                    Log.d("TrackingTargetActivity", "adjustmentValue: $adjustmentValue")
                    // 방향에 따라 부호 적용
                    flightControlVM.adjustYaw(adjustmentValue * sign(offsetX))
                } else {
                    flightControlVM.adjustYaw(0.0)
                }

                val eYFuture = trackingData!!.futureErrorY

                val vY = -KpValue * eYFuture

                val vYLimited = vY.coerceIn(-1.0, 1.0) * maxStickValue
                Log.d("TrackingTargetActivity", "vYLimited: $vYLimited")
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

            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )
            // Virtual Stick 상태 정보 표시
            Text("Virtual Stick 상태:")
            if (virtualStickState != null) {
                Text(virtualStickState!!)
            }

            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

            // Tracking Data 정보 표시
            Text("Tracking Data:")
            if (trackingData != null) {
                Text("Box Width: ${trackingData!!.newData.boxWidth}, Box Height: ${trackingData!!.newData.boxHeight}")
                Text("Normalized Offset X: ${trackingData!!.newData.normalizedOffsetX}")
                Text("Normalized Offset Y: ${trackingData!!.newData.normalizedOffsetY}")

            } else {
                Text("추적 데이터가 존재하지 않습니다.")
            }
            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

            Text("스트리밍 상태")
            Text("Streaming : $frameInfo")

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

            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

            Text("짐벌 상태")
            Text("Streaming : $gimbalInfo")

            Row {
                Button(
                    onClick = { gimbalVM.setGimbalAngle() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("짐벌 45도 세팅")
                }
                Button(onClick = { gimbalVM.resetGimbal()}) {
                    Text("짐벌 초기화")
                }
            }

            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )


            // Yaw 조정 버튼 추가
            Button(
                onClick = { isAdjustingYaw = !isAdjustingYaw },
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(if (isAdjustingYaw) "Stop Adjusting Yaw" else "Start Adjusting Yaw")
            }


            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

            // 이륙, 착륙, 가상 스틱 모드 제어 버튼 추가
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { flightControlVM.startTakeOff() }) {
                    Text("Take Off")
                }
                Button(onClick = { flightControlVM.ascendToCruiseAltitude(altitudeValue) }) {
                    Text("Cruising altitude")
                }
                Button(onClick = { flightControlVM.startLanding() }) {
                    Text("Land")
                }
            }

            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

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
            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

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
            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

            Text("고도 속도 조절:")
            Slider(
                value = altitudeValue.toFloat(),
                onValueChange = { value ->
                    altitudeValue = value.toInt()
                    flightControlVM.updateAltitude(altitudeValue) // 슬라이더 조정 시 altitude 값 업데이트
                    flightControlVM.adjustAltitude()
                },
                valueRange = 0f..200f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )
            Text("현재 Altitude: $altitudeValue")
            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

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
            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

            // 두 번째 슬라이더 (maxStickValue 조절)
            Text("Stick 조절:")
            Slider(
                value = maxStickValue.toFloat(),
                onValueChange = { value ->
                    maxStickValue = value.toDouble()
                },
                valueRange = 0f..110f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )
            Text("현재 Stick Value: $maxStickValue")
            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )
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
