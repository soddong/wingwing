package com.ssafy.shieldroneapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.ssafy.shieldroneapp.data.model.DataAvailability
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.repository.DataRepository
import com.ssafy.shieldroneapp.data.repository.SensorRepository
import com.ssafy.shieldroneapp.data.repository.MeasureMessage
import androidx.health.services.client.data.DataTypeAvailability
import kotlinx.coroutines.*

class HeartRateService : LifecycleService() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var sensorRepository: SensorRepository
    private lateinit var dataRepository: DataRepository
    private var measurementJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "HeartRateService"
        private const val CHANNEL_NAME = "Heart Rate Monitoring"
        private const val TAG = "워치: 심박수 레포"
    }

    override fun onCreate() {
        super.onCreate()
        sensorRepository = SensorRepository(this)
        dataRepository = DataRepository(this)
        acquireWakeLock()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ShieldDrone::HeartRateServiceWakeLock"
        ).apply {
            acquire()
        }
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
                        sensorRepository.heartRateMeasureFlow()
                            .collect { measureMessage ->
                                when (measureMessage) {
                                    is MeasureMessage.MeasureData -> {
                                        val bpm = measureMessage.data.last().value
                                        if (bpm > 0) { 
                                            val viewModel = WearableService.getHeartRateViewModel()
                                            if (viewModel != null) {
                                                viewModel.updateHeartRate(bpm)
                                            }
                                            sendHeartRateData(bpm, DataTypeAvailability.AVAILABLE)
                                        } else {
                                            delay(1000) 
                                        }
                                    }
                                    is MeasureMessage.MeasureAvailability -> {
                                        val newAvailability = measureMessage.availability
                                        Log.d(TAG, "가용성 변경: $newAvailability")
                                        if (newAvailability == DataTypeAvailability.UNAVAILABLE) {
                                            Log.d(TAG, "센서 연결 끊김, 재연결 시도")
                                            delay(1000) 
                                        }
                                        WearableService.getHeartRateViewModel()?.updateAvailability(newAvailability)
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "측정 중 오류 발생, 재시도", e)
                        delay(1000) 
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "서비스 실행 중 치명적 오류 발생", e)
                delay(1000)
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