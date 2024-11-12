package com.ssafy.shieldroneapp.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CustomToast(
    message: String,
    showToast: Boolean,
    duration: Int = Toast.LENGTH_SHORT,
    onToastShown: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(showToast) {
        if (showToast) {
            scope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, message, duration).show()
                }
                onToastShown()
            }
        }
    }
}

@Composable
fun SentMessageToast(
    showToast: Boolean,
    onToastShown: () -> Unit
) {
    CustomToast(
        message = "보호자에게 비상상황임을 알리고 위치를 공유합니다",
        showToast = showToast,
        onToastShown = onToastShown
    )
}