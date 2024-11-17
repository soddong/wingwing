package com.ssafy.shieldroneapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ssafy.shieldroneapp.R
import com.ssafy.shieldroneapp.data.source.remote.SafetyMessageSender
import com.ssafy.shieldroneapp.ui.map.screens.AlertHandler
import com.ssafy.shieldroneapp.ui.theme.Pretendard
import kotlinx.coroutines.*
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

data class AlertState(
    val isVisible: Boolean = false,
    val alertType: AlertType = AlertType.WARNING,
    val timestamp: Long = 0L,
)

enum class AlertType {
    WARNING, // 위험 신호 감지 (5초 타이머 + 버튼)
    OBJECT   // 타인 감지 (3초 자동 닫힘)
}

@Composable
fun AlertModal(
    alertState: AlertState,
    onDismiss: () -> Unit,
    onEmergencyAlert: (suspend () -> Boolean)? = null,
    onSafeConfirm: () -> Unit,
    alertHandler: AlertHandler,
    safetyMessageSender: SafetyMessageSender,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showModal by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(0) }
    var shouldShowToast by remember { mutableStateOf(false) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    var apiResponse by remember { mutableStateOf<Response<Unit>?>(null) }

    if (alertState.alertType == AlertType.WARNING) {
        SentMessageToast(
            apiResponse = apiResponse,
            onToastShown = { apiResponse = null }
        )
    }

    LaunchedEffect(alertState.isVisible) {
        if (alertState.isVisible) {
            if (alertHandler.isWatchConfirmed()) {
                timerJob?.cancel()
                showModal = false
                onDismiss()
                return@LaunchedEffect
            }

            showModal = true
            remainingSeconds = when (alertState.alertType) {
                AlertType.WARNING -> 5
                AlertType.OBJECT -> 3
            }

            timerJob = scope.launch {
                try {
                    while (remainingSeconds > 0) {
                        delay(1000)
                        remainingSeconds--
                    }

                    when (alertState.alertType) {
                        AlertType.WARNING -> {
                            onEmergencyAlert?.let { emergencyAlert ->
                                val success = emergencyAlert()
                                if (success) {
                                    shouldShowToast = true
                                }
                            }
                        }

                        AlertType.OBJECT -> {
                            alertHandler.dismissObjectAlert()
                        }
                    }
                } finally {
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
                    modifier = Modifier.fillMaxWidth()
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
                                        text = when (alertState.alertType) {
                                            AlertType.WARNING -> "위험 신호가 감지되었습니다. ${remainingSeconds}초 후 긴급 알림을 전송합니다."
                                            AlertType.OBJECT -> "주변에 타인이 감지되었습니다."
                                        },
                                        style = MaterialTheme.typography.body2.copy(
                                            fontFamily = Pretendard
                                        ),
                                        color = Color.DarkGray
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Image(
                                    painter = painterResource(
                                        id = when (alertState.alertType) {
                                            AlertType.WARNING -> R.drawable.alert_level3
                                            AlertType.OBJECT -> R.drawable.alert_level2
                                        }
                                    ),
                                    contentDescription = when (alertState.alertType) {
                                        AlertType.WARNING -> "드론 경고"
                                        AlertType.OBJECT -> "타인 감지"
                                    },
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                    }

                    if (alertState.alertType == AlertType.WARNING) {
                        Button(
                            onClick = {
                                scope.launch {
                                    // 안전 확인 처리
                                    alertHandler.handleSafeConfirmation()
                                    // 워치에 안전 확인 메시지 전송
                                    safetyMessageSender.sendSafeConfirmationToWatch()
                                    onSafeConfirm()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Black,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            )
                        ) {
                            Text(
                                text = "괜찮습니다. 위험하지 않습니다.",
                                style = MaterialTheme.typography.body2.copy(
                                    fontFamily = Pretendard
                                ),
                                modifier = Modifier.padding(vertical = 8.dp)
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