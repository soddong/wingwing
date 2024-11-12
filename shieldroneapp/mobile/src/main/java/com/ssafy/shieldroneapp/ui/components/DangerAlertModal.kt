package com.ssafy.shieldroneapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.R
import com.ssafy.shieldroneapp.ui.theme.Pretendard
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DangerAlertModal(
    alertState: DangerAlertState,
    onDismiss: () -> Unit,
    onEmergencyAlert: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showModal by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(5) }
    var shouldShowToast by remember { mutableStateOf(false) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    SentMessageToast(
        showToast = shouldShowToast,
        onToastShown = { shouldShowToast = false }
    )

    LaunchedEffect(alertState.isVisible) {
        if (alertState.isVisible) {
            showModal = true
            remainingSeconds = 5

            // Level 3일 경우 5초 후 API 호출
            if (alertState.level == 3) {
                timerJob = scope.launch {
                    try {
                        while (remainingSeconds > 0) {
                            delay(1000)
                            remainingSeconds--
                        }
                        onEmergencyAlert()
                        shouldShowToast = true
                    } finally {
                        showModal = false
                        onDismiss()
                    }
                }
            }
            // Level 1, 2는 이전과 동일하게 5초 후 자동으로 닫힘
            else {
                timerJob = scope.launch {
                    while (remainingSeconds > 0) {
                        delay(1000)
                        remainingSeconds--
                    }
                    showModal = false
                    onDismiss()
                }
            }
        } else {
            showModal = false
        }
    }

    AnimatedVisibility(
        visible = showModal,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color.White,
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = formatTimestamp(alertState.timestamp),
                                style = MaterialTheme.typography.body1.copy(
                                    fontFamily = Pretendard
                                ),
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "[위험 Level ${alertState.level}]",
                                        style = MaterialTheme.typography.h5.copy(
                                            fontFamily = Pretendard,
                                            color = Color(0xFFDC3545)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "위험 신호가 감지되었습니다. ${remainingSeconds}초 후 긴급 알림을 전송합니다.",
                                        style = MaterialTheme.typography.body2.copy(
                                            fontFamily = Pretendard
                                        ),
                                        color = Color.DarkGray
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Image(
                                    painter = painterResource(id = R.drawable.alert_level3),
                                    contentDescription = "드론 경고",
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color.Black,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    timerJob?.cancel()
                                    showModal = false
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                text = "괜찮습니다. 알림을 전송하지 않습니다.",
                                style = MaterialTheme.typography.body2.copy(
                                    fontFamily = Pretendard
                                ),
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

data class DangerAlertState(
    val isVisible: Boolean = false,
    val level: Int = 0,
    val timestamp: Long = 0L
)