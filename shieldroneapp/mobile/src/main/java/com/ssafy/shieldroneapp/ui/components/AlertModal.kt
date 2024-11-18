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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
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
    WARNING, // 위험 신호 감지 (10초 타이머 + 버튼)
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

    val warningAnimation by rememberLottieComposition(
        LottieCompositionSpec.Url("https://lottie.host/cf0cb34a-eb30-429b-a89b-684885eb27c0/XVtlsuk1JE.json")
    )

    val objectAnimation by rememberLottieComposition(
        LottieCompositionSpec.Url("https://lottie.host/98dca2bb-37e4-4430-b119-2a5e50907cf8/zlMAouUit2.json")
    )

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
                AlertType.WARNING -> 10
                AlertType.OBJECT -> 3
            }

            timerJob = scope.launch {
                try {
                    while (remainingSeconds > 0) {
                        delay(1000)
                        remainingSeconds--
                    }

                    if (!alertHandler.isWatchConfirmed()) {
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
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(0.9f)
                    .heightIn(min = 240.dp, max = 320.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color.White,
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimestamp(alertState.timestamp),
                        style = MaterialTheme.typography.body1.copy(
                            fontFamily = Pretendard
                        ),
                        color = Color.Black,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            top = 20.dp,
                            end = 24.dp
                        )
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (alertState.alertType) {
                            AlertType.WARNING -> {
                                LottieAnimation(
                                    composition = warningAnimation,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(140.dp)
                                )
                            }
                            AlertType.OBJECT -> {
                                LottieAnimation(
                                    composition = objectAnimation,
                                    iterations = LottieConstants.IterateForever,
                                    modifier = Modifier.size(140.dp)
                                )
                            }
                        }

                        Text(
                            text = when (alertState.alertType) {
                                AlertType.WARNING -> "위험 신호가 감지되었습니다.\n${remainingSeconds}초 후 긴급 알림을 전송합니다."
                                AlertType.OBJECT -> "주변에 타인이 감지되었습니다."
                            },
                            style = MaterialTheme.typography.body1.copy(
                                fontFamily = Pretendard
                            ),
                            color = Color.DarkGray,
                        )
                    }

                    // Bottom Section (Button)
                    if (alertState.alertType == AlertType.WARNING) {
                        Button(
                            onClick = {
                                scope.launch {
                                    timerJob?.cancel()
                                    alertHandler.handleSafeConfirmation()
                                    safetyMessageSender.sendSafeConfirmationToWatch()
                                    onSafeConfirm()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 20.dp),
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
                                )
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