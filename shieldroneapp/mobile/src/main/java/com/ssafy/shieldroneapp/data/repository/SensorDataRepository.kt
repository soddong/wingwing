package com.ssafy.shieldroneapp.data.repository

import android.util.Log
import com.ssafy.shieldroneapp.data.model.HeartRateData
import com.ssafy.shieldroneapp.data.source.remote.WebSocketService

class SensorDataRepository(
    private val webSocketService: WebSocketService
) {
    companion object {
        private const val TAG = "SensorDataRepository"
    }

    suspend fun processHeartRateData(data: HeartRateData) {
        try {
            webSocketService.sendHeartRateData(data)
            Log.d(TAG, "심박수 데이터가 처리되어 서버에 전송되었습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "심박수 데이터 처리 중 오류 발생", e)
        }
    }
}
