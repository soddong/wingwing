package com.ssafy.shieldroneapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.ssafy.shieldroneapp.ui.theme.*

@Composable
fun SafetyStatusCard(
    statusText: String = "안전",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = MaterialTheme.colors.surface,
            endBackgroundColor = MaterialTheme.colors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "상태",
                style = MaterialTheme.typography.caption2.copy(
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.title2.copy(
                    fontWeight = FontWeight.Bold,
                    color = Green400
                )
            )
        }
    }
}

@Composable
fun HeartRateCard(
    heartRate: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = MaterialTheme.colors.surface,
            endBackgroundColor = MaterialTheme.colors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "심박수",
                style = MaterialTheme.typography.caption2.copy(
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = heartRate.toString(),
                    style = MaterialTheme.typography.display2.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "BPM",
                    style = MaterialTheme.typography.caption1,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SensorDisplay(
    heartRate: Int,
    onSafetyStatusClick: () -> Unit,
    onHeartRateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SafetyStatusCard(
            onClick = onSafetyStatusClick,
            statusText = "안전"
        )

        HeartRateCard(
            heartRate = heartRate,
            onClick = onHeartRateClick
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}