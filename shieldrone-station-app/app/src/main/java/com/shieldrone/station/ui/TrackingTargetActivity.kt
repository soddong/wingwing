package com.shieldrone.station.ui

import android.annotation.SuppressLint
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
import com.shieldrone.station.model.CameraStreamVM
import com.shieldrone.station.model.FlightAutoControlVM
import com.shieldrone.station.model.GimbalVM
import com.shieldrone.station.model.RouteVM
import com.shieldrone.station.model.TrackingDataVM
import dji.sdk.keyvalue.value.common.ComponentIndexType

class TrackingTargetActivity : ComponentActivity() {
    private val trackingVM: TrackingDataVM by viewModels()
    private val flightControlVM: FlightAutoControlVM by viewModels()
    private val cameraStreamVM: CameraStreamVM by viewModels()
    private val gimbalVM: GimbalVM by viewModels()
    private val routeVM: RouteVM by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("TrackingTargetActivity", "Activity created")

        // 카메라 인덱스 설정
        cameraStreamVM.setCameraModeAndIndex(ComponentIndexType.LEFT_OR_MAIN)
        Log.i("TrackingTargetActivity", "Camera index set to LEFT_OR_MAIN")

        flightControlVM.setTrackingInfo(trackingVM.trackingDataDiffFlow)
        routeVM.startReceivingLocation()
        setContent {
            TrackingTargetScreen(trackingVM, flightControlVM, cameraStreamVM, gimbalVM, routeVM)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.i("TrackingTargetActivity", "Activity destroyed")
        cameraStreamVM.removeFrameListener()
        routeVM.stopReceivingLocation()
        Log.i("TrackingTargetActivity", "Camera frame listener removed")
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TrackingTargetScreen(
    trackingVM: TrackingDataVM,
    flightControlVM: FlightAutoControlVM,
    cameraStreamVM: CameraStreamVM,
    gimbalVM: GimbalVM,
    routeVM: RouteVM
) {
    val trackingData by trackingVM.trackingDataDiffFlow.collectAsState()
    val droneState by flightControlVM.droneState.collectAsState()
    val targetPosition by flightControlVM.targetPosition.collectAsState()
    val frameInfo by cameraStreamVM.frameInfo.collectAsState()
    val virtualStickState by flightControlVM.virtualStickState.collectAsState()
    val gimbalInfo by gimbalVM.gimbalInfo.collectAsState()
    val droneStatus by flightControlVM.status.collectAsState()
    val sonicHeight by flightControlVM.sonicHeight.collectAsState()

    val startFlag by routeVM.startFlag.collectAsState()
    var maxYaw by remember { mutableStateOf(220.0) }   // 최대 회전 속도
    var maxStickValue by remember { mutableStateOf(35.0) }   // 최대 전진 속도
    var altitudeValue by remember { mutableStateOf(30) } // 순항 고도 상승 속도
    var isAdjustingYaw by remember { mutableStateOf(false) } // Yaw 조정 시작/중지를 위한 상태 추가
    var KpValue by remember { mutableStateOf(1.25) } // 드론 속도 조절 가중치
    var targetAltitude by remember { mutableStateOf(1.8f) } // 목표 순항 고도
    val threshold = 0.2f // 드론 제어 시작 임계값
    val minYaw = 10.0    // 최소 회전 속도
    val TAG = "TrackingTargetActivity"

    // Yaw 조정 기능 (버튼 클릭 시에만 작동)
    LaunchedEffect(isAdjustingYaw) {
        if (isAdjustingYaw) {
            Log.i(TAG, "Yaw adjustment started")
            while (isAdjustingYaw) {
                flightControlVM.adjustAutoControl(targetAltitude, yawThreshold = threshold, maxYawPower = maxYaw,
                    minYawPower = minYaw, kpValue = KpValue, maxPitchPower = maxStickValue)
                kotlinx.coroutines.delay(100)
            }
        }
    }
    LaunchedEffect(startFlag) {
        if(startFlag) {
            Log.i("TrackingTargetActivity", "start TakeOff")
            gimbalVM.setGimbalAngle()
            flightControlVM.startTakeOff()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 왼쪽 영상 영역
        CameraStreamSurfaceView(
            cameraStreamVM,
            modifier = Modifier
                .weight(1f) // weight 추가
                .fillMaxHeight())
        // 오른쪽 정보 및 버튼 영역
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
                Text("$droneStatus")
//                Text("롤: ${String.format("%.1f", droneState!!.roll)}, 요: ${String.format("%.1f",droneState!!.yaw )}, 피치: ${String.format("%.1f",droneState!!.pitch )}")
                Text("고도: ${String.format("%.1f",droneState!!.altitude)}")
                Text("초음파 높이: ${sonicHeight}")
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
            Text("추적 데이터 상태")
            if (trackingData != null) {
                Text("Normalized Offset X: ${String.format("%.4f", trackingData!!.newData.normalizedOffsetX)}")
                Text("Normalized Offset Y: ${String.format("%.4f", trackingData!!.newData.normalizedOffsetY)}")

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
                Button(onClick = {
                    gimbalVM.setGimbalAngle()
                    flightControlVM.startTakeOff() }) {
                    Text("Take Off")
                }
                Button(onClick = { flightControlVM.ascendToCruiseAltitude(altitudeValue, targetAltitude) }) {
                    Text("Cruising altitude")
                }
                Button(onClick = {
                    gimbalVM.resetGimbal()
                    flightControlVM.startLanding() }) {
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
                steps = 100,
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
                steps = 201,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )
            Divider(
                color = Color.Gray, // 원하는 색상으로 변경 가능
                thickness = 2.dp,   // 구분선의 두께 조절
                modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
            )


            Text("순항 고도 조절:")
            Text("현재 순항 고도: $targetAltitude")
            Slider(
                value = targetAltitude,
                onValueChange = { value ->
                        targetAltitude = `value`
                },
                valueRange = 1.3f..3.8f,
                steps = 24,
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
                steps = 661,
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
                steps = 111,
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
            if (targetPosition != null) {
                Text("위도: ${targetPosition!!.latitude}")
                Text("경도: ${targetPosition!!.longitude}")
            } else {
                Text("목표 위치 정보가 없습니다.")
            }
        }
    }
}
