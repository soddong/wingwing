package com.ssafy.shieldroneapp.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.ui.components.ConnectionStatusSnackbar
import com.ssafy.shieldroneapp.viewmodels.HeartRateViewModel
import kotlinx.coroutines.launch
import com.ssafy.shieldroneapp.utils.await

@Composable
fun MainScreen(
    isAppActive: Boolean,
    viewModel: HeartRateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val heartRateState by viewModel.heartRateData.collectAsState()
    val connectionState by viewModel.watchConnectionState.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var connectedNodes by remember { mutableStateOf<List<Node>>(emptyList()) }
    var isChecking by remember { mutableStateOf(true) }

    // 워치 연결 상태 확인
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await(5000)
                connectedNodes = nodes
                isChecking = false
                if (nodes.isEmpty()) {
                    showDialog = true
                } else {
                    // 워치 앱 실행 요청
                    nodes.forEach { node ->
                        Wearable.getMessageClient(context).sendMessage(
                            node.id,
                            "/start/heart_rate_monitor",
                            ByteArray(0)
                        ).await(5000)
                    }
                }
            } catch (e: Exception) {
                isChecking = false
                showDialog = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to the Main Screen!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isAppActive) {
                Text(
                    text = "모바일 앱이 비활성화 되었습니다.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            } else if (isChecking) {
                Text(
                    text = "워치 연결 상태 확인 중...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "심박수 데이터: ${
                        when (heartRateState) {
                            true -> "높음 (경고)"
                            false -> "정상"
                            null -> "측정 중..."
                        }
                    }",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 음성 감지 상태 표시ㅇ
            Text(
                text = "음성 감지: 활성화됨",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 연결 상태 스낵바 표시
        ConnectionStatusSnackbar(
            connectionState = connectionState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 워치 앱 실행 요청 다이얼로그
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("알림") },
                text = {
                    Text(
                        if (connectedNodes.isEmpty())
                            "연결된 워치를 찾을 수 없습니다. 워치가 페어링되어 있는지 확인해주세요."
                        else
                            "심박수 정보를 얻기 위해 워치의 Shield Drone 앱을 실행해주세요."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (connectedNodes.isEmpty()) {
                            // Wear OS 앱 설정으로 이동
                            context.startActivity(Intent("com.google.android.wearable.app.cn"))
                        }
                        showDialog = false
                    }) {
                        Text(if (connectedNodes.isEmpty()) "설정으로 이동" else "확인")
                    }
                }
            )
        }
    }
}