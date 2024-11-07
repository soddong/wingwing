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
        private const val THRESHOLD_BPM = 75.0
        private const val SUSTAINED_DURATION = 10000L
        private const val TAG = "워치: 데이터 레포"
    }

    private var highBpmStartTime: Long? = null
    private var lastTransmissionTime = 0L
    private var isCurrentlyHighBpm = false

    suspend fun sendHeartRateData(heartRateData: HeartRateData) {
        try {
            val currentTime = System.currentTimeMillis()
            val currentBpm = heartRateData.bpm

            Log.d(TAG, "심박수 데이터 전송 시도 - BPM: $currentBpm")

            when {
                currentBpm >= THRESHOLD_BPM -> {
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
                                    val success = sendData(true, heartRateData.timestamp)
                                    if (success) {
                                        lastTransmissionTime = currentTime
                                        Log.d(TAG, "데이터 전송 성공")
                                    }
                                } else {
                                    Log.d(TAG, "전송 대기 중... (마지막 전송 후 ${currentTime - lastTransmissionTime}ms)")
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (isCurrentlyHighBpm) {
                        isCurrentlyHighBpm = false
                        highBpmStartTime = null
                        val success = sendData(false, heartRateData.timestamp)
                        if (success) {
                            Log.d(TAG, "정상 심박수 데이터 전송 성공")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "데이터 전송 중 오류 발생", e)
            e.printStackTrace()
        }
    }

    private suspend fun sendData(pulseFlag: Boolean, timestamp: Long): Boolean {
        try {
            Log.d(TAG, "데이터 전송 시작 - pulseFlag: $pulseFlag")
            val putDataMapRequest = PutDataMapRequest.create("/sendPulseFlag").apply {
                dataMap.putBoolean("pulseFlag", pulseFlag)
                dataMap.putLong("timestamp", timestamp)
                dataMap.putBoolean("sustained", true)
            }

            val putDataRequest = putDataMapRequest.asPutDataRequest().apply {
                setUrgent()
            }

            val result = dataClient.putDataItem(putDataRequest).await()
            Log.d(TAG, "데이터 아이템 전송 완료 - URI: ${result.uri}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "데이터 전송 실패", e)
            e.printStackTrace()
            return false
        }
    }
}