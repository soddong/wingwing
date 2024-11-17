package com.ssafy.shieldroneapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kakao.vectormap.KakaoMapPhase
import com.kakao.vectormap.KakaoMapSdk
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.services.connection.MobileConnectionManager
import com.ssafy.shieldroneapp.services.connection.WearableDataListenerService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * 앱이 실행될 때 가장 먼저 생성되는 클래스
 *
 * @HiltAndroidApp 어노테이션: Hilt의 의존성 주입 컨테이너를 생성
 * 앱 전역에서 사용할 초기 설정이나 싱글톤 객체들을 관리합니다.
 * */
@HiltAndroidApp
class MobileMainApplication : Application() {
    companion object {
        private const val TAG = "모바일: 메인 앱"
        const val NOTIFICATION_CHANNEL_ID = "watch_connection_channel"
        var isApplicationActive = true
    }
    @Inject
    lateinit var webSocketService: WebSocketService

    @Inject
    lateinit var connectionManager: MobileConnectionManager

    init {
        // 프로세스 강제 종료 시
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                runBlocking {
                    Log.d(TAG, "앱 프로세스 종료 감지")
                    connectionManager.notifyWatchOfMobileStatus(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "앱 종료 상태 전송 실패", e)
            }
        })
    }


    override fun onCreate() {
        super.onCreate()

        // 개발 중에는 모든 키 해시 허용, 출시할 때는 false로 변경
        val mapPhase = if (BuildConfig.DEBUG) KakaoMapPhase.valueOf("ALPHA") else KakaoMapPhase.valueOf("REAL")
        KakaoMapSdk.init(this, BuildConfig.KAKAO_API_KEY, mapPhase) // Kakao Maps SDK 초기화

        // 앱 시작시 필요한 초기화 작업
        // - SharedPreferences 초기화
        // - 네트워크 설정
        // - 푸시 알림 설정 등

        setupLogging()
        createNotificationChannels()
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

    override fun onTerminate() {
        super.onTerminate()
        isApplicationActive = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "앱 정상 종료 감지")
                connectionManager.notifyWatchOfMobileStatus(false)
            } catch (e: Exception) {
                Log.e(TAG, "앱 종료 상태 전송 실패", e)
            }
        }
    }
}