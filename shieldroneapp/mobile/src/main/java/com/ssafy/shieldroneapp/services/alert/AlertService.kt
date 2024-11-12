package com.ssafy.shieldroneapp.services.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ssafy.shieldroneapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "모바일: 알림 서비스"
        private const val ALERT_CHANNEL_ID = "alert_channel"
        private const val ALERT_CHANNEL_NAME = "위험 알림"
        private const val WARNING_NOTIFICATION_ID = 2000
        private val VIBRATION_PATTERN = longArrayOf(0, 300, 150, 300)
    }

    private var mediaPlayer: MediaPlayer? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "위험 상황 알림을 표시하는 채널입니다"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun handleWarningBeep(warningFlag: Boolean) {
        if (warningFlag) {
            startWarningSound()
            showWarningNotification()
            startWarningVibration()
        } else {
            stopWarningSound()
            cancelWarningNotification()
            stopWarningVibration()
        }
    }

    private fun startWarningSound() {
        if (mediaPlayer == null || !mediaPlayer!!.isPlaying) {
            mediaPlayer?.release()
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(context, notificationUri).apply {
                isLooping = false
                start()
            }
            Log.d(TAG, "경고음 재생 시작")
        }
    }

    private fun stopWarningSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        Log.d(TAG, "경고음 중지")
    }

    private fun showWarningNotification() {
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("위험 상황 감지!")
            .setContentText("주변 상황에 주의하세요.")
            .setSmallIcon(R.drawable.alert_ic)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        notificationManager.notify(WARNING_NOTIFICATION_ID, notification)
        Log.d(TAG, "위험 알림 표시")
    }

    private fun startWarningVibration() {
        CoroutineScope(Dispatchers.Default).launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        VIBRATION_PATTERN,
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VIBRATION_PATTERN, -1)
            }
            Log.d(TAG, "진동 시작")
        }
    }

    private fun stopWarningVibration() {
        vibrator.cancel()
        Log.d(TAG, "진동 중지")
    }

    private fun cancelWarningNotification() {
        notificationManager.cancel(WARNING_NOTIFICATION_ID)
        Log.d(TAG, "위험 알림 취소")
    }

    fun release() {
        stopWarningSound()
        cancelWarningNotification()
        stopWarningVibration()
    }
}