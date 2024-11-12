package com.ssafy.shieldroneapp.ui.map

import com.ssafy.shieldroneapp.data.model.DroneState
import com.ssafy.shieldroneapp.data.model.LatLng
import com.ssafy.shieldroneapp.data.model.RouteLocation

enum class SearchType {
    NONE, START, END
}

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
    val searchType: SearchType = SearchType.NONE,

    // 모달 상태
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