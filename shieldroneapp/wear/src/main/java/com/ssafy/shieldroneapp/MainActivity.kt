package com.ssafy.shieldroneapp

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ssafy.shieldroneapp.data.source.remote.AlertHandler
import com.ssafy.shieldroneapp.receivers.ScreenReceiver
import com.ssafy.shieldroneapp.services.WakeLockManager
import com.ssafy.shieldroneapp.services.connection.WearConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var wearConnectionManager: WearConnectionManager

    @Inject
    lateinit var alertHandler: AlertHandler

    private lateinit var wakeLockManager: WakeLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        wakeLockManager = WakeLockManager.getInstance(applicationContext)
        wakeLockManager.acquireWakeLock()

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

        // 워치 앱 활성화 상태 전송
        wearConnectionManager.onAppStateChange(true)

        setTheme(android.R.style.Theme_DeviceDefault_Light)

        setContent {
            WearApp(
                sensorRepository = sensorRepository,
                dataRepository = dataRepository,
                wearConnectionManager = wearConnectionManager,
                alertHandler = alertHandler
            )
        }
    }

    override fun onResume() {
        super.onResume()
        wakeLockManager.acquireWakeLock()
        wearConnectionManager.onAppStateChange(true)
    }

    override fun onPause() {
        super.onPause()
        // onPause에서도 WakeLock 유지
        wearConnectionManager.onAppStateChange(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLockManager.releaseWakeLock()
        wearConnectionManager.onAppStateChange(false)
        try {
            unregisterReceiver(ScreenReceiver())
        } catch (e: Exception) {
            // 이미 해제되었거나 등록되지 않은 경우
        }
    }
}