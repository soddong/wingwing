package com.ssafy.shieldroneapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.services.connection.MobileConnectionManager.WatchConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ConnectionStatusSnackbar(
    connectionState: WatchConnectionState,
    modifier: Modifier = Modifier
) {
    var showSnackbar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 연결 상태가 변경될 때마다 스낵바 표시
    LaunchedEffect(connectionState) {
        showSnackbar = true
        scope.launch {
            delay(3000) 
            showSnackbar = false
        }
    }

    if (showSnackbar) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                backgroundColor = when (connectionState) {
                    is WatchConnectionState.Connected -> Color(0xFF4CAF50)
                    is WatchConnectionState.Disconnected -> Color(0xFFE57373)
                    else -> Color.Gray
                }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (connectionState) {
                            is WatchConnectionState.Connected -> Icons.Default.CheckCircle
                            is WatchConnectionState.Disconnected -> Icons.Default.Warning
                            else -> Icons.Default.Warning
                        },
                        contentDescription = "연결 상태 아이콘",
                        tint = Color.White
                    )
                    Text(
                        text = when (connectionState) {
                            is WatchConnectionState.Connected -> "워치와 연결되었습니다"
                            is WatchConnectionState.Disconnected -> "워치와 연결이 끊어졌습니다"
                            is WatchConnectionState.Error -> "워치 연결 오류: ${connectionState.message}"
                        },
                        color = Color.White
                    )
                }
            }
        }
    }
}