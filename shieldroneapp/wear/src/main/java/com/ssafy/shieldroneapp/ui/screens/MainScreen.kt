package com.ssafy.shieldroneapp.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.wear.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.ssafy.shieldroneapp.data.repository.SensorRepository
import com.ssafy.shieldroneapp.ui.components.PrimaryButton
import com.ssafy.shieldroneapp.ui.components.Flex
import com.ssafy.shieldroneapp.viewmodels.HeartRateMeasureUiState
import com.ssafy.shieldroneapp.viewmodels.SensorViewModel
import com.ssafy.shieldroneapp.viewmodels.HeartRateMeasureViewModelFactory
import com.ssafy.shieldroneapp.ui.components.HeartRateMeasure

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    sensorRepository: SensorRepository
) {
    val viewModel: SensorViewModel = viewModel(
        factory = HeartRateMeasureViewModelFactory(
            sensorRepository = sensorRepository
        )
    )

    val enabled by viewModel.enabled.collectAsState()
    val hr by viewModel.hr
    val availability by viewModel.availability
    val uiState by viewModel.uiState

    if (uiState == HeartRateMeasureUiState.Supported) {
        val permissionState = rememberPermissionState(
            permission = android.Manifest.permission.BODY_SENSORS,
            onPermissionResult = { granted ->
                if (granted) {
                    viewModel.toggleEnabled()
                }
            }
        )

        when (permissionState.status) {
            is PermissionStatus.Granted -> {
                HeartRateMeasure(
                    hr = hr,
                    availability = availability,
                    permissionState = permissionState
                )
            }
            is PermissionStatus.Denied -> {
                LaunchedEffect(Unit) {
                    permissionState.launchPermissionRequest()
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
                            permissionState.launchPermissionRequest()
                        },
                        text = "권한 허용"
                    )
                }
            }
        }
    } else if (uiState == HeartRateMeasureUiState.NotSupported) {
        NotSupportedScreen()
    } else {
        // uiState == HeartRateMeasureUiState.Startup
        LaunchedEffect(Unit) {
            viewModel.toggleEnabled()
        }
        HeartRateMeasure(
            hr = hr,
            availability = availability,
            permissionState = rememberPermissionState(
                permission = android.Manifest.permission.BODY_SENSORS
            )
        )
    }
}