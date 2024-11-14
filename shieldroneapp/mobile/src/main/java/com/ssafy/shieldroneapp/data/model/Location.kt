package com.ssafy.shieldroneapp.data.model

import com.ssafy.shieldroneapp.data.model.response.DroneResponse

/**
 * 위치 좌표를 저장하는 데이터 클래스
 *
 * @property lat 위도
 * @property lng 경도
 */
data class LatLng(
    val lat: Double,
    val lng: Double
)

/**
 * 위치 유형을 구분하는 열거형
 */
enum class LocationType {
    START,  // 출발지 (드론 정류장)
    END     // 도착지 (집)
}

/**
 * 출발지/도착지 위치 정보를 통합 관리하는 데이터 클래스
 *
 * @property locationType 위치 유형 (출발지/도착지)
 * @property locationName 위치 이름
 * @property distance 현재 위치로부터의 거리 (m)
 * @property lat 위도
 * @property lng 경도
 *
 * @property hiveId 정류장 ID (출발지인 경우)
 * @property hiveNo 정류장 번호 (출발지인 경우)
 * @property direction 이동 방면 (출발지인 경우)
 * @property availableDrone 이용 가능한 드론 개수 (출발지인 경우)
 * @property drones 드론 상태 목록 (출발지인 경우)
 *
 * @property homeAddress 도로명 주소 (도착지인 경우)
 */
data class RouteLocation(
    val locationType: LocationType,
    
    // 공통
    val locationName: String? = null,
    val distance: Int? = null,
    val lat: Double,
    val lng: Double,

    // 출발지(START)
    val hiveId: Int? = null,
    val hiveNo: Int? = null,
    val direction: String? = null,
    val availableDrone: Int? = null,
    val drones: List<DroneResponse>? = null,

    // 도착지(END)
    val homeAddress: String? = null
)