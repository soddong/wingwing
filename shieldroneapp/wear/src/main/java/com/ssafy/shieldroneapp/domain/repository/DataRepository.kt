package com.ssafy.shieldroneapp.domain.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.domain.model.HeartRateData
import com.ssafy.shieldroneapp.data.remote.WearConnectionManager
import com.ssafy.shieldroneapp.core.utils.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepository @Inject constructor(
    private val context: Context,
) : WearConnectionManager.MonitoringCallback {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private var isMonitoring = false
    private var PATH_HEART_RATE = "/sendPulseFlag"

    @Inject
    fun init(wearConnectionManager: WearConnectionManager) {
        wearConnectionManager.setMonitoringCallback(this)
    }

    override fun pauseMonitoring() {
        isMonitoring = false
    }

    override fun resumeMonitoring() {
        isMonitoring = true
    }

    companion object {
        private const val THRESHOLD_BPM = 75.0
        private const val SUSTAINED_DURATION = 10000L
        private const val TAG = "워치: 데이터 레포"
    }

    private var highBpmStartTime: Long? = null
    private var lastTransmissionTime = 0L
    private var isCurrentlyHighBpm = false

    suspend fun sendHeartRateData(heartRateData: HeartRateData) {
        if (!isMonitoring) {
            Log.d(TAG, "심박수 모니터링이 일시 중지된 상태입니다")
            return
        }

        try {
            val currentTime = System.currentTimeMillis()
            val currentBpm = heartRateData.bpm

            sendRegularHeartRateData(heartRateData)

            when {
                currentBpm >= THRESHOLD_BPM -> {
                    handleHighHeartRate(currentTime, heartRateData)
                }

                else -> {
                    if (isCurrentlyHighBpm) {
                        isCurrentlyHighBpm = false
                        highBpmStartTime = null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "데이터 전송 중 오류 발생", e)
            e.printStackTrace()
        }
    }

    private suspend fun sendRegularHeartRateData(heartRateData: HeartRateData) {
        try {
            val putDataMapRequest = PutDataMapRequest.create(PATH_HEART_RATE).apply {
                dataMap.putDouble("bpm", heartRateData.bpm)
                dataMap.putLong("timestamp", heartRateData.timestamp)
                dataMap.putString("availability", heartRateData.availability.name)
            }

            val putDataRequest = putDataMapRequest.asPutDataRequest().apply {
                setUrgent()
            }

            val result = dataClient.putDataItem(putDataRequest).await(5000)
            Log.d(TAG, "심박수 데이터 전송 성공 - URI: ${result.uri}")
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 전송 실패", e)
            e.printStackTrace()
        }
    }

    private suspend fun handleHighHeartRate(currentTime: Long, heartRateData: HeartRateData) {
        if (!isCurrentlyHighBpm) {
            highBpmStartTime = currentTime
            isCurrentlyHighBpm = true
            Log.d(TAG, "높은 심박수 감지됨, 타이머 시작")
        } else {
            highBpmStartTime?.let { startTime ->
                val duration = currentTime - startTime
                Log.d(TAG, "높은 심박수 지속 시간: ${duration}ms")

                if (duration >= SUSTAINED_DURATION) {
                    if (currentTime - lastTransmissionTime >= SUSTAINED_DURATION) {
                        val success = sendAlertData(true, heartRateData.timestamp)
                        if (success) {
                            lastTransmissionTime = currentTime
                            Log.d(TAG, "알림 데이터 전송 성공")
                        }
                    }
                }
            }
        }
    }

    private suspend fun sendAlertData(pulseFlag: Boolean, timestamp: Long): Boolean {
        try {
            Log.d(TAG, "알림 데이터 전송 시작 - pulseFlag: $pulseFlag")
            val putDataMapRequest = PutDataMapRequest.create("/sendPulseFlag").apply {
                dataMap.putBoolean("pulseFlag", pulseFlag)
                dataMap.putLong("timestamp", timestamp)
                dataMap.putBoolean("sustained", true)
            }

            val putDataRequest = putDataMapRequest.asPutDataRequest().apply {
                setUrgent()
            }

            val result = dataClient.putDataItem(putDataRequest).await(5000)
            Log.d(TAG, "알림 데이터 전송 완료 - URI: ${result.uri}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "알림 데이터 전송 실패", e)
            e.printStackTrace()
            return false
        }
    }
}