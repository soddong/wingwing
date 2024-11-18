package com.ssafy.shieldroneapp.features.permission.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Text
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.ChipDefaults
import androidx.compose.ui.Alignment
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import com.ssafy.shieldroneapp.core.ui.theme.Gray100
import com.ssafy.shieldroneapp.core.ui.theme.Red400

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
fun PermissionScreen(
    permissionState: PermissionState,
    modifier: Modifier = Modifier
) {
    // 권한 설명 화면
    Column(
        modifier = modifier.fillMaxSize(),
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