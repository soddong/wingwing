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
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Todo: 임시 아이콘 삽입
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 사용자가 쉽게 지울 수 없도록 설정
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "서비스 생성")
        createNotificationChannel()
        webSocketService.initialize()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_RECORDING -> {
                startRecording()
                // 녹음 시작 시 포그라운드 서비스 시작
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_STOP_RECORDING -> stopRecording()
            null -> Log.w(TAG, "Null intent received")
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (!isRecording) {
            isRecording = true
            Log.d(TAG, "녹음 시작")
            serviceScope.launch {
                try {
                    audioRecorder.startRecording()
                } catch (e: Exception) {
                    Log.e(TAG, "녹음 시작 중 오류 발생", e)
                    stopRecording()
                }
            }
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            Log.d(TAG, "녹음 중지")
            isRecording = false
            audioRecorder.stopRecording()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "서비스 종료")
        stopRecording()
        serviceScope.cancel()
        super.onDestroy()
    }
}

