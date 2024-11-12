package com.ssafy.shieldroneapp.data.model.request

/**
 * 기본 도착지(집) 설정 요청 모델
 *
 * @property homeAddress 도로명 주소
 * @property lat 위도
 * @property lng 경도
 */
data class HomeLocationRequest(
    val homeAddress: String,
    val lat: Double,
    val lng: Double,
)

/**
 * 드론 경로 안내 요청 모델
 *
 * @property startLocation 출발 정류장 정보
 * @property endLocation 도착지 위치 정보
 */
data class DroneRouteRequest(
    val startLocation: StartLocation,
    val endLocation: EndLocation
)

data class StartLocation(
    val hiveId: Int
)

data class EndLocation(
    val lat: Double,
    val lng: Double
)

/**
 * 드론 배정 취소 요청 모델
 * 10분 내 코드 미입력 시 사용
 */
data class DroneCancelRequest(
    val droneId: Int
)

/**
 * 드론 매칭 요청 모델
 * 드론에 표시된 코드 입력 시 사용
 */
data class DroneMatchRequest(
    val droneId: Int,
    val droneCode: Int
)