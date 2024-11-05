package com.ssafy.shieldroneapp.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.utils.await

class DataRepository(private val context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    companion object {
        private const val THRESHOLD_BPM = 82.0
        private const val SUSTAINED_DURATION = 10000L 
        private const val TAG = "DataRepository"
    }

    private var highBpmStartTime: Long? = null
    private var lastTransmissionTime = 0L
    private var isCurrentlyHighBpm = false

    suspend fun sendHeartRateData(heartRateData: HeartRateData) {
        try {
            val currentTime = System.currentTimeMillis()
            val currentBpm = heartRateData.bpm

            when {
                currentBpm >= THRESHOLD_BPM -> {
                    if (!isCurrentlyHighBpm) {
                        highBpmStartTime = currentTime
                        isCurrentlyHighBpm = true
                        Log.d(TAG, "현재 심박수: $currentBpm, 타이머 시작")
                    } else {
                        highBpmStartTime?.let { startTime ->
                            val duration = currentTime - startTime
                            if (duration >= SUSTAINED_DURATION) {
                                if (currentTime - lastTransmissionTime >= SUSTAINED_DURATION) {
                                    sendData(true, heartRateData.timestamp)
                                    lastTransmissionTime = currentTime
                                    Log.d(TAG, "10초이상 높은 심박수 유지됨: 데이터 전송")
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (isCurrentlyHighBpm) {
                        isCurrentlyHighBpm = false
                        highBpmStartTime = null
                        Log.d(TAG, "BPM dropped below threshold: $currentBpm")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 전송 에러", e)
            e.printStackTrace()
        }
    }

    private suspend fun sendData(pulseFlag: Boolean, timestamp: Long) {
        val putDataMapRequest = PutDataMapRequest.create("/sendPulseFlag").apply {
            dataMap.putBoolean("pulseFlag", pulseFlag)
            dataMap.putLong("timestamp", timestamp)
            dataMap.putBoolean("sustained", true)
        }
        val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()
        dataClient.putDataItem(putDataRequest).await()
        Log.d(TAG, "Heart rate data sent: pulseFlag=$pulseFlag, sustained=true")
    }
}