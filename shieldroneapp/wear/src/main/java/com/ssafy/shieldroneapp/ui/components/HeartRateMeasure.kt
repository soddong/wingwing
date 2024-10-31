package com.ssafy.shieldroneapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.tooling.preview.Preview
import androidx.health.services.client.data.DataTypeAvailability
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HeartRateMeasure(
    hr: Double,
    availability: DataTypeAvailability,
    permissionState: PermissionState
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HrLabel(
            hr = hr,
            availability = availability
        )
        if (permissionState.status.isGranted) {
        } else {
            permissionState.launchPermissionRequest()
        }
    }
}

@ExperimentalPermissionsApi
@Preview(
    device = WearDevices.LARGE_ROUND,
    showBackground = false,
    showSystemUi = true
)
@Composable
private fun HeartRateMeasurePreview() {
    val permissionState = object : PermissionState {
        override val permission = "android.permission.ACTIVITY_RECOGNITION"
        override val status: PermissionStatus = PermissionStatus.Granted
        override fun launchPermissionRequest() {}
    }
    ShieldroneappTheme {
        HeartRateMeasure(
            hr = 65.0,
            availability = DataTypeAvailability.AVAILABLE,
            permissionState = permissionState
        )
    }
}