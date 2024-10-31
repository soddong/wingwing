package com.ssafy.shieldroneapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ssafy.shieldroneapp.viewmodels.SensorViewModel

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
                        text = "75",
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
                    text = "걷기",
                    style = MaterialTheme.typography.title3
                )
            }
        }
    }
}