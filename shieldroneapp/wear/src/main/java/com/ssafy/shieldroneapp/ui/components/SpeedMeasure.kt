package com.ssafy.shieldroneapp.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.services.client.data.DataTypeAvailability
import androidx.wear.compose.material.Text
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeedMeasure(
    modifier: Modifier = Modifier,
    speed: Double,
    availability: DataTypeAvailability,
    permissionState: PermissionState,
    onStartMeasuring: () -> Unit
) {
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            onStartMeasuring()
        }
    }

    LaunchedEffect(speed, availability) {
        Log.d("SpeedMeasure", "Current speed: $speed")
        Log.d("SpeedMeasure", "Current availability: $availability")
    }

    Flex(
        modifier = modifier
    ) {
        Text(
            text = "현재 속도",
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        when (availability) {
            DataTypeAvailability.AVAILABLE -> {
                Column {
                    Text(
                        text = "${speed.roundToOneDecimal()} m/s",
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(speed * 3.6).roundToOneDecimal()} km/h",
                        color = Color.White
                    )
                }
            }

            DataTypeAvailability.ACQUIRING -> {
                Text(
                    text = "속도 측정 중...",
                    color = Color.White
                )
            }

            else -> {
                Text(
                    text = "속도 측정 불가 (상태: $availability)",
                    color = Color.White
                )
            }
        }
    }
}

private fun Double.roundToOneDecimal(): Double =
    (this * 10.0).toInt() / 10.0