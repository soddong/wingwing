package com.ssafy.shieldroneapp.data.model

import com.ssafy.shieldroneapp.data.model.response.DroneRouteResponse

/**
 * 드론의 가용 상태 및 매칭 프로세스 상태를 통합 관리하는 열거형
 */
enum class DroneStatus {
    // 드론 가용 상태
    AVAILABLE, RESERVED, IN_USE,

    // 매칭 프로세스 상태
    MATCHING_NONE,      // 초기 상태
    MATCHING_ASSIGNED,  // 배정됨
    MATCHING_FAILED,    // 코드 실패
    MATCHING_COMPLETE,  // 매칭 완료
    MATCHING_CANCELLED, // 취소됨
    MATCHING_TIMEOUT    // 시간 초과
}

/**
 * 드론 상태 정보를 통합 관리하는 데이터 클래스
 *
 * @property droneId 드론 ID
 * @property stationIP 드론 스테이션 서버 IP (매칭 성공 시)
 *
 * @property matchStatus 현재 매칭 상태
 *
 * @property battery 드론 배터리 잔량 (0-100, %)
 * @property estimatedTime 예상 소요 시간 (분)
 * @property distance 이동 거리 (미터)
 * @property assignedTime 드론 배정 시간 (밀리초)
 */
data class DroneState(
    val droneId: Int,
    val stationIP: String? = null,

    val matchStatus: DroneStatus = DroneStatus.MATCHING_NONE,

    val battery: Int? = null,
    val estimatedTime: Int? = null,
    val distance: Int? = null,
    val assignedTime: Long? = null,
) {
    companion object {
        fun createDroneStateFromResponse(response: DroneRouteResponse): DroneState {
            return DroneState(
                droneId = response.droneId ?: -1,
                estimatedTime = response.estimatedTime,
                distance = response.distance,
                matchStatus = DroneStatus.MATCHING_ASSIGNED,
            )
        }
    }
}