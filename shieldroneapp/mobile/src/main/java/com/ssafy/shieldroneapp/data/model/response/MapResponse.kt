package com.ssafy.shieldroneapp.data.model.response

import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation

/**
 * 기본 도착지(집) 정보 응답 모델
 *
 * @property homeAddress 도로명 주소
 * @property distance 현재 위치로부터의 거리 (미터 단위)
 * @property lat 위도
 * @property lng 경도
 */
data class HomeLocationResponse(
    val homeAddress: String,
    val distance: Int,
    val lat: Double,
    val lng: Double,
)

/**
 * 출발지(드론 정류장) 정보 응답 모델
 *
 * @property hiveId 정류장 고유 ID
 * @property hiveName 정류장 이름
 * @property hiveNo 정류장 번호
 * @property direction 이동 방면
 * @property distance 현재 위치로부터의 거리 (미터 단위)
 * @property lat 위도
 * @property lng 경도
 */
data class HiveResponse(
    val hiveId: Int,
    val hiveName: String,
    val hiveNo: Int,
    val direction: String,
    val distance: Int,
    val lat: Double,
    val lng: Double,
) {
    fun toRouteLocation(): RouteLocation {
        return RouteLocation(
            locationType = LocationType.START,
            hiveId = hiveId,
            hiveName = hiveName,
            hiveNo = hiveNo,
            direction = direction,
            distance = distance,
            lat = lat,
            lng = lng
        )
    }
}

/**
 * 드론 경로 안내 가능 여부 응답 모델
 *
 * @property droneId 배정된 드론 ID (가능한 경우)
 * @property estimatedTime 예상 소요 시간 (분 단위)
 * @property distance 이동 거리 (미터 단위)
 */
data class DroneRouteResponse(
    val droneId: Int?,
    val estimatedTime: Int?,
    val distance: Int,
)

/**
 * 드론 매칭 성공 응답 모델
 *
 * @property droneId 매칭된 드론 ID
 * @property stationIP 드론 스테이션 서버 IP
 */
data class DroneMatchResponse(
    val droneId: Int,
    val stationIP: String,
)

/**
 * 카카오맵 장소 검색 응답 모델
 * 도착지 검색 시 사용
 *
 * @property placeName 장소명
 * @property roadAddressName 도로명 주소
 * @property distance 현재 위치로부터의 거리 (미터 단위)
 * @property lat 위도
 * @property lng 경도
 */
data class KakaoSearchResponse(
    val placeName: String,
    val roadAddressName: String,
    val distance: Int,
    val lat: Double,
    val lng: Double
) {
    fun toHomeLocationResponse() = HomeLocationResponse(
        homeAddress = roadAddressName,
        distance = distance,
        lat = lat,
        lng = lng,
    )
}