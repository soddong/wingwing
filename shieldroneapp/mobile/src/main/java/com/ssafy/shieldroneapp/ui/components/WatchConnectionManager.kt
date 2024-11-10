package com.ssafy.shieldroneapp.ui.components

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.utils.await
import kotlinx.coroutines.launch

@Composable
fun WatchConnectionManager(
    onConnectionStatusDetermined: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var connectedNodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var isChecking by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await(5000)
                connectedNodes = nodes
                isChecking = false
                if (nodes.isEmpty()) {
                    showDialog = true
                    onConnectionStatusDetermined(false)
                } else {
                    nodes.forEach { node ->
                        Wearable.getMessageClient(context).sendMessage(
                            node.id,
                            "/start/heart_rate_monitor",
                            ByteArray(0)
                        ).await(5000)
                    }
                    onConnectionStatusDetermined(true)
                }
            } catch (e: Exception) {
                isChecking = false
                showDialog = true
                onConnectionStatusDetermined(false)
            }
        }
    }

    if (showDialog) {
        WatchConnectionDialog(
            connectedNodes = connectedNodes,
            onDismiss = {
                showDialog = false
                isChecking = false
            }
        )
    }
}

@Composable
private fun WatchConnectionDialog(
    connectedNodes: List<Node>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "갤럭시 워치 연동 안내",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                if (connectedNodes.isEmpty())
                    "정확한 위험 상황 판단을 위해 갤럭시 워치와 페어링 해주세요."
                else
                    "심박수 정보를 얻기 위해 워치의 Shield Drone 앱을 실행해주세요."
            )
        },
        buttons = {
            if (connectedNodes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "모바일 앱만 사용하기",
                        style = TextStyle(
                            textDecoration = TextDecoration.Underline,
                            fontSize = 12.sp,
                            color = Color.Gray
                        ),
                        modifier = Modifier.clickable(onClick = onDismiss)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    )
}