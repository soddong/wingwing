package com.ssafy.shieldroneapp.services.sensor

import android.content.Intent
import com.ssafy.shieldroneapp.data.audio.AudioRecorder
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.services.base.BaseMobileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ssafy.shieldroneapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


@AndroidEntryPoint
class AudioRecordService : BaseMobileService() {
    companion object {
        private const val TAG = "모바일: 오디오 서비스"
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
        private const val CHANNEL_ID = "audio_record_channel"
        private const val CHANNEL_NAME = "음성 감지 서비스"
        const val NOTIFICATION_ID = 1
    }

    private val reconnectionJob = Job()
    private val reconnectionScope = CoroutineScope(Dispatchers.IO + reconnectionJob)

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var webSocketService: WebSocketService

    private var isRecording = false

    // 알림 채널 생성
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "백그라운드에서 음성을 감지하는 서비스입니다"
                setShowBadge(false) // 앱 아이콘에 뱃지 표시 안 함
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "노티피케이션 채널 생성됨")
        }
    }

    // 포그라운드 서비스 알림 생성
    private fun createNotification(): Notification {
        Log.d(TAG, "포그라운드 서비스 알림 생성")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("음성 감지 중")
            .setContentText("백그라운드에서 음성을 감지하고 있습니다")
            .setSmallIcon(R.drawable.record_ic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 사용자가 쉽게 지울 수 없도록 설정
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupWebSocketConnection()
    }

    private fun setupWebSocketConnection() {
        reconnectionScope.launch {
            while (true) {
                try {
                    if (!webSocketService.getConnectionState()) { 
                        Log.d(TAG, "WebSocket 연결 시도")
                        webSocketService.initialize()
                    }
                     // 5초마다 연결 상태 확인
                    delay(5000)
                } catch (e: Exception) {
                    Log.e(TAG, "WebSocket 연결 실패, 재시도 예정", e)
                    delay(1000)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startRecording()
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (!isRecording) {
            isRecording = true
            serviceScope.launch {
                try {
                    audioRecorder.startRecording()
                } catch (e: Exception) {
                    Log.e(TAG, "녹음 시작 실패", e)
                    handleRecordingError(e)
                }
            }
        }
    }

    private fun handleRecordingError(error: Exception) {
        Log.e(TAG, "녹음 중 오류 발생", error)
        serviceScope.launch {
            delay(3000)
            startRecording()
        }
    }


    private fun stopRecording() {
        Log.d(TAG, "녹음 중지")
        isRecording = false
        audioRecorder.stopRecording()
        webSocketService.shutdown() 
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        reconnectionJob.cancel()
        stopRecording()
    }
}