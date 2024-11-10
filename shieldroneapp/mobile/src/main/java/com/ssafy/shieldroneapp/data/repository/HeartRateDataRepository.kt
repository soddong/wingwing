package com.ssafy.shieldroneapp.data.repository

import android.util.Log
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.source.local.HeartRateLocalDataSource
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.viewmodels.HeartRateViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

class HeartRateDataRepository @Inject constructor(
    private val webSocketService: WebSocketService,
    private val heartRateViewModel: HeartRateViewModel,
    private val localDataSource: HeartRateLocalDataSource
) {
    companion object {
        private const val TAG = "모바일: 심박수 데이터 레포"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    fun startSendingLocalHeartRateData(intervalMillis: Long = 5000L) {
        coroutineScope.launch {
            while (isActive) {
                val heartRateData = localDataSource.getHeartRateData() 
                
                if (heartRateData != null) {
                    processHeartRateData(heartRateData)
                } else {
                   Log.d(TAG, "로컬에 심박수 데이터가 없습니다.")
                }
                delay(intervalMillis)
            }
        }
    }
    
    suspend fun processHeartRateData(data: HeartRateData) {
        try {
            webSocketService.sendHeartRateData(data)
            // Boolean을 Int로 변환 (true -> 1, false -> 0)
            val pulseValue = if (data.pulseFlag) 1 else 0
            heartRateViewModel.updateHeartRate(pulseValue)
            Log.d(TAG, "심박수 데이터가 처리되어 서버에 전송되었습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 처리 중 오류 발생", e)
            localDataSource.saveHeartRateData(data)
        }
    }



    fun stopSendingData() {
        coroutineScope.coroutineContext.cancelChildren()
    }
}
