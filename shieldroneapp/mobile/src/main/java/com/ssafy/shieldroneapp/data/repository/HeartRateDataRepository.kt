package com.ssafy.shieldroneapp.data.repository

import android.util.Log
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.source.local.HeartRateLocalDataSource
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.viewmodels.HeartRateViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class HeartRateDataRepository @Inject constructor(
    private val webSocketService: WebSocketService,
    private val localDataSource: HeartRateLocalDataSource,
    private val heartRateViewModel: HeartRateViewModel
) {
    companion object {
        private const val TAG = "모바일: 심박수 데이터 레포"
        private const val THRESHOLD_BPM = 75.0
        private const val SUSTAINED_DURATION = 10000L
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private var highBpmStartTime: Long? = null
    private var isCurrentlyHighBpm = false
    private var lastProcessedBpm: Double = 0.0

    private val _heartRateFlow = MutableStateFlow(0.0)
    val heartRateFlow: StateFlow<Double> = _heartRateFlow.asStateFlow()

    fun startSendingLocalHeartRateData(intervalMillis: Long = 5000L) {
        coroutineScope.launch {
            while (isActive) {
                val heartRateData = localDataSource.getHeartRateData()
                if (heartRateData != null) {
                    // bpm이 0이면 마지막으로 처리된 값 사용
                    if (heartRateData.bpm <= 0 && lastProcessedBpm > 0) {
                        processHeartRateData(heartRateData.copy(bpm = lastProcessedBpm))
                    } else if (heartRateData.bpm > 0) {
                        processHeartRateData(heartRateData)
                    }
                } else {
                    Log.d(TAG, "로컬에 심박수 데이터가 없습니다.")
                }
                delay(intervalMillis)
            }
        }
    }

    suspend fun processHeartRateData(data: HeartRateData) {
        try {
            if (data.bpm > 0) {
                lastProcessedBpm = data.bpm
            }

            val currentTime = System.currentTimeMillis()

            when {
                data.bpm >= THRESHOLD_BPM -> {
                    if (!isCurrentlyHighBpm) {
                        highBpmStartTime = currentTime
                        isCurrentlyHighBpm = true
                        webSocketService.sendHeartRateData(
                            data.copy(
                                pulseFlag = true,
                                timestamp = currentTime
                            )
                        )
                    }
                }
                else -> {
                    if (isCurrentlyHighBpm) {
                        isCurrentlyHighBpm = false
                        highBpmStartTime = null
                        webSocketService.sendHeartRateData(
                            data.copy(
                                pulseFlag = false,
                                timestamp = currentTime
                            )
                        )
                    }
                }
            }

            localDataSource.saveHeartRateData(data)
            Log.d(TAG, "processHeartRateData: processing 심박수 = ${data.bpm}")

            // 심박수 업데이트
            heartRateViewModel.updateHeartRate(data.bpm)

        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 처리 중 오류 발생", e)
        }
    }

    suspend fun getLastHeartRate(): Double? {
        return try {
            val lastData = localDataSource.getHeartRateData()
            lastData?.bpm
        } catch (e: Exception) {
            Log.e(TAG, "마지막 심박수 데이터 조회 실패", e)
            null
        }
    }

    fun stopSendingData() {
        coroutineScope.coroutineContext.cancelChildren()
    }
}
