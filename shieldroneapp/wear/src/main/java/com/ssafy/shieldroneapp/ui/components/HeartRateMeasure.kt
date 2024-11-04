package com.ssafy.shieldroneapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.health.services.client.data.DataTypeAvailability
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted

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