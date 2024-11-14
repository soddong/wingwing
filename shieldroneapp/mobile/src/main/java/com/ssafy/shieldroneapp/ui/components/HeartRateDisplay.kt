package com.ssafy.shieldroneapp.ui.components

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
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HeartRateDisplay(
    heartRate: State<Double?>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .clickable {},
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
                heartRate.value?.let { bpm ->
                    Text(
                        text = "$bpm bpm",
                        style = MaterialTheme.typography.h3.copy(fontWeight = FontWeight.Bold)
                    )
                } ?: Text(
                    text = "측정 중...",
                    style = MaterialTheme.typography.h3.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}