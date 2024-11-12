package com.ssafy.shieldroneapp.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.shieldroneapp.data.repository.AlertRepository
import com.ssafy.shieldroneapp.data.source.remote.ApiService
import com.ssafy.shieldroneapp.ui.components.DangerAlertState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Map 화면의 상태와 로직을 관리하는 ViewModel 클래스.
 *
 * 유저의 GPS 위치, 근처 정류장 리스트, 출발지/도착지 정보 등을 관리.
 * 드론 배정 요청, QR 코드 인식, 위험 상황 알림 등 주요 이벤트를 처리한다.
 * 위치 정보와 드론 관련 데이터를 처리하기 위해 리포지토리와 상호작용한다.
 *
 * @property locationRepository 출발지/도착지 위치 데이터를 관리하는 리포지토리 객체
 * @property droneRepository 드론 배정 및 매칭 관련 데이터를 관리하는 리포지토리 객체
 * @property alertRepository 위험 상황 알림 데이터를 관리하는 리포지토리 객체
 */

@HiltViewModel
class MapViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val alertHandler: AlertHandler,
    private val apiService: ApiService
) : ViewModel() {
    companion object {
        private const val TAG = "모바일: 맵 뷰모델"
    }

    val alertState = alertRepository.alertState
        .map { alertData ->
            alertData?.let {
                DangerAlertState(
                    isVisible = true,
                    level = 3, // 현재는 Level 3만 구현
                    timestamp = it.timestamp
                )
            } ?: DangerAlertState()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DangerAlertState()
        )

    fun dismissAlert() {
        alertRepository.clearAlert()
        alertHandler.dismissAlert()
    }

    fun sendEmergencyAlert() {
        viewModelScope.launch {
            try {
                apiService.setEmergency(
                    lat = BigDecimal("37.123"),
                    lng = BigDecimal("127.456")
                )
                Log.d(TAG, "긴급 알림 전송 성공")
            } catch (e: Exception) {
                Log.e(TAG, "긴급 알림 전송 실패", e)
                Log.e(TAG, "에러 메시지: ${e.message}")
                Log.e(TAG, "에러 원인: ${e.cause}")
                e.printStackTrace()
            }
        }
    }
}