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

    // 로컬에서 데이터 주기적으로 가져와 전송하는 메서드
    fun startSendingLocalHeartRateData(intervalMillis: Long = 5000L) {
        coroutineScope.launch {
            while (isActive) {
                val heartRateData = localDataSource.getHeartRateData() 
                
                if (heartRateData != null) {
                    // 데이터를 웹소켓으로 전송
                    processHeartRateData(heartRateData)
                } else {
//                    Log.d(TAG, "로컬에 심박수 데이터가 없습니다.")
                }
                delay(intervalMillis)
            }
        }
    }
    
    // processHeartRateData에서 웹소켓으로 데이터 전송
    suspend fun processHeartRateData(data: HeartRateData) {
        try {
            webSocketService.sendHeartRateData(data)
            // 심박수 상태 업데이트
            heartRateViewModel.updateHeartRate(data.pulseFlag)
            Log.d(TAG, "심박수 데이터가 처리되어 서버에 전송되었습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 처리 중 오류 발생", e)
            // 에러가 발생해도 로컬에 저장만 하고 연결은 유지
            localDataSource.saveHeartRateData(data)
        }
    }



    // 데이터 전송 중지 메서드
    fun stopSendingData() {
        coroutineScope.coroutineContext.cancelChildren()
    }
}
