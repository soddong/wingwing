package com.ssafy.shieldroneapp.data.repository

/**
 * 알람 데이터를 관리하는 리포지토리 클래스.
 *
 * 서버와의 WebSocket 통신을 통해 실시간으로 알람 데이터를 수신하고,
 * 수신된 알람 데이터를 처리하여 화면에 표시하거나 경고음을 발생시킨다.
 *
 * @property webSocketService 서버와의 실시간 통신을 위한 WebSocket 서비스 객체
 */

import com.ssafy.shieldroneapp.data.model.AlertData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor() {
    private val _alertState = MutableStateFlow<AlertData?>(null)
    val alertState: StateFlow<AlertData?> = _alertState

    fun updateAlertState(warningFlag: Boolean) {
        _alertState.value = AlertData(
            warningFlag = warningFlag,
            timestamp = System.currentTimeMillis()
        )
    }

    fun clearAlert() {
        _alertState.value = null
    }
}