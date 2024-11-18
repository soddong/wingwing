package com.ssafy.shieldroneapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.ssafy.shieldroneapp.domain.model.DataAvailability
import com.ssafy.shieldroneapp.domain.model.HeartRateData
import com.ssafy.shieldroneapp.domain.repository.DataRepository
import com.ssafy.shieldroneapp.domain.repository.SensorRepository
import com.ssafy.shieldroneapp.domain.repository.MeasureMessage
import androidx.health.services.client.data.DataTypeAvailability
import com.ssafy.shieldroneapp.core.utils.WakeLockManager
import com.ssafy.shieldroneapp.data.remote.WearConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class HeartRateService : LifecycleService() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var measurementJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastMeasurementTime = 0L

    @Inject
    lateinit var sensorRepository: SensorRepository

    @Inject
    lateinit var dataRepository: DataRepository

    @Inject
    lateinit var wearConnectionManager: WearConnectionManager

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "HeartRateService"
        private const val CHANNEL_NAME = "Heart Rate Monitoring"
        private const val TAG = "워치: 심박수 레포"
        private const val MEASUREMENT_INTERVAL = 1000L
    }

    override fun onCreate() {
        super.onCreate()
        WakeLockManager.getInstance(applicationContext).acquireWakeLock()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startHeartRateMonitoring()
        return START_STICKY
    }

    private fun startHeartRateMonitoring() {
        measurementJob?.cancel()
        measurementJob = serviceScope.launch {
            try {
                if (!sensorRepository.hasHeartRateCapability()) {
                    Log.e(TAG, "심박수 센서를 지원하지 않습니다")
                    stopSelf()
                    return@launch
                }

                while (isActive) {
                    try {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastMeasurementTime >= MEASUREMENT_INTERVAL) {
                            sensorRepository.heartRateMeasureFlow()
                                .collect { measureMessage ->
                                    when (measureMessage) {
                                        is MeasureMessage.MeasureData -> {
                                            measureMessage.data.lastOrNull()?.let { heartRateSample ->
                                                val bpm = heartRateSample.value
                                                if (bpm > 0) {
                                                    WearableService.getHeartRateViewModel()?.updateHeartRate(bpm)
                                                    sendHeartRateData(bpm, DataTypeAvailability.AVAILABLE)
                                                    lastMeasurementTime = currentTime
                                                }
                                            }
                                        }
                                        is MeasureMessage.MeasureAvailability -> {
                                            val availability = measureMessage.availability
                                            Log.d(TAG, "가용성 변경: $availability")
                                            WearableService.getHeartRateViewModel()?.updateAvailability(availability)
                                            if (availability == DataTypeAvailability.UNAVAILABLE) {
                                                Log.d(TAG, "센서 연결 끊김, 재연결 시도")
                                            }
                                        }
                                    }
                                }
                        }
                        delay(MEASUREMENT_INTERVAL)
                    } catch (e: Exception) {
                        Log.e(TAG, "측정 중 오류 발생, 재시도", e)
                        delay(MEASUREMENT_INTERVAL)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "서비스 실행 중 치명적 오류 발생", e)
                delay(MEASUREMENT_INTERVAL)
                startHeartRateMonitoring()
            }
        }
    }

    private suspend fun sendHeartRateData(bpm: Double, availability: DataTypeAvailability) {
        val heartRateData = HeartRateData(
            bpm = bpm,
            availability = when (availability) {
                DataTypeAvailability.AVAILABLE -> DataAvailability.AVAILABLE
                DataTypeAvailability.ACQUIRING -> DataAvailability.ACQUIRING
                DataTypeAvailability.UNAVAILABLE -> DataAvailability.UNAVAILABLE
                else -> DataAvailability.UNKNOWN
            }
        )
        dataRepository.sendHeartRateData(heartRateData)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "심박수 모니터링 중"
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("심박수 모니터링")
        .setContentText("백그라운드에서 심박수를 측정하고 있습니다")
        .setSmallIcon(com.google.android.horologist.tiles.R.drawable.ic_nordic )
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        measurementJob?.cancel()
        serviceScope.cancel()
        wakeLock?.release()

        val intent = Intent(this, HeartRateService::class.java)
        startService(intent)
    }
}