package com.ssafy.shieldroneapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssafy.shieldroneapp.ui.components.SensorDisplay
import com.ssafy.shieldroneapp.viewmodels.SensorViewModel

@Composable
fun MainScreen(
    sensorViewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    SensorDisplay(
        heartRate = sensorViewModel.currentHeartRate,
        onSafetyStatusClick = { /* 상태 상세 보기 */ },
        onHeartRateClick = { /* 심박수 상세 보기 */ },
        modifier = modifier
    )
}