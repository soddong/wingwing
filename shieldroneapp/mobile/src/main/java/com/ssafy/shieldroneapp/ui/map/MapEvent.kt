package com.ssafy.shieldroneapp.ui.map

import com.ssafy.shieldroneapp.data.model.DroneMatchingResult
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.data.model.request.DroneCancelRequest
import com.ssafy.shieldroneapp.data.model.request.HiveSearchRequest
import com.ssafy.shieldroneapp.data.model.request.KakaoSearchRequest

/**
 * Map 화면에서 발생하는 사용자 동작 및 이벤트를 정의하는 클래스
 */
sealed class MapEvent {
    // 출발지/도착지 리스트 조회/검색
    object LoadCurrentLocationAndFetchHives : MapEvent() // 현재 위치 로드 및 주변 출발지 조회
    data class SearchHivesByKeyword(val keyword: HiveSearchRequest) : MapEvent() // 키워드로 정류장 검색
    data class SearchDestination(val destination: KakaoSearchRequest) : MapEvent() // 도착지 검색

    // 출발지/도착지 마커 선택 및 모달 관리
    data class StartLocationSelected(val location: RouteLocation) : MapEvent()
    data class EndLocationSelected(val location: RouteLocation) : MapEvent()
    object CloseModal : MapEvent()

    // 출발지/도착지 검색 입력 필드 클릭 / 텍스트 입력 시
    data class SearchFieldClicked(val type: LocationType) : MapEvent()
    data class UpdateStartLocationText(val text: String) : MapEvent()
    data class UpdateEndLocationText(val text: String) : MapEvent()

    // 출발지/도착지 선택
    data class SetStartLocation(val location: RouteLocation) : MapEvent()
    data class SetEndLocation(val location: RouteLocation) : MapEvent()

    // 드론 배정 요청 / 배정 취소 / 최종 매칭 요청 / 결과 처리
    object RequestDroneAssignment : MapEvent()
    data class RequestDroneCancel(val droneId: DroneCancelRequest) : MapEvent()
    data class RequestDroneMatching (val droneCode: Int) : MapEvent()
    data class HandleDroneMatchingResult (val result: DroneMatchingResult) : MapEvent()

    // 위치 서비스(GPS, 네트워크)의 활성화 상태 업데이트
    data class UpdateLocationServicesState(val isEnabled: Boolean) : MapEvent()

    // 실시간 위치 추적 이벤트
    object StartLocationTracking : MapEvent() // 실시간 위치 추적 시작
    object StopLocationTracking : MapEvent() // 실시간 위치 추적 중지

    // 오류 메시지 설정
    data class SetErrorMessage(val message: String) : MapEvent()
}

