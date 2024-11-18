package com.ssafy.shieldroneapp.features.heartrate.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.health.services.client.data.DataTypeAvailability
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.ssafy.shieldroneapp.core.ui.components.HrLabel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HeartRateMeasure(
    modifier: Modifier = Modifier,
    hr: Double,
    availability: DataTypeAvailability,
    permissionState: PermissionState,
    onStartMeasuring: () -> Unit
) {
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            Log.d("HeartRateMeasure", "권한 허용됨, 측정 시작")
            onStartMeasuring()
        }
    }

    SideEffect {
        if (permissionState.status.isGranted && availability == DataTypeAvailability.UNKNOWN) {
            Log.d("HeartRateMeasure", "이미 권한이 허용된 상태, 측정 시작")
            onStartMeasuring()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
            HrLabel(
                hr = hr,
                availability = availability
            )

        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }
}