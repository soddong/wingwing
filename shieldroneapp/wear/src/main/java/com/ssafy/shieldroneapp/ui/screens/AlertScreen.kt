package com.ssafy.shieldroneapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ssafy.shieldroneapp.ui.components.PrimaryButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ssafy.shieldroneapp.R

@Composable
fun AlertScreen(
    onSafeConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeLeft by remember { mutableStateOf(5) }
    var isTimerRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        if (timeLeft == 0) {
            onSafeConfirm()
        }
    }

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
                onSafeConfirm()
            }
        )
    }
}