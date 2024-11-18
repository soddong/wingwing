package com.ssafy.shieldroneapp.domain.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.ssafy.shieldroneapp.domain.model.AlertData
import com.ssafy.shieldroneapp.core.utils.await
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "워치: 알림 저장소"
        private const val KEY_TIMESTAMP = "timestamp"
    }

    private val _currentAlert = MutableStateFlow<AlertData?>(null)
    val currentAlert: StateFlow<AlertData?> = _currentAlert.asStateFlow()

    private var lastSentTimestamp = 0L

    suspend fun processDangerAlert(alertData: AlertData) {
        Log.d(TAG, "⚠️ 위험 알림 활성화 - time: ${alertData.time}")
        _currentAlert.value = alertData
        sendDangerFlag(true)
    }

    suspend fun processObjectAlert(alertData: AlertData) {
        Log.d(TAG, "⚠️ 물체 감지 알림 활성화 - time: ${alertData.time}")
        _currentAlert.value = alertData
        sendObjectFlag(true)
    }

    suspend fun clearAlert() {
        Log.d(TAG, "위험 알림 해제")
        _currentAlert.value = null
        sendDangerFlag(false)
    }

    private suspend fun sendDangerFlag(flag: Boolean) {
        try {
            val currentTime = System.currentTimeMillis()
            // 마지막 전송 후 500ms 이내의 중복 요청은 무시
            if (currentTime - lastSentTimestamp < 500) {
                Log.d(TAG, "중복 전송 방지: 이전 전송 후 500ms 이내")
                return
            }

            val request = PutDataMapRequest.create("/dangerAlert").apply {
                dataMap.putBoolean("dangerFlag", flag)
                dataMap.putLong(KEY_TIMESTAMP, currentTime)
            }
            val putDataReq = request.asPutDataRequest()
            putDataReq.setUrgent()

            Wearable.getDataClient(context).putDataItem(putDataReq).await(5000)
            lastSentTimestamp = currentTime
            Log.d(TAG, "위험 알림 플래그 전송 성공: $flag")
        } catch (e: Exception) {
            Log.e(TAG, "위험 알림 플래그 전송 실패", e)
        }
    }

    private suspend fun sendObjectFlag(flag: Boolean) {
        try {
            val currentTime = System.currentTimeMillis()
            // 마지막 전송 후 500ms 이내의 중복 요청은 무시
            if (currentTime - lastSentTimestamp < 500) {
                Log.d(TAG, "중복 전송 방지: 이전 전송 후 500ms 이내")
                return
            }

            val request = PutDataMapRequest.create("/objectAlert").apply {
                dataMap.putBoolean("objectFlag", flag)
                dataMap.putLong(KEY_TIMESTAMP, currentTime)
            }
            val putDataReq = request.asPutDataRequest()
            putDataReq.setUrgent()

            Wearable.getDataClient(context).putDataItem(putDataReq).await(5000)
            lastSentTimestamp = currentTime 
            Log.d(TAG, "물체 감지 알림 플래그 전송 성공: $flag")
        } catch (e: Exception) {
            Log.e(TAG, "물체 감지 플래그 전송 실패", e)
        }
    }

    private val _isSafeConfirmed = MutableStateFlow(false)
    val isSafeConfirmed: StateFlow<Boolean> = _isSafeConfirmed

    fun updateSafeConfirmation(isConfirmed: Boolean) {
        _isSafeConfirmed.value = isConfirmed
        if (isConfirmed) {
            _currentAlert.value = null 
        }
    }
}