package com.ssafy.shieldroneapp.ui.map.screens

/**
 * 드론 배정 및 안내 종료, 위험 감지 등 다양한 상황에 따른 알림을 관리하는 클래스.
 *
 * 서버에서 수신한 위험 상황 데이터 및 드론 배정 상태를 처리하고,
 * `Alert` 컴포넌트를 사용해 사용자가 이해하기 쉽게 알림을 표시한다.
 *
 * 드론 배정 성공/실패/취소 시: Alert 드론 타입으로 알림 표시.
 * 위험 상황 감지 시: Alert 위험 감지 타입으로 3단계 알림 표시.
 * 목적지 도착 시: Alert 드론 타입으로 안내 종료 알림 표시.
 *
 * @property context 애플리케이션 컨텍스트 (알림을 표시하거나 시스템 리소스를 사용할 때 필요)
 */

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import com.ssafy.shieldroneapp.data.source.remote.SafetyMessageSender
import com.ssafy.shieldroneapp.data.source.remote.WebSocketSubscriptions
import com.ssafy.shieldroneapp.ui.components.AlertState
import com.ssafy.shieldroneapp.ui.components.AlertType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertHandler @Inject constructor(
    private val alertRepository: AlertRepository,
) {
    var alertState by mutableStateOf(AlertState())
        private set

    private val _isSafeConfirmed = mutableStateOf(false)
    val isSafeConfirmed: State<Boolean> = _isSafeConfirmed

    private val scope = CoroutineScope(Dispatchers.Main)
    private var alertJob: Job? = null

    private val _watchConfirmed = mutableStateOf(false)
    val watchConfirmed: State<Boolean> = _watchConfirmed

    fun handleSafeConfirmation() {
        val currentAlert = alertRepository.alertState.value
        val hasFrame = currentAlert?.frame != null
        val wasWarningActive = currentAlert?.warningFlag == true

        _isSafeConfirmed.value = true

        if (wasWarningActive) {
            if (!hasFrame) {
                dismissAlert()
            } else {
                currentAlert?.let { alert ->
                    alert.frame?.let { frame ->
                        alertRepository.updateWarningAlert(frame)
                        alertRepository.updateSafeConfirmation(true)
                    }
                }
            }
        }
    }

    fun handleWatchSafeConfirmation() {
        _watchConfirmed.value = true
        dismissAlert()
        _isSafeConfirmed.value = true
        alertRepository.updateSafeConfirmation(true)
    }

    fun resetWatchConfirmation() {
        _watchConfirmed.value = false
    }

    fun isWatchConfirmed(): Boolean = _watchConfirmed.value

    fun dismissAlert() {
        alertState = alertState.copy(isVisible = false)
        alertJob?.cancel()
        alertRepository.clearAlert()

        if (_isSafeConfirmed.value) {
            resetSafeConfirmation()
        }
        if (_watchConfirmed.value) {
            resetWatchConfirmation()
        }
    }

    fun showDangerAlert() {
        alertState = AlertState(
            isVisible = true,
            alertType = AlertType.WARNING,
            timestamp = System.currentTimeMillis()
        )
    }

    fun showObjectAlert() {
        alertState = AlertState(
            isVisible = true,
            alertType = AlertType.OBJECT,
            timestamp = System.currentTimeMillis()
        )
    }

    fun dismissObjectAlert() {
        alertState = alertState.copy(isVisible = false)
        alertRepository.clearAlert()
    }

    fun handleWarningBeep(warningFlag: Boolean) {
        if (warningFlag && !_isSafeConfirmed.value && !_watchConfirmed.value) {
            showDangerAlert()
            alertRepository.updateWarningAlert()
        } else {
            dismissAlert()
        }
    }

    fun handleObjectBeep(objectFlag: Boolean) {
        if (objectFlag) {
            showObjectAlert()
            alertRepository.updateObjectAlert()
        } else {
            dismissObjectAlert()
        }
    }

    fun setSafeConfirmation(isConfirmed: Boolean) {
        _isSafeConfirmed.value = isConfirmed
        if (isConfirmed) {
            dismissAlert()
        }
    }

    fun resetSafeConfirmation() {
        _isSafeConfirmed.value = false
    }

    fun getSafeConfirmationStatus(): Boolean {
        return _isSafeConfirmed.value
    }
}