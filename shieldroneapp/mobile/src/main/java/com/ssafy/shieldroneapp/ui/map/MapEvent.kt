package com.ssafy.shieldroneapp.ui.map

import com.ssafy.shieldroneapp.data.model.DroneState
import com.ssafy.shieldroneapp.data.model.DroneStatus
import com.ssafy.shieldroneapp.data.model.LocationType
import com.ssafy.shieldroneapp.data.model.RouteLocation
import com.ssafy.shieldroneapp.data.model.request.DroneCancelRequest
import com.ssafy.shieldroneapp.data.model.request.DroneMatchRequest
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

    // 출발지/도착지 마커 선택
    data class StartLocationSelected(val location: RouteLocation) : MapEvent()
    data class EndLocationSelected(val location: RouteLocation) : MapEvent()

    // 모달 관리
    data class OpenModal(val modalType: ModalType) : MapEvent() // 특정 모달 열기
    data class CloseModal(val modalType: ModalType) : MapEvent() // 특정 모달 닫기
    object CloseAllModals : MapEvent() // 모든 모달 닫기

    // 출발지/도착지 검색 입력 필드 클릭 / 텍스트 입력 시
    data class SearchFieldClicked(val type: LocationType) : MapEvent()
    data class UpdateStartLocationText(val text: String) : MapEvent()
    data class UpdateEndLocationText(val text: String) : MapEvent()

    // 출발지/도착지 선택
    data class SetStartLocation(val location: RouteLocation) : MapEvent()
    data class SetEndLocation(val location: RouteLocation) : MapEvent()

    // 드론 배정 요청 / 배정 취소 / 최종 매칭 요청 / 결과 처리 / 서비스 종료
    object RequestDroneAssignment : MapEvent()
    data class RequestDroneCancel(val droneId: DroneCancelRequest) : MapEvent()
    data class RequestDroneMatching (val request: DroneMatchRequest) : MapEvent()
    data class HandleDroneMatchingResult (val result: DroneStatus) : MapEvent()
    data class RequestServiceEnd(val droneId: DroneCancelRequest) : MapEvent()

    // 애니메이션 관리 이벤트
    object StartDroneAnimation : MapEvent() // 드론 애니메이션 시작
    object EndDroneAnimation : MapEvent() // 드론 애니메이션 종료

    // 위치 서비스(GPS, 네트워크)의 활성화 상태 업데이트
    data class UpdateLocationServicesState(val isEnabled: Boolean) : MapEvent()

    // 실시간 위치 추적 이벤트
    object StartLocationTracking : MapEvent() // 실시간 위치 추적 시작
    object StopLocationTracking : MapEvent() // 실시간 위치 추적 중지

    // 오류 메시지 설정
    data class SetErrorMessage(val message: String) : MapEvent()

    // [로컬 저장소] 드론 상태 관리
    object LoadDroneState : MapEvent() // 드론 상태 가져오기
    data class SaveDroneState(val droneState: DroneState) : MapEvent() // 드론 상태 저장
    object ClearDroneState : MapEvent() // 드론 상태 초기화

    // [로컬 저장소] 출발지/도착치 관리
    object LoadStartAndEndLocations : MapEvent() // 출발지, 도착지 가져오기
    data class SaveStartLocation(val location: RouteLocation) : MapEvent() // 출발지 저장
    data class SaveEndLocation(val location: RouteLocation) : MapEvent() // 도착지 저장
    object ClearLocationData : MapEvent() // 초기화
}

