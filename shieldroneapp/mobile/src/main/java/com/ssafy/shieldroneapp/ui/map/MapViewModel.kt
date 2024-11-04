package com.ssafy.shieldroneapp.ui.map

/**
 * Map 화면의 상태와 로직을 관리하는 ViewModel 클래스.
 *
 * 유저의 GPS 위치, 근처 정류장 리스트, 출발지/도착지 정보 등을 관리.
 * 드론 배정 요청, QR 코드 인식, 위험 상황 알림 등 주요 이벤트를 처리한다.
 * 위치 정보와 드론 관련 데이터를 처리하기 위해 리포지토리와 상호작용한다.
 *
 * @property locationRepository 출발지/도착지 위치 데이터를 관리하는 리포지토리 객체
 * @property droneRepository 드론 배정 및 매칭 관련 데이터를 관리하는 리포지토리 객체
 * @property alertRepository 위험 상황 알림 데이터를 관리하는 리포지토리 객체
 */