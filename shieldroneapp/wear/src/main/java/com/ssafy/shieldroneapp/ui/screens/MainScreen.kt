package com.ssafy.shieldroneapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.wear.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.ssafy.shieldroneapp.data.repository.SensorRepository
import com.ssafy.shieldroneapp.ui.components.*
import com.ssafy.shieldroneapp.viewmodels.*
import com.ssafy.shieldroneapp.data.repository.DataRepository
import com.ssafy.shieldroneapp.services.connection.WearConnectionManager

private const val TAG = "워치: 메인스크린"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    sensorRepository: SensorRepository,
    dataRepository: DataRepository,
    wearConnectionManager: WearConnectionManager,
) {
    val isMobileConnected by remember { wearConnectionManager.isMobileActive }

    if (!isMobileConnected) {
        MobileDisconnectedScreen()
    } else {
        val heartRateViewModel: HeartRateViewModel = viewModel(
            factory = HeartRateMeasureViewModelFactory(
                sensorRepository = sensorRepository,
                dataRepository = dataRepository
            )
        )

        val hr by heartRateViewModel.hr
        val hrAvailability by heartRateViewModel.availability
        val hrUiState by heartRateViewModel.uiState

        // 권한 상태 관리
        val hrPermissionState = rememberPermissionState(
            permission = android.Manifest.permission.BODY_SENSORS,
            onPermissionResult = { granted ->
                if (granted) {
                    heartRateViewModel.toggleEnabled()
                }
            }
        )

        val hrBackgroundPermissionState = rememberPermissionState(
            permission = android.Manifest.permission.BODY_SENSORS_BACKGROUND,
            onPermissionResult = { granted ->
                if (granted) {
                    Log.d(TAG, "바디 센서 권한 승인됨")
                }
            }
        )

        LaunchedEffect(Unit) {
            if (hrPermissionState.status is PermissionStatus.Denied) {
                hrPermissionState.launchPermissionRequest()
            }
        }

        LaunchedEffect(hrPermissionState.status) {
            if (hrPermissionState.status is PermissionStatus.Granted &&
                hrBackgroundPermissionState.status is PermissionStatus.Denied
            ) {
                hrBackgroundPermissionState.launchPermissionRequest()
            }
        }

        // 심박수 측정 화면 표시
        if (hrUiState == HeartRateMeasureUiState.Supported) {
            when (hrPermissionState.status) {
                is PermissionStatus.Granted -> {
                    when (hrBackgroundPermissionState.status) {
                        is PermissionStatus.Granted -> {
                            Column {
                                HeartRateMeasure(
                                    modifier = Modifier.weight(1f),
                                    hr = hr,
                                    availability = hrAvailability,
                                    permissionState = hrPermissionState,
                                    onStartMeasuring = { heartRateViewModel.toggleEnabled() }
                                )
                            }
                        }

                        is PermissionStatus.Denied -> {
                            // 백그라운드 권한이 거부된 경우에만 요청 UI 표시
                            Flex {
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = "백그라운드에서",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "심박수 측정을 위해",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "권한을 허용해주세요",
                                    color = Color.White,
                                    fontSize = 14.sp

                                )
                                Spacer(Modifier.height(16.dp))
                                PrimaryButton(
                                    onClick = {
                                        hrBackgroundPermissionState.launchPermissionRequest()
                                    },
                                    text = "권한 허용"
                                )
                            }
                        }
                    }
                }

                is PermissionStatus.Denied -> {
                    // BODY_SENSORS 권한이 거부된 경우에만 요청 UI 표시
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
}

@Composable
fun MobileDisconnectedScreen() {
    Flex {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "모바일 앱과",
            color = Color.White,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "연결되지 않았습니다",
            color = Color.White,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(16.dp))
    }
}