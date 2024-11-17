package com.shieldrone.station.ui

import android.os.Bundle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shieldrone.station.model.FlightControlGPSVM

class FlightControlGPSActivity : ComponentActivity() {
    private val flightControlGPSVM: FlightControlGPSVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel 생성
        // Compose UI 렌더링
        setContent {
            FlightControlGPSScreen(flightControlGPSVM)
        }

        // 위치 수신 시작
        flightControlGPSVM.startReceivingLocation()
    }

    override fun onDestroy() {
        super.onDestroy()
        flightControlGPSVM.stopReceivingLocation()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FlightControlGPSScreen(flightControlGPSVM: FlightControlGPSVM) {
        val currentLocation by flightControlGPSVM.currentLocation.collectAsState()
        val droneState by flightControlGPSVM.droneState.collectAsState()
        val virtualStickState by flightControlGPSVM.virtualStickState.collectAsState()

        var maxPitchValue by remember { mutableStateOf(110.0) }   // 최대 피치 값
        var altitudeValue by remember { mutableStateOf(0) } // 순항 고도 상승 속도
        val KpValue by remember { mutableStateOf(2.0) } // 드론 속도 조절 가중치

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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { flightControlGPSVM.startTakeOff() }) {
                        Text("Take Off")
                    }
                    Button(onClick = { flightControlGPSVM.ascendToCruiseAltitude(altitudeValue) }) {
                        Text("Cruising altitude")
                    }
                    Button(onClick = { flightControlGPSVM.startLanding() }) {
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
                    Button(onClick = { flightControlGPSVM.enableVirtualStickMode() }) {
                        Text("Enable Virtual Stick")
                    }
                    Button(onClick = { flightControlGPSVM.disableVirtualStickMode() }) {
                        Text("Disable Virtual Stick")
                    }
                }
                Divider(
                    color = Color.Gray, // 원하는 색상으로 변경 가능
                    thickness = 2.dp,   // 구분선의 두께 조절
                    modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
                )

                // Slider (Compose의 SeekBar 대체)
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
                        flightControlGPSVM.updateAltitude(altitudeValue) // 슬라이더 조정 시 altitude 값 업데이트
                        flightControlGPSVM.adjustAltitude()
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

                Text("전진 속도 조절:")
                Slider(
                    value = maxPitchValue.toFloat(),
                    onValueChange = { value ->
                        flightControlGPSVM.updatePitch(value.toInt())
                        flightControlGPSVM.adjustPitch()
                        maxPitchValue = value.toDouble()
                    },
                    valueRange = 0f..110f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                )
                Text("현재 Stick Value: $maxPitchValue")
                Divider(
                    color = Color.Gray, // 원하는 색상으로 변경 가능
                    thickness = 2.dp,   // 구분선의 두께 조절
                    modifier = Modifier.padding(vertical = 8.dp) // 상하 여백 추가
                )
                // currentLocation 정보 표시
                Text("목표 위치:")
                Text("위도: ${currentLocation.latitude}")
                Text("경도: ${currentLocation.longitude}")
//                  Text("고도: ${currentLocation!!.altitude}")
            }
        }
    }
}