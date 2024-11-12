package com.ssafy.shieldroneapp.ui.map

import com.ssafy.shieldroneapp.data.model.RouteLocation

/**
 * Map 화면에서 발생하는 사용자 동작 및 이벤트를 정의하는 클래스
 */
sealed class MapEvent {
    object LoadCurrentLocationAndFetchHives : MapEvent() // 현재 위치 로드 및 주변 출발지 조회
    data class SearchHivesByKeyword(val keyword: String) : MapEvent() // 키워드로 정류장 검색
    data class SearchDestination(val destination: String) : MapEvent() // 도착지 검색

    data class StartLocationSelected(val location: RouteLocation) : MapEvent() // 출발지 선택
    data class EndLocationSelected(val location: RouteLocation) : MapEvent() // 도착지 선택

    object RequestDroneAssignment : MapEvent() // 드론 배정 시작
    object BackPressed : MapEvent() // 뒤로 가기

    // 실시간 위치 추적 이벤트
    object StartLocationTracking : MapEvent() // 실시간 위치 추적 시작
    object StopLocationTracking : MapEvent() // 실시간 위치 추적 중지
}