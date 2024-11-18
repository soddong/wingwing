package com.shieldrone.station.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.shieldrone.station.constant.FlightConstant.Companion.GPS_ALTITUDE
import com.shieldrone.station.model.CameraStreamVM
import com.shieldrone.station.model.FlightControlVM
import com.shieldrone.station.model.GimbalVM
import com.shieldrone.station.model.TrackingDataVM
import dji.sdk.keyvalue.value.common.ComponentIndexType
import kotlin.math.abs
import kotlin.math.sign

private val TAG = "FlightControlActivity"

class FlightControlActivity : ComponentActivity() {
    private val trackingVM: TrackingDataVM by viewModels()
    private val flightControlVM: FlightControlVM by viewModels()
    private val cameraStreamVM: CameraStreamVM by viewModels()
    private val gimbalVM: GimbalVM by viewModels()

    override fun onDestroy() {
        super.onDestroy()
        trackingVM.stopReceivingData()
        cameraStreamVM.removeFrameListener()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Activity created")

        // 카메라 인덱스 설정
        cameraStreamVM.setCameraModeAndIndex(ComponentIndexType.LEFT_OR_MAIN)
        Log.i(TAG, "Camera index set to LEFT_OR_MAIN")

//        flightControlVM.startReceivingLocation()
        Log.i(TAG, "location receive start")

        setContent {
            TrackingTargetScreen(trackingVM, flightControlVM, cameraStreamVM, gimbalVM)
        }
    }
}

@Composable
fun TrackingTargetScreen(
    trackingVM: TrackingDataVM,
    flightControlVM: FlightControlVM,
    cameraStreamVM: CameraStreamVM,
    gimbalVM: GimbalVM
) {
    val trackingData by trackingVM.trackingDataDiffFlow.collectAsState()
    val droneState by flightControlVM.droneState.collectAsState()
    val currentLocation by flightControlVM.currentLocation.collectAsState()
    val frameInfo by cameraStreamVM.frameInfo.collectAsState()
    val virtualStickState by flightControlVM.virtualStickState.collectAsState()
    val gimbalInfo by gimbalVM.gimbalInfo.collectAsState()

    var maxYaw by remember { mutableStateOf(220.0) }   // 최대 회전 속도
    var maxStickValue by remember { mutableStateOf(30.0) }   // 최대 전진 속도
    var altitudeValue by remember { mutableStateOf(30) } // 순항 고도 상승 속도
    var isAdjustingYaw by remember { mutableStateOf(false) } // Yaw 조정 시작/중지를 위한 상태 추가
    var KpValue by remember { mutableStateOf(1.5) } // 드론 속도 조절 가중치
    val threshold = 0.2 // 드론 제어 시작 임계값
    val minYaw = 10.0    // 최소 회전 속도
    Log.d("Recomposition", "TrackingTargetScreen recomposed")

    // Yaw 조정 기능 (버튼 클릭 시에만 작동)
    LaunchedEffect(isAdjustingYaw) {
        var yawAdjustment = 0.0
        if (isAdjustingYaw) {
            Log.i(TAG, "Yaw adjustment started")
            while (isAdjustingYaw) {

                val offsetX = trackingData!!.newData.normalizedOffsetX
                val absOffsetX = abs(offsetX)

                Log.d(TAG, "OffsetX: $offsetX, AbsOffsetX: $absOffsetX")

                if (absOffsetX > threshold) {
                    // 임계값을 초과한 부분을 0부터 1 사이로 정규화
                    val scaledOffset = (absOffsetX - threshold) / (1.0 - threshold)
                    // 최소 및 최대 회전 속도 사이에서 보간
                    val adjustmentValue = scaledOffset * (maxYaw - minYaw) + minYaw
                    Log.d(TAG, "adjustmentValue: $adjustmentValue")
                    // 방향에 따라 부호 적용
                    yawAdjustment = adjustmentValue * sign(offsetX)
                } else {
                    yawAdjustment = 0.0
                }
                // yaw와 고도를 동시에 조절
                flightControlVM.adjustLeftStick(yawAdjustment, GPS_ALTITUDE)

                val eYFuture = trackingData!!.futureErrorY

                val vY = -KpValue * eYFuture

                val vYLimited = vY.coerceIn(-1.0, 1.0) * maxStickValue
                Log.d(TAG, "vYLimited: $vYLimited")
                // 200ms 간격으로 업데이트
                flightControlVM.updatePitch(vYLimited.toInt())
                flightControlVM.adjustPitch()

                kotlinx.coroutines.delay(200)
            }
        }
    }
    Row(modifier = Modifier.fillMaxSize()) {
        // 왼쪽 영상 영역
        CameraStreamSurfaceView(
            cameraStreamVM,
            modifier = Modifier
                .weight(1f) // weight 추가
                .fillMaxHeight())
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 드론 상태 정보 표시
            Text("드론 상태:")
            if (droneState != null) {
                Text("롤: ${droneState!!.roll}, 요: ${droneState!!.yaw}, 피치: ${droneState!!.pitch}")
                Text("고도: ${droneState!!.altitude}")
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
            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )


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
//                Text("Streaming : $gimbalInfo")

            Row {
                Button(
                    onClick = { gimbalVM.setGimbalAngle() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("gimbal 45degree")
                }
                Button(
                    onClick = { gimbalVM.resetGimbal() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("gimbal reset")
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
//                    Button(onClick = { flightControlVM.ascendToCruiseAltitude(altitudeValue) }) {
//                        Text("Cruising altitude")
//                    }
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
            Text("현재 Kp: $KpValue")
            Slider(
                value = KpValue.toFloat(),
                onValueChange = { value ->
                    KpValue = value.toDouble()
                },
                valueRange = 0f..4f,
                steps = 80,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

            Text("순항 고도 상승 속도 조절:")
            Text("현재 속도: $altitudeValue")
            Slider(
                value = altitudeValue.toFloat(),
                onValueChange = { value ->
                    altitudeValue = value.toInt()
                },
                valueRange = 0f..200f,
                steps = 200,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

            Text("Yaw 자동 제어 속도 조절:")
            Text("현재 속도: $maxYaw")
            Slider(
                value = maxYaw.toFloat(),
                onValueChange = { value ->
                    maxYaw = value.toDouble()
                },
                valueRange = 0f..660f,
                steps = 660,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )

            // 두 번째 슬라이더 (maxStickValue 조절)
            Text("pitch(전후진) 자동제어 최대속도 조절:")
            Text("현재 최대 속도: $maxStickValue")
            Slider(
                value = maxStickValue.toFloat(),
                onValueChange = { value ->
                    maxStickValue = value.toDouble()
                },
                valueRange = 0f..110f,
                steps = 110,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )
            // targetPosition 정보 표시
            Text("목표 위치:")
            if (currentLocation != null) {
                Text("위도: ${currentLocation!!.latitude}")
                Text("경도: ${currentLocation!!.longitude}")
//                Text("고도: ${targetPosition!!.altitude}")
            } else {
                Text("목표 위치 정보가 없습니다.")
            }
            }
    }
}
