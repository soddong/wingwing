package com.ssafy.shieldroneapp.data.repository

import android.util.Log
import com.ssafy.shieldroneapp.data.model.AlertData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor() {
    private val _alertState = MutableStateFlow<AlertData?>(null)
    val alertState: StateFlow<AlertData?> = _alertState

    companion object {
        private const val TAG = "모바일: 알림 레포지토리"
    }

    private val _isSafeConfirmed = MutableStateFlow(false)
    val isSafeConfirmed: StateFlow<Boolean> = _isSafeConfirmed

    fun updateWarningAlert() {
        Log.d(TAG, "경고 알림 상태 업데이트")
        _alertState.value = AlertData(
            warningFlag = true,
            objectFlag = false,
            timestamp = System.currentTimeMillis()
        )
    }

    fun updateObjectAlert() {
        Log.d(TAG, "물체 감지 알림 상태 업데이트")
        _alertState.value = AlertData(
            warningFlag = false,
            objectFlag = true,
            timestamp = System.currentTimeMillis()
        )
    }

    fun updateSafeConfirmation(isConfirmed: Boolean) {
        _isSafeConfirmed.value = isConfirmed
        if (isConfirmed) {
            clearAlert()
        }
    }

    fun clearAlert() {
        _alertState.value = null
        Log.d(TAG, "알림 활성화 취소")
        _isSafeConfirmed.value = true
    }
}