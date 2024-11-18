package com.ssafy.shieldroneapp.data.remote

import android.util.Log
import com.google.gson.Gson
import com.ssafy.shieldroneapp.domain.model.AlertData
import com.ssafy.shieldroneapp.domain.repository.AlertRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
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
            val jsonObject = JSONObject(alertJson)
            val alertData = AlertData(
                time = jsonObject.optLong("time", System.currentTimeMillis()),
                warningFlag = jsonObject.optBoolean("warningFlag", false),
                objectFlag = jsonObject.optBoolean("objectFlag", false),
                isProcessed = false,
                frame = if (jsonObject.has("frame")) alertJson else null
            )

            Log.d(TAG, "위험 알림 수신 및 변환 성공 - time: ${alertData.time}, warning: ${alertData.warningFlag}, frame: ${if (alertData.frame != null) "있음" else "없음"}")

            scope.launch {
                if (alertData.warningFlag) {
                    alertRepository.processDangerAlert(alertData)
                    alertRepository.updateSafeConfirmation(false)
                } else {
                    handleAlertClear(alertData.frame != null)
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
                    handleAlertClear(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "위험 알림 처리 실패", e)
            e.printStackTrace()
        }
    }

    fun updateSafeConfirmation(isConfirmed: Boolean) {
        if (isConfirmed) {
            val currentAlert = alertRepository.currentAlert.value
            if (currentAlert?.frame != null) {
                scope.launch {
                    alertRepository.updateSafeConfirmation(true)
                }
            } else {
                dismissAlert()
                scope.launch {
                    alertRepository.updateSafeConfirmation(true)
                }
            }
        }
    }

    private fun handleAlertClear(hasFrame: Boolean) {
        if (!hasFrame) {
            scope.launch {
                alertRepository.clearAlert()
            }
        }
    }

    fun dismissAlert() {
        scope.launch {
            alertRepository.clearAlert()
        }
    }
}