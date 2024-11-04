package com.ssafy.shieldroneapp

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ssafy.shieldroneapp.receivers.ScreenReceiver

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 화면 켜짐 유지 및 잠금화면 관련 설정
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(ScreenReceiver(), filter)

        val application = application as MainApplication
        val sensorRepository = application.sensorRepository
        val dataRepository = application.dataRepository

        setTheme(android.R.style.Theme_DeviceDefault_Light)

        setContent {
            WearApp(
                sensorRepository = sensorRepository,
                dataRepository = dataRepository
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(ScreenReceiver())
        } catch (e: Exception) {
            // 이미 해제되었거나 등록되지 않은 경우 예외 처리
        }
    }
}