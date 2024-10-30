package com.ssafy.shieldroneapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
//import com.ssafy.shieldroneapp.ui.components.StatusIndicator
//import com.ssafy.shieldroneapp.ui.components.SensorDisplay
import com.ssafy.shieldroneapp.viewmodels.SensorViewModel
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize

@Composable
fun MainScreen(
    sensorViewModel: SensorViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 상태 카드
        Card(
            onClick = { /* 상태 상세 보기 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "상태",
                    style = MaterialTheme.typography.caption1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "정상",
                    style = MaterialTheme.typography.title2,
                    color = MaterialTheme.colors.primary
                )
            }
        }

        // 심박수 카드
        Card(
            onClick = { /* 심박수 상세 보기 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "심박수",
                    style = MaterialTheme.typography.caption1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "75",  // sensorViewModel.heartRate 값 사용
                        style = MaterialTheme.typography.title1
                    )
                    Text(
                        text = " BPM",
                        style = MaterialTheme.typography.caption2,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // 활동 상태 카드
        Card(
            onClick = { /* 활동 상세 보기 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "활동",
                    style = MaterialTheme.typography.caption1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "걷기",  // sensorViewModel.activity 값 사용
                    style = MaterialTheme.typography.title2
                )
            }
        }

        // 배터리 상태
        Card(
            onClick = { /* 배터리 상세 보기 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "배터리",
                    style = MaterialTheme.typography.caption1
                )
                Text(
                    text = "85%",  // sensorViewModel.batteryLevel 값 사용
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.secondary
                )
            }
        }
    }
}