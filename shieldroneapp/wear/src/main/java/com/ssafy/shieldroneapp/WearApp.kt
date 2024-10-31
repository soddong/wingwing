package com.ssafy.shieldroneapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.tooling.preview.devices.WearDevices
import com.ssafy.shieldroneapp.ui.components.Layout
import com.ssafy.shieldroneapp.ui.screens.AlertScreen
import com.ssafy.shieldroneapp.ui.screens.MainScreen
import com.ssafy.shieldroneapp.ui.screens.PermissionScreen
import com.ssafy.shieldroneapp.ui.theme.ShieldroneappTheme
import com.ssafy.shieldroneapp.viewmodels.SensorViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ssafy.shieldroneapp.ui.theme.*

@Composable
private fun HeartRateIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = "Heart Icon",
        modifier = modifier.size(28.dp),
        tint = Red400
    )
}

@Composable
private fun PermissionTitle() {
    Text(
        text = "심박수 측정 권한",
        style = MaterialTheme.typography.title2.copy(
            fontWeight = FontWeight.Bold,
            color = Gray100
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun PermissionButton(onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        colors = ChipDefaults.primaryChipColors(),
        border = ChipDefaults.chipBorder(),
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Allow",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "권한 허용",
                style = MaterialTheme.typography.button,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
    }
}


@Composable
private fun PermissionText() {
    Text(
        text = "정확한 심박수 측정을 위해\n센서 접근 권한이 필요합니다",
        style = MaterialTheme.typography.caption2.copy(
            lineHeight = 20.sp
        ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.secondary,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WearApp() {
    val permissionState = rememberPermissionState(permission = android.Manifest.permission.BODY_SENSORS)
    val sensorViewModel = remember { SensorViewModel() }
    var currentScreen by remember { mutableStateOf<SensorViewModel.Screen?>(null) }

    ShieldroneappTheme {
        Layout(
            hasClock = true
        ) {
            when (permissionState.status) {
                is PermissionStatus.Granted -> {
                    currentScreen = sensorViewModel.currentScreen
                    when (currentScreen) {
                        is SensorViewModel.Screen.Main -> MainScreen(sensorViewModel = sensorViewModel)
                        is SensorViewModel.Screen.Alert -> AlertScreen(
                            onSafeConfirm = { sensorViewModel.hideAlert() }
                        )
                        else -> {}
                    }
                }
                is PermissionStatus.Denied -> {
                    LaunchedEffect(Unit) {
                        permissionState.launchPermissionRequest()
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        HeartRateIcon()
                        PermissionTitle()
                        PermissionText()
                        Spacer(modifier = Modifier.height(16.dp))
                        PermissionButton(
                            onClick = {
                                permissionState.launchPermissionRequest()
                            }
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

//@OptIn(ExperimentalPermissionsApi::class)
//@Composable
//fun WearApp() {
//    val permissionState =
//        rememberPermissionState(permission = android.Manifest.permission.BODY_SENSORS)
//    val sensorViewModel = remember { SensorViewModel() }
//
//    ShieldroneappTheme {
//        Layout(
//            hasClock = true
//        ) {
//            when (permissionState.status) {
//                is PermissionStatus.Granted -> {
//                    MainScreen(sensorViewModel = sensorViewModel)
//                    when (sensorViewModel.currentScreen) {
//                        is SensorViewModel.Screen.Main -> MainScreen(sensorViewModel = sensorViewModel)
//                        is SensorViewModel.Screen.Alert -> AlertScreen(
//                            onSafeConfirm = { sensorViewModel.hideAlert() }
//                        )
//                    }
//                }
//
//                is PermissionStatus.Denied -> {
//                    LaunchedEffect(Unit) {
//                        permissionState.launchPermissionRequest()
//                    }
//                    PermissionScreen(
//                        permissionState = permissionState,
//                    )
//                }
//            }
//        }
//    }
//}

//@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
//@Composable
//fun AlertScreenPreview() {
//    ShieldroneappTheme {
//        Layout {
//            AlertScreen(
//                onSafeConfirm = {},
//            )
//        }
//    }
//}