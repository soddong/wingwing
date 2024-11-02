package com.ssafy.shieldroneapp.ui.screens

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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    sensorRepository: SensorRepository
) {
    val heartRateViewModel: HeartRateViewModel = viewModel(
        factory = HeartRateMeasureViewModelFactory(
            sensorRepository = sensorRepository
        )
    )
    val speedViewModel: SpeedViewModel = viewModel(
        factory = SpeedViewModelFactory(
            sensorRepository = sensorRepository
        )
    )

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

    val speedPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.ACTIVITY_RECOGNITION,
        onPermissionResult = { granted ->
            if (granted) {
                speedViewModel.toggleEnabled()
            }
        }
    )

    LaunchedEffect(hrPermissionState.status) {
        if (hrPermissionState.status is PermissionStatus.Granted &&
            speedPermissionState.status is PermissionStatus.Denied) {
            speedPermissionState.launchPermissionRequest()
        }
    }

    if (hrUiState == HeartRateMeasureUiState.Supported) {
        when (hrPermissionState.status) {
            is PermissionStatus.Granted -> {
                when (speedPermissionState.status) {
                    is PermissionStatus.Granted -> {
                        Column {
                            HeartRateMeasure(
                                hr = hr,
                                availability = hrAvailability,
                                permissionState = hrPermissionState,
                                onStartMeasuring = { heartRateViewModel.toggleEnabled() }
                            )

                            // 속도 UI
                            if (speedUiState == SpeedUiState.Supported) {
                                Spacer(Modifier.height(16.dp))
                                SpeedMeasure(
                                    speed = speed,
                                    availability = speedAvailability,
                                    permissionState = speedPermissionState,
                                    onStartMeasuring = { speedViewModel.toggleEnabled() }
                                )
                            } else if (speedUiState == SpeedUiState.NotSupported) {
                                NotSupportedScreen()
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