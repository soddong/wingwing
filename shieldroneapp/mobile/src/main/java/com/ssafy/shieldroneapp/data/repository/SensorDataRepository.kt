package com.ssafy.shieldroneapp.data.repository

import android.util.Log
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService
import com.ssafy.shieldroneapp.viewmodels.HeartRateViewModel
import javax.inject.Inject

class SensorDataRepository @Inject constructor(
    private val webSocketService: WebSocketService,
    private val heartRateViewModel: HeartRateViewModel
) {
    companion object {
        private const val TAG = "모바일: 심박수 데이터 레포"
    }

    suspend fun processHeartRateData(data: HeartRateData) {
        try {
            webSocketService.sendHeartRateData(data)
            heartRateViewModel.updateHeartRate(data.pulseFlag)
            Log.d(TAG, "심박수 데이터가 처리되어 서버에 전송되었습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 처리 중 오류 발생", e)
        }
    }
}