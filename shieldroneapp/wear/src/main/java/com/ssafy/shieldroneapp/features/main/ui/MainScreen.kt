package com.ssafy.shieldroneapp.features.main.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.wear.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.ssafy.shieldroneapp.domain.repository.SensorRepository
import com.ssafy.shieldroneapp.domain.repository.DataRepository
import com.ssafy.shieldroneapp.data.remote.WearConnectionManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.ssafy.shieldroneapp.core.ui.components.Flex
import com.ssafy.shieldroneapp.core.ui.components.PrimaryButton
import com.ssafy.shieldroneapp.data.remote.AlertHandler
import com.ssafy.shieldroneapp.features.alert.AlertViewModel
import com.ssafy.shieldroneapp.features.alert.ui.AlertScreen
import com.ssafy.shieldroneapp.features.heartrate.HeartRateMeasureUiState
import com.ssafy.shieldroneapp.features.heartrate.HeartRateMeasureViewModelFactory
import com.ssafy.shieldroneapp.features.heartrate.HeartRateViewModel
import com.ssafy.shieldroneapp.features.heartrate.ui.HeartRateMeasure
import com.ssafy.shieldroneapp.features.notsupported.ui.NotSupportedScreen

private const val TAG = "워치: 메인스크린"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    sensorRepository: SensorRepository,
    dataRepository: DataRepository,
    wearConnectionManager: WearConnectionManager,
    alertHandler: AlertHandler,
) {
    val alertViewModel: AlertViewModel = hiltViewModel()
    val currentAlert by alertViewModel.currentAlert.collectAsState(initial = null)

    val isMobileConnected by remember { wearConnectionManager.isMobileActive }

    if (currentAlert != null) {
        AlertScreen(
            alertViewModel = alertViewModel,
            wearConnectionManager = wearConnectionManager,
            alertHandler = alertHandler
        )
        return
    }

    if (!isMobileConnected) {
        MobileDisconnectedScreen(wearConnectionManager = wearConnectionManager)
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

        if (hrUiState == HeartRateMeasureUiState.Supported) {
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
fun MobileDisconnectedScreen(wearConnectionManager: WearConnectionManager) {
    var isLoading by remember { mutableStateOf(false) }

    Flex {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "모바일 앱이 실행되지 않았습니다",
            color = Color.White,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "모바일 앱을 실행해주세요",
            color = Color.White,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
            )
        } else {
            PrimaryButton(
                onClick = {
                    isLoading = true
                    wearConnectionManager.requestMobileAppLaunch()
                },
                text = "모바일 앱 실행"
            )
        }
    }
}