package com.ssafy.shieldroneapp.ui.map

/**
 * 사용자의 현재 위치와 드론 경로 안내를 제공하는 메인 Map 화면.
 *
 * 유저의 GPS 위치를 받아 점으로 표시하고, 근처 정류장을 하늘색 마커로 표시.
 * 출발지/도착지 입력, 드론 배정 요청, 경로 안내 등 주요 기능을 관리한다.
 * 드론 배정 성공 시 경로를 표시하고, QR 코드 인식 및 위험 상황 알림을 처리한다.
 *
 * @property viewModel Map 화면의 상태와 로직을 관리하는 ViewModel
 */