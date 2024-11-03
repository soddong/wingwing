package com.ssafy.shieldroneapp.ui.screens

import android.util.Log
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.wear.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.ssafy.shieldroneapp.data.repository.SensorRepository
import com.ssafy.shieldroneapp.ui.components.*
import com.ssafy.shieldroneapp.viewmodels.*
import androidx.compose.runtime.rememberCoroutineScope
import com.ssafy.shieldroneapp.data.repository.DataRepository

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    sensorRepository: SensorRepository,
    dataRepository: DataRepository
) {
    val heartRateViewModel: HeartRateViewModel = viewModel(
        factory = HeartRateMeasureViewModelFactory(
            sensorRepository = sensorRepository,
            dataRepository = dataRepository
        )
    )
    val speedViewModel: SpeedViewModel = viewModel(
        factory = SpeedViewModelFactory(
            sensorRepository = sensorRepository
        )
    )

    val scope = rememberCoroutineScope()

    // HeartRate 상태
    val hr by heartRateViewModel.hr
    val hrAvailability by heartRateViewModel.availability
    val hrUiState by heartRateViewModel.uiState

    // Speed 상태
    val speed by speedViewModel.speed
    val speedAvailability by speedViewModel.availability
    val speedUiState by speedViewModel.uiState

    // 권한 상태 관리
    val hrPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.BODY_SENSORS,
        onPermissionResult = { granted ->
            if (granted) {
                heartRateViewModel.toggleEnabled()
            }
        }
    )

//    val speedPermissionState = rememberPermissionState(
//        permission = android.Manifest.permission.ACTIVITY_RECOGNITION,
//        onPermissionResult = { granted ->
//            if (granted) {
////                speedViewModel.recheckCapability()
////                speedViewModel.toggleEnabled()
//                scope.launch {
//                    Log.d("MainScreen", "Speed permission granted, checking capability")
//                    speedViewModel.recheckCapability()
//                    Log.d("MainScreen", "Capability check done, toggling enabled")
//                    speedViewModel.toggleEnabled()
//                }
//            }
//        }
//    )
    val speedPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.ACTIVITY_RECOGNITION,
        onPermissionResult = { granted ->
            if (granted) {
                scope.launch {  // viewModelScope 대신 scope 사용
                    Log.d("MainScreen", "Speed permission granted, checking capability")
                    speedViewModel.recheckCapability()
                    Log.d("MainScreen", "Capability check done, toggling enabled")
                    speedViewModel.toggleEnabled()
                }
            }
        }
    )

    val locationPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.ACCESS_FINE_LOCATION,
        onPermissionResult = { granted ->
            if (granted) {
                scope.launch {  // 여기도 scope 추가
                    speedViewModel.recheckCapability()
                    speedViewModel.toggleEnabled()
                }
            }
        }
    )

    // 권한 요청 순서 제어
    LaunchedEffect(hrPermissionState.status) {
        if (hrPermissionState.status is PermissionStatus.Granted &&
            speedPermissionState.status is PermissionStatus.Denied
        ) {
            speedPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(speedPermissionState.status) {
        if (speedPermissionState.status is PermissionStatus.Granted &&
            locationPermissionState.status is PermissionStatus.Denied
        ) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    if (hrUiState == HeartRateMeasureUiState.Supported) {
        when (hrPermissionState.status) {
            is PermissionStatus.Granted -> {
                when (speedPermissionState.status) {
                    is PermissionStatus.Granted -> {
                        when (locationPermissionState.status) {
                            is PermissionStatus.Granted -> {
                                Log.d("MainScreen", "모든 권한 허용")
                                Log.d("속도 상태", "$speedUiState")
                                Log.d("심박수 상태", "$hrUiState")

                                Column {
                                    // 심박수 UI
                                    HeartRateMeasure(
                                        modifier = Modifier.weight(1f),
                                        hr = hr,
                                        availability = hrAvailability,
                                        permissionState = hrPermissionState,
                                        onStartMeasuring = { heartRateViewModel.toggleEnabled() }
                                    )

                                    // 속도 UI
                                    if (speedUiState == SpeedUiState.Supported) {
                                        Log.d("MainScreen", "SpeedUiState is Supported")
                                        Spacer(Modifier.height(16.dp))
                                        SpeedMeasure(
                                            modifier = Modifier.weight(1f),
                                            speed = speed,
                                            availability = speedAvailability,
                                            permissionState = speedPermissionState,
                                            onStartMeasuring = { speedViewModel.toggleEnabled() }
                                        )
                                    } else if (speedUiState == SpeedUiState.NotSupported) {
                                        NotSupportedScreen()
                                    } else if (speedUiState == SpeedUiState.Startup) {
                                        Spacer(Modifier.height(16.dp))
                                        Flex {
                                            Text(
                                                text = "센서 초기화 중...",
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Log.d("MainScreen", "SpeedUiState is ${speedUiState}")
                                    }
                                }
                            }

                            is PermissionStatus.Denied -> {
                                Flex {
                                    Spacer(Modifier.height(20.dp))
                                    Text(
                                        text = "위치 권한이 필요합니다",
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    PrimaryButton(
                                        onClick = {
                                            locationPermissionState.launchPermissionRequest()
                                        },
                                        text = "권한 허용"
                                    )
                                }
                            }
                        }
                    }

                    is PermissionStatus.Denied -> {
                        LaunchedEffect(Unit) {
                            speedPermissionState.launchPermissionRequest()
                        }
                        Flex {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = "속도 측정을 위해",
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "권한을 허용해주세요",
                                color = Color.White
                            )
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton(
                                onClick = {
                                    speedPermissionState.launchPermissionRequest()
                                },
                                text = "권한 허용"
                            )
                        }
                    }
                }
            }

            is PermissionStatus.Denied -> {
                LaunchedEffect(Unit) {
                    hrPermissionState.launchPermissionRequest()
                }
                Flex {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "심박수 측정을 위해",
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "권한을 허용해주세요",
                        color = Color.White
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        onClick = {
                            hrPermissionState.launchPermissionRequest()
                        },
                        text = "권한 허용"
                    )
                }
            }
        }
    } else if (hrUiState == HeartRateMeasureUiState.NotSupported) {
        NotSupportedScreen()
    }
}