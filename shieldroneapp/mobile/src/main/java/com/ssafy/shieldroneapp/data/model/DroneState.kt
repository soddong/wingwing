package com.ssafy.shieldroneapp.data.model

import com.ssafy.shieldroneapp.data.model.response.DroneRouteResponse

/**
 * 드론 이용 가능 상태를 나타내는 열거형 클래스.
 *
 * AVAILABLE: 사용 가능
 * RESERVED: 예약된 상태
 * INUSE: 현재 사용 중
 */
enum class DroneStatus {
    AVAILABLE, RESERVED, IN_USE
}

/**
 * 드론 매칭 최종 상태
 *
 * 사용자 입력 코드 검증 결과를 나타냄.
 */
enum class DroneMatchingResult {
    SUCCESS, FAILURE, TIMEOUT
}

/**
 * 드론 상태 정보를 관리하는 데이터 클래스
 *
 * @property droneId 드론 ID
 * @property stationIP 드론 스테이션 서버 IP (매칭 성공 시)
 *
 * @property battery 드론 배터리 잔량 (0-100, %)
 * @property status 드론 이용 가능 상태 (DroneStatus 사용)
 *
 * @property isAssigned 드론 배정 여부 (초기 배정)
 * @property isMatched 드론 코드 매칭 여부 (코드 입력 성공)
 *
 * @property estimatedTime 예상 소요 시간 (분 단위)
 * @property distance 이동 거리 (미터 단위)
 * @property assignedTime 드론 배정 시간 (밀리초, 10분 타이머 용)
 */
data class DroneState(
    val droneId: Int,
    val stationIP: String? = null,

    val battery: Int? = null,
    val status: DroneStatus? = DroneStatus.AVAILABLE,

    val isAssigned: Boolean = false,
    val isMatched: Boolean = false,

    val estimatedTime: Int? = null,
    val distance: Int? = null,
    val assignedTime: Long? = null
) {
    companion object {
        /**
         * DroneRouteResponse 데이터를 기반으로 DroneState를 생성
         */
        fun createDroneStateFromResponse(response: DroneRouteResponse): DroneState {
            return DroneState(
                droneId = response.droneId ?: -1,
                estimatedTime = response.estimatedTime,
                distance = response.distance,
                isAssigned = true
            )
        }
    }
}