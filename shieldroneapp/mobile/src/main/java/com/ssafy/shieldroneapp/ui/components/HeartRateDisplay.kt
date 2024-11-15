package com.ssafy.shieldroneapp.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.BuildConfig

@Composable
fun HeartRateDisplay(
    heartRate: Double,
) {
    // 마지막 유효한 심박수 값을 저장
    var lastValidHeartRate by rememberSaveable { mutableStateOf(heartRate) }

    LaunchedEffect(heartRate) {
        if (heartRate > 0) {
            lastValidHeartRate = heartRate
        }
    }

    Card(
        modifier = Modifier
            .padding(16.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "현재 심박수",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${if (heartRate > 0) heartRate.toInt() else lastValidHeartRate.toInt()} bpm",
                style = MaterialTheme.typography.h3.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}