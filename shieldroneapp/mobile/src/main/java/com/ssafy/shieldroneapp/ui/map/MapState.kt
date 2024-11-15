package com.ssafy.shieldroneapp.ui.map

import com.ssafy.shieldroneapp.data.model.DroneState
import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation

data class MapState(
    // 위치 관련
    val currentLocation: LatLng? = null,
    val nearbyHives: List<RouteLocation> = emptyList(),
    val selectedStart: RouteLocation? = null,
    val selectedEnd: RouteLocation? = null,

    // 검색 관련
    val startSearchText: String = "",
    val endSearchText: String = "",
    val searchResults: List<RouteLocation> = emptyList(),
    val searchType: LocationType = LocationType.START,

    // 마커 및 모달
    val selectedStartMarker: RouteLocation? = null, // 선택된 출발지 마커
    val selectedEndMarker: RouteLocation? = null, // 선택된 도착지 마커
    val showStartMarkerModal: Boolean = false,
    val showEndMarkerModal: Boolean = false,
    val showSearchModal: Boolean = false,

    // 드론 상태
    val droneState: DroneState? = null,

    // UI 상태
    val isLoading: Boolean = false,
    val error: String? = null,

    // 실시간 위치 추적 상태 추가
    val isTrackingLocation: Boolean = false
)