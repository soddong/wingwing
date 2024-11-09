package com.ssafy.shieldroneapp

/**
 * 앱이 실행될 때 가장 먼저 생성되는 클래스
 *
 * @HiltAndroidApp 어노테이션: Hilt의 의존성 주입 컨테이너를 생성
 * 앱 전역에서 사용할 초기 설정이나 싱글톤 객체들을 관리합니다.
 * */

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.services.connection.WearableDataListenerService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MobileMainApplication : Application() {
    companion object {
        private const val TAG = "모바일: 메인 앱"
        const val NOTIFICATION_CHANNEL_ID = "watch_connection_channel"
    }
    @Inject
    lateinit var webSocketService: WebSocketService

    override fun onCreate() {
        super.onCreate()
        // - SharedPreferences 초기화
        // - 네트워크 설정
        // - 로깅 설정
        setupLogging()
        // - 푸시 알림 설정 등
        createNotificationChannels()
        // 앱 시작시 필요한 초기화 작업
        initializeWebSocket()
        // 서비스 자동 시작
        startService(Intent(this, WearableDataListenerService::class.java))
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "워치 연결 상태",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "워치와의 연결 상태 알림"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 50, 100)
                setShowBadge(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "디버그 빌드에서만 상세 로깅 활성화")
        }
    }
    private fun initializeWebSocket() {
        webSocketService.initialize()
    }
}