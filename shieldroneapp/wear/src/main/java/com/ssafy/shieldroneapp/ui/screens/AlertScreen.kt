package com.ssafy.shieldroneapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ssafy.shieldroneapp.ui.components.PrimaryButton
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.compose.ui.platform.LocalContext
import com.ssafy.shieldroneapp.viewmodels.AlertViewModel
import kotlinx.coroutines.delay

@Composable
fun AlertScreen(
    viewModel: AlertViewModel,  // AlertViewModel 주입
    modifier: Modifier = Modifier
) {
    val currentAlert by viewModel.currentAlert.collectAsState(initial = null)
    var timeLeft by remember { mutableStateOf(5) }
    var isTimerRunning by remember { mutableStateOf(true) }
    var showEmergencyNotification by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 현재 알림이 없으면 빈 화면 반환
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

    // TODO: 위험 상황 모바일 앱으로 알림 전송
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

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        if (timeLeft == 0) {
            showEmergencyAlert()
            showEmergencyNotification = true
            delay(2000L)
            viewModel.clearAlert()
        }
    }

    if (showEmergencyNotification) {
        EmergencyNotificationScreen()
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
                    isTimerRunning = false
                    viewModel.clearAlert() 
                }
            )
        }
    }
}