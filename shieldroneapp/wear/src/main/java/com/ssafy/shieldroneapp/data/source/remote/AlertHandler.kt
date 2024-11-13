package com.ssafy.shieldroneapp.data.source.remote

import android.util.Log
import com.google.gson.Gson
import com.ssafy.shieldroneapp.data.model.AlertData
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertHandler @Inject constructor(
    private val alertRepository: AlertRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    companion object {
        private const val TAG = "워치: 알림 핸들러"
    }

    private val gson = Gson()

    fun handleDangerAlert(alertJson: String) {
        try {
            val alertData = gson.fromJson(alertJson, AlertData::class.java)
            Log.d(TAG, "위험 알림 수신 및 변환 성공 - time: ${alertData.time}, warning: ${alertData.warningFlag}")

            scope.launch {
                if (alertData.warningFlag) {
                    alertRepository.processDangerAlert(alertData)
                } else {
                    alertRepository.clearAlert()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "위험 알림 처리 실패", e)
            e.printStackTrace()
        }
    }
    fun handleObjectAlert(alertJson: String) {
        try {
            val alertData = gson.fromJson(alertJson, AlertData::class.java)
            Log.d(TAG, "위험 알림 수신 및 변환 성공 - time: ${alertData.time}, object: ${alertData.objectFlag}")

            scope.launch {
                if (alertData.objectFlag) {
                    alertRepository.processObjectAlert(alertData)
                } else {
                    alertRepository.clearAlert()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "위험 알림 처리 실패", e)
            e.printStackTrace()
        }
    }
}