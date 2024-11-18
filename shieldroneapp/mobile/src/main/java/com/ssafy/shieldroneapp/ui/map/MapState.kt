package com.ssafy.shieldroneapp.ui.map

import com.ssafy.shieldroneapp.data.model.DroneState
import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.data.model.response.DroneMatchResponse

/**
 * 모달 상태를 관리하는 열거형
 */
enum class ModalType {
    SEARCH_RESULTS, // 검색 결과 모달
    START_MARKER_INFO, // 출발지 마커 정보 모달
    END_MARKER_INFO, // 도착지 마커 정보 모달
    DRONE_MATCH_RESULT, // 드론 매칭 결과 모달
    DRONE_ASSIGNMENT_SUCCESS, // 드론 배정 성공 모달
    DRONE_ASSIGNMENT_FAILURE, // 드론 배정 실패 모달
    CANCEL_SUCCESS, // 드론 배정 취소 완료 모달
    SERVICE_END, // 서비스 종료 모달
}

data class MapState(
    // 위치 관련
    val currentLocation: LatLng? = null,
    val nearbyHives: List<RouteLocation> = emptyList(),
    val selectedStart: RouteLocation? = null,
    val selectedEnd: RouteLocation? = null,

    // 마커
    val selectedStartMarker: RouteLocation? = null,
    val selectedEndMarker: RouteLocation? = null,

    // 검색 관련
    val startSearchText: String = "",
    val endSearchText: String = "",
    val searchResults: List<RouteLocation> = emptyList(),
    val searchType: LocationType = LocationType.START,

    // 모달 관리
    val showSearchResultsModal: Boolean = false, // 검색 결과
    val showStartMarkerModal: Boolean = false,  // 출발지 마커 정보
    val showEndMarkerModal: Boolean = false,    // 도착지 마커 정보
    val showDroneMatchResultModal: Boolean = false, // 드론 매칭 결과
    val showDroneAssignmentSuccessModal: Boolean = false, // 드론 배정 성공
    val showDroneAssignmentFailureModal: Boolean = false, // 드론 배정 실패
    val showCancelSuccessModal: Boolean = false, // 드론 배정 취소 완료
    val showServiceEndModal: Boolean = false,    // 서비스 종료 완료 
    
    // 애니메이션
    val showDroneAnimation: Boolean = false, // 드론 매칭 성공 시 애니메이션 표시 여부

    // 드론 상태
    val droneState: DroneState? = null,
    val droneMatchResult: DroneMatchResponse? = null, // 드론 매칭 결과 추가
    val remainingTime: Int = 600, // 드론 배정 후 매칭까지 유효기간 타이머 10분 (초 단위)
    val droneAssignmentError: String? = null, // 드론 배정 에러

    // UI 상태
    val isLoading: Boolean = false,
    val error: String? = null,

    // 실시간 위치 추적 상태 추가
    val isTrackingLocation: Boolean = false
)