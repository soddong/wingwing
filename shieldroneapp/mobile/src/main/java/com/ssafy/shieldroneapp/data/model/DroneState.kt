package com.ssafy.shieldroneapp.data.model

/**
 * 드론 상태 정보를 관리하는 데이터 클래스
 *
 * @property droneId 드론 ID
 * @property stationIP 드론 스테이션 서버 IP (매칭 성공 시)
 * @property isAssigned 드론 배정 여부 (초기 배정)
 * @property isMatched 드론 코드 매칭 여부 (코드 입력 성공)
 * @property estimatedTime 예상 소요 시간 (분 단위)
 * @property distance 이동 거리 (미터 단위)
 * @property assignedTime 드론 배정 시간 (밀리초, 10분 타이머 용)
 */
data class DroneState(
    val droneId: Int,
    val stationIP: String? = null,
    val isAssigned: Boolean = false,
    val isMatched: Boolean = false,
    val estimatedTime: Int? = null,
    val distance: Int? = null,
    val assignedTime: Long? = null
)