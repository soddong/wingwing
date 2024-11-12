package com.ssafy.shieldroneapp.ui.map.screens

/**
 * MapMarker에 대한 상세 정보를 표시하는 모달 컴포넌트.
 *
 * 출발지 타입: 출발지 설정 시 사용.
 *   - 근처 드론 정류장(출발지 후보) 중에 선택한 경우: 선택 버튼이 포함된 모달.
 *   - 검색을 통해 선택한 경우: 선택 버튼이 없는 모달.
 *   - 정류장 이름, 정류장 번호, 이동 방면 정보를 UI에 표시한다.
 *
 * 도착지 타입: 도착지 설정 시 사용.
 *   - 선택 버튼이 없는 모달.
 *   - 정류장 이름, 도로명 주소, 현재 위치로부터의 거리를 UI에 표시한다.
 *
 * @param locationName 선택된 위치의 이름
 * @param locationNumber 정류장 번호 (출발지)
 * @param directionInfo 이동 방면 정보 (출발지)
 * @param address 도로명 주소 (도착지)
 * @param distanceFromCurrentLocation 현재 위치로부터의 거리 (도착지)
 * @param onConfirm 선택 버튼 클릭 시 호출되는 콜백 함수 (출발지 후보 선택 시에만 사용)
 */