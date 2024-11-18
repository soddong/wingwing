package com.ssafy.shieldroneapp.features.alert.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ssafy.shieldroneapp.core.ui.components.PrimaryButton
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.compose.ui.platform.LocalContext
import com.ssafy.shieldroneapp.data.remote.AlertHandler
import com.ssafy.shieldroneapp.data.remote.WearConnectionManager
import com.ssafy.shieldroneapp.features.alert.AlertViewModel
import com.ssafy.shieldroneapp.features.images.ui.ImageScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AlertScreen(
    alertHandler: AlertHandler,
    alertViewModel: AlertViewModel,
    modifier: Modifier = Modifier,
    wearConnectionManager: WearConnectionManager,
) {
    val currentAlert by alertViewModel.currentAlert.collectAsState(initial = null)
    var showImage by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(5) }
    var isTimerRunning by remember { mutableStateOf(true) }
    var showEmergencyNotification by remember { mutableStateOf(false) }
    val isSafeConfirmed by alertViewModel.isSafeConfirmed.collectAsState()
    val context = LocalContext.current

    val confirmedFromMobile by alertViewModel.confirmedFromMobile.collectAsState()

    if (currentAlert == null) {
        return
    }

    fun showEmergencyAlert() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "emergency_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("긴급 알림 전송됨")
            .setContentText("보호자와 경찰에 긴급 상황이 전달되었습니다")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    @Composable
    fun EmergencyNotificationScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "긴급 알림 전송됨",
                style = MaterialTheme.typography.title2,
                color = MaterialTheme.colors.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "보호자와 경찰측에\n긴급 상황이 전달되었습니다.",
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center
            )
        }
    }

    LaunchedEffect(isTimerRunning, isSafeConfirmed) {
        while (isTimerRunning && timeLeft > 0 && !isSafeConfirmed) {
            delay(1000L)
            timeLeft--
        }

        if (timeLeft == 0 && !isSafeConfirmed) {
            showEmergencyAlert()
            showEmergencyNotification = true
            delay(5000L)
            showEmergencyNotification = false
            if (currentAlert?.frame != null) {
                showImage = true
            } else {
                alertViewModel.clearAlert()
            }
        }
    }

    if (showImage && currentAlert?.frame != null) {
        ImageScreen(
            jsonMessage = currentAlert?.frame,
            onTimeout = {
                showImage = false
                alertViewModel.clearAlert()
            }
        )
    } else if (showEmergencyNotification) {
        EmergencyNotificationScreen()
    } else if (isSafeConfirmed) {
        AlertConfirmScreen(
            message = if (confirmedFromMobile) {
                "모바일 앱에서 '안전'이 확인되었습니다."
            } else {
                "워치에서 '안전'이 확인되어 알림이 중지됩니다."
            },
            onDismiss = {
                alertViewModel.clearAlert()
            }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "이상 감지",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "안전한 상태이신가요?",
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center
            )

            Text(
                text = "버튼이 눌리지 않으면 \n ${timeLeft}초 후 자동으로 긴급 연락됩니다.",
                style = MaterialTheme.typography.caption3,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = "안전 확인",
                onClick = {
                    alertHandler.updateSafeConfirmation(true)
                    Log.d("이거", "alertHandler 눌림")
                    CoroutineScope(Dispatchers.IO).launch {
                        wearConnectionManager.sendSafeConfirmationToMobile()
                        Log.d("이거", "wearConnectionManager 눌림")
                    }
                }
            )
        }
    }
}

@Composable
fun SafeConfirmedScreen(confirmedFromMobile: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "모바일 앱에서 안전함이 확인되어\n알림이 중지됩니다",
            style = MaterialTheme.typography.body2,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}