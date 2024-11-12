package com.ssafy.shieldroneapp.ui.map.screens

/**
 * 근처 드론 정류장(출발지 후보), 출발지, 도착지를 나타내는 MapMarker 클래스.
 *
 * 하늘색 마커: 서버에서 제공하는 근처 드론 정류장(출발지 후보)을 나타냄.
 * 파란색 마커: 사용자가 출발지로 선택한 위치를 강조.
 * 빨간색 마커: 도착지로 선택된 위치를 강조.
 *
 * 클릭 이벤트에 따라 마커의 색상과 상태를 변경하며, 관련 모달을 표시한다.
 *
 * @property latitude 마커의 위도
 * @property longitude 마커의 경도
 * @property isSelected 선택 여부에 따라 마커의 색상을 변경
 */