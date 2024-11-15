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
import android.os.VibratorManager
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
    private val context: Context,
) {
    companion object {
        private const val TAG = "모바일: 알림 서비스"
        private const val ALERT_CHANNEL_ID = "alert_channel"
        private const val ALERT_CHANNEL_NAME = "위험 알림"
        private const val WARNING_NOTIFICATION_ID = 2000
        private const val SAFE_CONFIRMATION_NOTIFICATION_ID = 2001

        // 위험 감지용 진동 패턴 (0: 대기, 300: 진동, 150: 대기, 300: 진동)
        private val WARNING_VIBRATION_PATTERN = longArrayOf(0, 300, 150, 300)

        // 타인 감지용 진동 패턴 (한 번만 짧게)
        private val OBJECT_VIBRATION_PATTERN = longArrayOf(0, 100)

        // 진동 세기 (안드로이드 O 이상)
        private val WARNING_VIBRATION_AMPLITUDE = intArrayOf(0, 255, 0, 255)  // 최대 세기
        private val OBJECT_VIBRATION_AMPLITUDE = intArrayOf(0, 100)  // 약한 세기

        // 소음 감지용 진동 패턴
        private val NOISE_VIBRATION_PATTERN = longArrayOf(0, 50)
        private val NOISE_VIBRATION_AMPLITUDE = intArrayOf(0, 50)  // 가장 약한 세기
    }

    private var mediaPlayer: MediaPlayer? = null
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

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
                vibrationPattern = WARNING_VIBRATION_PATTERN
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun handleWarningBeep(warningFlag: Boolean) {
        if (warningFlag) {
            startWarningSound()
            showWarningNotification()
            startVibration(VibrationType.WARNING)
        } else {
            stopWarningSound()
            cancelWarningNotification()
            stopVibration()
        }
    }

    fun handleObjectBeep(objectFlag: Boolean) {
        if (objectFlag) {
            startWarningSound()
            startVibration(VibrationType.OBJECT)
        } else {
            stopWarningSound()
            stopVibration()
        }
    }

    private enum class VibrationType {
        WARNING, OBJECT, NOISE
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

    private fun stopVibration() {
        vibrator.cancel()
        Log.d(TAG, "진동 중지")
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

    private fun cancelWarningNotification() {
        notificationManager.cancel(WARNING_NOTIFICATION_ID)
        Log.d(TAG, "위험 알림 취소")
    }

    private fun startVibration(type: VibrationType) {
        CoroutineScope(Dispatchers.Default).launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (type) {
                    VibrationType.WARNING -> {
                        val effect = VibrationEffect.createWaveform(
                            WARNING_VIBRATION_PATTERN,
                            WARNING_VIBRATION_AMPLITUDE,
                            -1
                        )
                        vibrator.vibrate(effect)
                        Log.d(TAG, "위험 감지 진동 시작 - 강한 세기")
                    }

                    VibrationType.OBJECT -> {
                        val effect = VibrationEffect.createWaveform(
                            OBJECT_VIBRATION_PATTERN,
                            OBJECT_VIBRATION_AMPLITUDE,
                            -1
                        )
                        vibrator.vibrate(effect)
                        Log.d(TAG, "타인 감지 진동 시작 - 약한 세기")
                    }

                    VibrationType.NOISE -> {
                        val effect = VibrationEffect.createWaveform(
                            NOISE_VIBRATION_PATTERN,
                            NOISE_VIBRATION_AMPLITUDE,
                            -1
                        )
                        vibrator.vibrate(effect)
                        Log.d(TAG, "소음 감지 진동 시작 - 가장 약한 세기")
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    VibrationType.WARNING -> {
                        vibrator.vibrate(WARNING_VIBRATION_PATTERN, -1)
                    }

                    VibrationType.OBJECT -> {
                        vibrator.vibrate(OBJECT_VIBRATION_PATTERN, -1)
                    }

                    VibrationType.NOISE -> {
                        vibrator.vibrate(NOISE_VIBRATION_PATTERN, -1)
                    }
                }
            }
        }
    }

    fun showSafeConfirmationNotification(title: String, message: String) {
        cancelWarningNotification()

        stopWarningSound()
        stopVibration()

        startVibration(VibrationType.NOISE)

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.alert_ic)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SAFE_CONFIRMATION_NOTIFICATION_ID, notification)
        Log.d(TAG, "안전 확인 알림 표시: $title - $message")
    }

    fun handleWarningBeep(warningFlag: Boolean, isSafeConfirmed: Boolean = false) {
        if (warningFlag && !isSafeConfirmed) {
            startWarningSound()
            showWarningNotification()
            startVibration(VibrationType.WARNING)
        } else {
            stopWarningSound()
            cancelWarningNotification()
            stopVibration()
        }
    }

    fun release() {
        stopWarningSound()
        cancelWarningNotification()
        notificationManager.cancel(SAFE_CONFIRMATION_NOTIFICATION_ID)
        stopVibration()
    }
}