package com.ssafy.shieldroneapp.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.utils.await
import kotlin.math.abs

class DataRepository(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    companion object {
        private const val THRESHOLD_BPM = 90.0
        private const val DELTA_THRESHOLD = 5.0
        private const val TRANSMISSION_INTERVAL = 5000L
        private const val TAG = "DataRepository"
    }

    private var previousBpm: Double? = null
    private var lastTransmissionTime = 0L

    suspend fun sendHeartRateData(heartRateData: HeartRateData) {
        try {
            val currentTime = System.currentTimeMillis()
            val currentBpm = heartRateData.bpm
            val pulseFlag = currentBpm >= THRESHOLD_BPM

            // 주기적 전송: 5초마다 데이터를 전송
            if (currentTime - lastTransmissionTime >= TRANSMISSION_INTERVAL) {
                sendData(pulseFlag, heartRateData.timestamp)
                lastTransmissionTime = currentTime
                return
            }

            // 임계값 변화에 따른 전송
            if (pulseFlag) {
                sendData(pulseFlag, heartRateData.timestamp)
                Log.d(TAG, "Heart rate above threshold: ${currentBpm} BPM")
            } else {
                Log.d(TAG, "Heart rate below threshold (${currentBpm} BPM), not sending")
            }

            // 변화량에 따른 전송: 이전 bpm과의 차이가 5 이상일 때 전송
            previousBpm?.let { previous ->
                if (abs(currentBpm - previous) >= DELTA_THRESHOLD) {
                    sendData(pulseFlag, heartRateData.timestamp)
                    Log.d(TAG, "Heart rate changed significantly: ${currentBpm} BPM (delta: ${currentBpm - previous})")
                }
            }

            previousBpm = currentBpm

        } catch (e: Exception) {
            Log.e(TAG, "Error sending heart rate data", e)
            e.printStackTrace()
        }
    }

    private suspend fun sendData(pulseFlag: Boolean, timestamp: Long) {
        val putDataMapRequest = PutDataMapRequest.create("/sendPulseFlag").apply {
            dataMap.putBoolean("pulseFlag", pulseFlag)
            dataMap.putLong("timestamp", timestamp)
        }
        val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()
        dataClient.putDataItem(putDataRequest).await()
        Log.d(TAG, "Heart rate data sent: pulseFlag=$pulseFlag")
    }
}