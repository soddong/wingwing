package com.ssafy.shieldroneapp.data.model.response

import com.google.gson.annotations.SerializedName
import com.ssafy.shieldroneapp.data.model.DroneStatus
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation

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
 * @property availableDrone 이용 가능 드론 수
 * @property drones 드론 상태 목록
 *
 */
data class HiveResponse(
    val hiveId: Int,
    val hiveName: String,
    val hiveNo: Int,
    val direction: String,
    val distance: Int,
    val lat: Double,
    val lng: Double,
    val availableDrone: Int,
    val drones: List<DroneResponse>
) {
    fun toRouteLocation() = RouteLocation(
        locationType = LocationType.START,
        locationName = hiveName,
        distance = distance,
        lat = lat,
        lng = lng,
        hiveId = hiveId,
        hiveNo = hiveNo,
        direction = direction,
        availableDrone = availableDrone,
        drones = drones,
    )
}

/**
 * 도착지(집) 검색 시,
 * 카카오 맵 장소 검색 응답 모델
 */
data class KakaoSearchResponse(
    @SerializedName("documents") val documents: List<Document>,
    @SerializedName("meta") val meta: Meta
) {
    data class Document(
        @SerializedName("id") val id: String,
        @SerializedName("place_name") val placeName: String,
        @SerializedName("category_name") val categoryName: String,
        @SerializedName("category_group_code") val categoryGroupCode: String,
        @SerializedName("category_group_name") val categoryGroupName: String,
        @SerializedName("phone") val phone: String,
        @SerializedName("address_name") val addressName: String,
        @SerializedName("road_address_name") val roadAddressName: String,
        @SerializedName("x") val x: String,  // longitude
        @SerializedName("y") val y: String,  // latitude
        @SerializedName("place_url") val placeUrl: String,
        @SerializedName("distance") val distance: String
    ) {
        fun toRouteLocation() = RouteLocation(
            locationType = LocationType.END,
            locationName = placeName,
            distance = distance.toIntOrNull(),
            lat = y.toDoubleOrNull() ?: 0.0,
            lng = x.toDoubleOrNull() ?: 0.0,
            homeAddress = roadAddressName.ifEmpty { addressName }
        )

        fun toHomeLocationResponse() = HomeLocationResponse(
            homeAddress = roadAddressName.ifEmpty { addressName },
            distance = distance.toInt(),
            lat = y.toDoubleOrNull() ?: 0.0,
            lng = x.toDoubleOrNull() ?: 0.0,
        )
    }

    data class Meta(
        @SerializedName("total_count") val totalCount: Int,
        @SerializedName("pageable_count") val pageableCount: Int,
        @SerializedName("is_end") val isEnd: Boolean,
        @SerializedName("same_name") val sameName: RegionInfo?
    )

    data class RegionInfo(
        @SerializedName("region") val region: List<String>,
        @SerializedName("keyword") val keyword: String,
        @SerializedName("selected_region") val selectedRegion: String
    )
}

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
 * 드론 응답 데이터를 관리하는 데이터 클래스.
 *
 * @property droneId 드론 ID
 * @property battery 드론 배터리 잔량 (0-100, %)
 * @property status 드론 이용 가능 상태 (DroneStatus 사용)
 * @property droneCode 드론 고유 코드
 */
data class DroneResponse(
    val droneId: Int,
    val battery: Int,
    val status: DroneStatus,
    val droneCode: Int,
)

/**
 * 드론 배정 요청에 대한 응답 모델
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